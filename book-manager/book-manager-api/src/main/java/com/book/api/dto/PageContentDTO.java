package com.book.api.dto;

import lombok.Data;

@Data
public class PageContentDTO {
    private String content;          // 当前页内容
    private int pageNumber;          // 当前页码
    private int chapterNumber;       // 当前章节号
    private String chapterTitle;     // 章节标题
    private boolean hasNextPage;     // 是否有下一页
    private boolean hasNextChapter;  // 是否有下一章
    private int totalPages;          // 当前章节总页数
    private ReadingProgressDTO progress;  // 当前阅读进度
} 