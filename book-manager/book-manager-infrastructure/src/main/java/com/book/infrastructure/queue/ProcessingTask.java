package com.book.infrastructure.queue;

import lombok.Getter;
import reactor.core.publisher.Flux;

@Getter
public abstract class ProcessingTask {
    private final String identifier;
    private final String filename;
    private int retryCount;
    private final int maxRetries;

    protected ProcessingTask(String identifier, String filename, int maxRetries) {
        this.identifier = identifier;
        this.filename = filename;
        this.maxRetries = maxRetries;
    }

    public abstract Flux<Double> execute();

    public boolean shouldRetry() {
        return retryCount < maxRetries;
    }

    public boolean incrementRetryCount() {
        retryCount++;
        return shouldRetry();
    }
} 