package com.book.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reading_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingProgress {
    @Id
    private String id;
    private String userId;
    private String bookId;
    private int currentChapter;    // 当前章节
    private int currentPage;       // 当前页码
    private int totalPages;        // 总页数
    private double readingProgress; // 阅读进度(百分比)
    private LocalDateTime lastReadTime;
    private ReadingStatus status;  // 阅读状态

    public ReadingProgress(String userId, String bookId) {
        this.userId = userId;
        this.bookId = bookId;
        this.currentChapter = 0;
        this.currentPage = 0;
        this.totalPages = 0;
        this.readingProgress = 0.0;
        this.lastReadTime = LocalDateTime.now();
        this.status = ReadingStatus.READING;
    }

    public void updateProgress(int currentChapter, int currentPage, int totalPages) {
        this.currentChapter = currentChapter;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.readingProgress = calculateProgress();
        this.lastReadTime = LocalDateTime.now();
        
        if (isCompleted()) {
            this.status = ReadingStatus.COMPLETED;
        }
    }

    private double calculateProgress() {
        if (totalPages == 0) return 0;
        return (double) currentPage / totalPages * 100;
    }

    private boolean isCompleted() {
        return currentPage >= totalPages;
    }
} 