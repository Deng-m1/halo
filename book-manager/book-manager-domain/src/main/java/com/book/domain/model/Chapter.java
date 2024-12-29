package com.book.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chapter {
    private String title;
    private String content;
    private int chapterNumber;
    private int wordCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Chapter(String title, String content, int chapterNumber) {
        this.title = title;
        this.content = content;
        this.chapterNumber = chapterNumber;
        this.wordCount = calculateWordCount(content);
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public void updateContent(String content) {
        this.content = content;
        this.wordCount = calculateWordCount(content);
        this.updateTime = LocalDateTime.now();
    }

    private int calculateWordCount(String content) {
        return content != null ? content.length() : 0;
    }
} 