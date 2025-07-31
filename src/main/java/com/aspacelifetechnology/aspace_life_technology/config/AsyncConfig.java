package com.aspacelifetechnology.aspace_life_technology.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {
    @Bean("blockingDbExecutor")
    public Executor blockingDbExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(20);          // allow more concurrency if needed
        exec.setQueueCapacity(200);      // bigger queue to absorb bursts
        exec.setThreadNamePrefix("db-save-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // fallback: run in caller thread
        exec.initialize();
        return exec;
    }

}