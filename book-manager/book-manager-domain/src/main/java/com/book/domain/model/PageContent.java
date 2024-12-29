package com.book.domain.model;

import lombok.Getter;

@Getter
public class PageContent {
    private final String content;          // 当前页内容
    private final int pageNumber;          // 当前页码
    private final int chapterNumber;       // 当前章节号
    private final String chapterTitle;     // 章节标题
    private final boolean hasNextPage;     // 是否有下一页
    private final boolean hasNextChapter;  // 是否有下一章
    private final int totalPages;          // 当前章节总页数

    public PageContent(String content, int pageNumber, int chapterNumber, 
                      String chapterTitle, boolean hasNextPage, 
                      boolean hasNextChapter, int totalPages) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.chapterNumber = chapterNumber;
        this.chapterTitle = chapterTitle;
        this.hasNextPage = hasNextPage;
        this.hasNextChapter = hasNextChapter;
        this.totalPages = totalPages;
    }
} 