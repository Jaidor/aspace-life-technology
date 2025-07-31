package com.aspacelifetechnology.aspace_life_technology.controller;

import com.aspacelifetechnology.aspace_life_technology.models.PostModel;
import com.aspacelifetechnology.aspace_life_technology.repository.PostRepository;
import com.aspacelifetechnology.aspace_life_technology.services.PostImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/import")
public class PostController {
    private final PostImportService importService;
    private final PostRepository postRepository;

    @PostMapping("/posts")
    public CompletableFuture<ResponseEntity<String>> importPosts() {
        return importService.fetchAndSaveAllPosts()
                .thenApply(v -> ResponseEntity.ok("Imported posts successfully"))
                .exceptionally(ex -> ResponseEntity.status(500).body("Import failed: " + ex.getMessage()));
    }

    @GetMapping("/posts")
    public PagedResponse<PostModel> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<PostModel> p = postRepository.findAll(pageable);
        return new PagedResponse<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.isLast()
        );
    }

    public record PagedResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {}
}
