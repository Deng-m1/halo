package com.book.api.service;

import com.book.api.command.CreateBookCommand;
import com.book.api.command.UpdateBookCommand;
import com.book.api.command.AddChapterCommand;
import com.book.api.command.AddReviewCommand;
import com.book.api.dto.BookDTO;
import com.book.domain.factory.BookFactory;
import com.book.domain.model.Book;
import com.book.domain.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BookApplicationService {
    private final BookService bookService;
    private final BookDTOAssembler bookDTOAssembler;

    // 创建图书
    public Mono<BookDTO> createBook(CreateBookCommand command) {
        return bookService.createBook(
                command.getTitle(),
                command.getAuthor(),
                command.getDescription(),
                command.getCoverUrl(),
                command.getCategories(),
                command.getTags()
            )
            .map(bookDTOAssembler::toDTO);
    }

    // 发布图书
    public Mono<BookDTO> publishBook(String bookId) {
        return bookService.publishBook(bookId)
            .map(bookDTOAssembler::toDTO);
    }

    // 添加章节
    public Mono<BookDTO> addChapter(String bookId, AddChapterCommand command) {
        return bookService.addChapter(
                bookId,
                bookDTOAssembler.toChapter(command)
            )
            .map(bookDTOAssembler::toDTO);
    }

    // 添加评论
    public Mono<BookDTO> addReview(String bookId, AddReviewCommand command) {
        return bookService.addReview(
                bookId,
                bookDTOAssembler.toReview(command)
            )
            .map(bookDTOAssembler::toDTO);
    }

    // 查询图书
    public Mono<BookDTO> findById(String id) {
        return bookService.findById(id)
            .map(bookDTOAssembler::toDTO);
    }

    // 按分类查询
    public Flux<BookDTO> findByCategory(String category) {
        return bookService.findByCategory(category)
            .map(bookDTOAssembler::toDTO);
    }

    // 搜索图书
    public Flux<BookDTO> searchBooks(String keyword) {
        return bookService.searchBooks(keyword)
            .map(bookDTOAssembler::toDTO);
    }
} 