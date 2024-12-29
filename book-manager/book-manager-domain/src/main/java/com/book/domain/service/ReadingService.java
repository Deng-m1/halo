package com.book.domain.service;

import com.book.domain.model.Book;
import com.book.domain.model.ReadingProgress;
import reactor.core.publisher.Mono;

public interface ReadingService {
    // 开始阅读
    Mono<ReadingProgress> startReading(String userId, String bookId);
    
    // 获取当前阅读进度
    Mono<ReadingProgress> getReadingProgress(String userId, String bookId);
    
    // 更新阅读进度
    Mono<ReadingProgress> updateReadingProgress(String userId, String bookId, 
                                              int currentChapter, int currentPage, int totalPages);
    
    // 获取章节内容
    Mono<String> getChapterContent(String bookId, int chapterNumber);
    
    // 获取分页内容
    Mono<PageContent> getPageContent(String bookId, int chapterNumber, int pageNumber, int pageSize);
} 