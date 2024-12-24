package com.reader.domain.user;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface UserRepository {
    Mono<User> save(User user);
    Mono<User> findById(Long id);
    Mono<User> findByUsername(String username);
    Mono<User> findByEmail(String email);
    Flux<User> findAll();
    Mono<Void> deleteById(Long id);
    Mono<Boolean> existsByUsername(String username);
    Mono<Boolean> existsByEmail(String email);
} 