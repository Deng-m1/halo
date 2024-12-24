package com.reader.infrastructure.persistence;

import com.reader.domain.user.User;
import com.reader.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class R2dbcUserRepository implements UserRepository {
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<User> save(User user) {
        return template.insert(User.class).using(user);
    }

    @Override
    public Mono<User> findById(Long id) {
        return template.selectOne(
            Query.query(Criteria.where("id").is(id)),
            User.class
        );
    }

    @Override
    public Mono<User> findByUsername(String username) {
        return template.selectOne(
            Query.query(Criteria.where("username").is(username)),
            User.class
        );
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return template.selectOne(
            Query.query(Criteria.where("email").is(email)),
            User.class
        );
    }

    @Override
    public Flux<User> findAll() {
        return template.select(User.class).all();
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return template.delete(
            Query.query(Criteria.where("id").is(id)),
            User.class
        ).then();
    }

    @Override
    public Mono<Boolean> existsByUsername(String username) {
        return template.exists(
            Query.query(Criteria.where("username").is(username)),
            User.class
        );
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return template.exists(
            Query.query(Criteria.where("email").is(email)),
            User.class
        );
    }
} 