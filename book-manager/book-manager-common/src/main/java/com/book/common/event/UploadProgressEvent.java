package com.book.common.event;

import lombok.Data;

@Data
public class UploadProgressEvent {
    private String identifier;
    private String filename;
    private double progress;      // 0-100
    private UploadStatus status;  // UPLOADING, PROCESSING, COMPLETED, FAILED
    private String message;
    private long timestamp;

    public UploadProgressEvent(String identifier, String filename) {
        this.identifier = identifier;
        this.filename = filename;
        this.timestamp = System.currentTimeMillis();
    }
} 