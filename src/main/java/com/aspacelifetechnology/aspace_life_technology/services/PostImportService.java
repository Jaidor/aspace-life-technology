package com.aspacelifetechnology.aspace_life_technology.services;

import com.aspacelifetechnology.aspace_life_technology.controller.dtos.PostDto;
import com.aspacelifetechnology.aspace_life_technology.models.PostModel;
import com.aspacelifetechnology.aspace_life_technology.repository.PostRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class PostImportService {
    private static final Logger log = LoggerFactory.getLogger(PostImportService.class);

    private final PostRepository postRepository;
    private final Executor blockingDbExecutor;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // Endpoint
    private static final String POSTS_URL = "https://jsonplaceholder.typicode.com/posts";

    public PostImportService(PostRepository postRepository,
                             @Qualifier("blockingDbExecutor") Executor blockingDbExecutor) {
        this.postRepository = postRepository;
        this.blockingDbExecutor = blockingDbExecutor;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /**
     * Fetches all posts from JSONPlaceholder and saves them into the database.
     * Returns a CompletableFuture that completes when all work (fetch + save) is done.
     */
    public CompletableFuture<Void> fetchAndSaveAllPosts() {
        return fetchWithRetry(3)
                .thenCompose(body -> parseAndSaveBatch(body));
    }

    /**
     * Fetch with simple retry/backoff.
     */
    private CompletableFuture<String> fetchWithRetry(int remainingAttempts) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(POSTS_URL))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(resp -> {
                    if (resp.statusCode() == 200) {
                        return CompletableFuture.completedFuture(resp.body());
                    } else {
                        String msg = "Unexpected status code " + resp.statusCode();
                        if (remainingAttempts > 1) {
                            log.warn("Fetch failed ({}), retrying... attempts left: {}", msg, remainingAttempts - 1);
                            // simple exponential backoff
                            int backoffMillis = (int) Math.pow(2, (4 - remainingAttempts)) * 250;
                            return delayedFuture(backoffMillis)
                                    .thenCompose(v -> fetchWithRetry(remainingAttempts - 1));
                        } else {
                            return CompletableFuture.failedFuture(new RuntimeException("Failed fetch: " + msg));
                        }
                    }
                })
                .exceptionallyCompose(ex -> {
                    if (remainingAttempts > 1) {
                        log.warn("Exception during fetch ({}), retrying... attempts left: {}", ex.getMessage(), remainingAttempts - 1);
                        int backoffMillis = (int) Math.pow(2, (4 - remainingAttempts)) * 250;
                        return delayedFuture(backoffMillis)
                                .thenCompose(v -> fetchWithRetry(remainingAttempts - 1));
                    } else {
                        return CompletableFuture.failedFuture(ex);
                    }
                });
    }

    /**
     * Parses the JSON and performs a batched save asynchronously.
     */
    private CompletableFuture<Void> parseAndSaveBatch(String jsonBody) {
        List<PostDto> dtos;
        try {
            dtos = objectMapper.readValue(jsonBody, new TypeReference<>() {});
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new RuntimeException("Failed to parse JSON", e));
        }

        List<PostModel> entities = dtos.stream()
                .map(dto -> new PostModel(
                        dto.getId(),
                        dto.getUserId(),
                        dto.getTitle(),
                        dto.getBody()
                ))
                .toList();

        return CompletableFuture.runAsync(() -> {
            try {
                postRepository.saveAll(entities);
                log.info("Saved {} posts", entities.size());
            } catch (DataAccessException dae) {
                // Fallback: try per-entity save to isolate failures
                log.warn("Batch save failed, falling back to per-entity save: {}", dae.getMessage());
                for (PostModel p : entities) {
                    try {
                        postRepository.save(p);
                    } catch (Exception ex) {
                        log.error("Failed saving post id={} : {}", p.getId(), ex.getMessage());
                    }
                }
            }
        }, blockingDbExecutor);
    }

    /**
     * Helper to create a delayed CompletableFuture.
     */
    private static CompletableFuture<Void> delayedFuture(long millis) {
        CompletableFuture<Void> f = new CompletableFuture<>();
        // schedule using a daemon thread; acceptable for small retry logic
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(millis);
                f.complete(null);
            } catch (InterruptedException e) {
                f.completeExceptionally(e);
            }
        });
        t.setDaemon(true);
        t.start();
        return f;
    }
}