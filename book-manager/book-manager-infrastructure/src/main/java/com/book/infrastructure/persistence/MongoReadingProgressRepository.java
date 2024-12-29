package com.book.infrastructure.persistence;

import com.book.domain.model.ReadingProgress;
import com.book.domain.repository.ReadingProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class MongoReadingProgressRepository implements ReadingProgressRepository {
    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<ReadingProgress> save(ReadingProgress progress) {
        return mongoTemplate.save(progress);
    }

    @Override
    public Mono<ReadingProgress> findByUserIdAndBookId(String userId, String bookId) {
        return mongoTemplate.findOne(
            Query.query(Criteria.where("userId").is(userId)
                .and("bookId").is(bookId)),
            ReadingProgress.class
        );
    }
} 