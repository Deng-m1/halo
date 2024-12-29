package com.book.domain.service;

import com.book.domain.model.Book;
import com.book.domain.model.Chapter;
import com.book.domain.model.Review;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface BookService {
    // ͼ�����
    Mono<Book> createBook(String title, String author, String description, String coverUrl, 
                         Set<String> categories, Set<String> tags);
    Mono<Book> updateBook(String id, String title, String author, String description);
    Mono<Book> publishBook(String id);
    Mono<Void> deleteBook(String id);
    
    // �½ڹ���
    Mono<Book> addChapter(String bookId, Chapter chapter);
    Mono<Book> updateChapter(String bookId, String chapterId, Chapter chapter);
    Mono<Book> removeChapter(String bookId, String chapterId);
    
    // ���۹���
    Mono<Book> addReview(String bookId, Review review);
    Mono<Book> removeReview(String bookId, String reviewId);
    
    // ��ѯ����
    Mono<Book> findById(String id);
    Flux<Book> findByCategory(String category);
    Flux<Book> findByTag(String tag);
    Flux<Book> searchBooks(String keyword);
}