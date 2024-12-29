package com.book.common.model;

import lombok.Data;

@Data
public class ChunkInfo {
    private String identifier;    // 文件唯一标识
    private Integer chunkNumber;  // 当前分片编号
    private Integer totalChunks;  // 总分片数
    private Long chunkSize;       // 分片大小
    private Long totalSize;       // 文件总大小
    private String filename;      // 文件名
} 