package com.book.domain.service.impl;

import com.book.domain.factory.BookFactory;
import com.book.domain.model.Book;
import com.book.domain.model.Chapter;
import com.book.domain.model.Review;
import com.book.domain.repository.BookRepository;
import com.book.domain.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final BookFactory bookFactory;
    private final BookRepository bookRepository;

    @Override
    public Mono<Book> createBook(String title, String author, String description, 
                                String coverUrl, Set<String> categories, Set<String> tags) {
        return Mono.fromCallable(() -> bookFactory.createBook(title, author, description, coverUrl, categories, tags))
                .flatMap(bookRepository::save);
    }

    @Override
    public Mono<Book> updateBook(String id, String title, String author, String description) {
        return bookRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("图书不存在")))
                .map(book -> {
                    book.updateBasicInfo(title, author, description);
                    return book;
                })
                .flatMap(bookRepository::save);
    }

    @Override
    public Mono<Book> publishBook(String id) {
        return bookRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("图书不存在")))
                .map(book -> {
                    book.publish();
                    return book;
                })
                .flatMap(bookRepository::save);
    }

    @Override
    public Mono<Book> addChapter(String bookId, Chapter chapter) {
        return bookRepository.findById(bookId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("图书不存在")))
                .map(book -> {
                    book.addChapter(chapter);
                    return book;
                })
                .flatMap(bookRepository::save);
    }

    @Override
    public Mono<Book> addReview(String bookId, Review review) {
        return bookRepository.findById(bookId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("图书不存在")))
                .map(book -> {
                    book.addReview(review);
                    return book;
                })
                .flatMap(bookRepository::save);
    }

    // ... 其他方法实现
} 