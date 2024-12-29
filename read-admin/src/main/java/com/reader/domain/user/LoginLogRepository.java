package com.reader.domain.user;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface LoginLogRepository extends ReactiveCrudRepository<LoginLog, Long> {
    Flux<LoginLog> findByUserId(Long userId);
} 