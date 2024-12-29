package com.book.infrastructure.persistence;

import com.book.domain.model.Book;
import com.book.domain.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class MongoBookRepository implements BookRepository {
    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<Book> save(Book book) {
        return mongoTemplate.save(book);
    }

    @Override
    public Mono<Book> findById(String id) {
        return mongoTemplate.findById(id, Book.class);
    }

    @Override
    public Mono<Book> findByTitle(String title) {
        return mongoTemplate.findOne(
            Query.query(Criteria.where("title").is(title)),
            Book.class
        );
    }

    @Override
    public Flux<Book> findByAuthor(String author) {
        return mongoTemplate.find(
            Query.query(Criteria.where("author").is(author)),
            Book.class
        );
    }

    @Override
    public Flux<Book> findByCategory(String category) {
        return mongoTemplate.find(
            Query.query(Criteria.where("categories").in(category)),
            Book.class
        );
    }

    @Override
    public Flux<Book> findByTag(String tag) {
        return mongoTemplate.find(
            Query.query(Criteria.where("tags").in(tag)),
            Book.class
        );
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return mongoTemplate.remove(
            Query.query(Criteria.where("id").is(id)),
            Book.class
        ).then();
    }
} 