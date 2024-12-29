package com.reader.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String ATTEMPT_PREFIX = "login_attempt:";
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    public Mono<Void> recordFailedAttempt(String username) {
        String key = ATTEMPT_PREFIX + username;
        return redisTemplate.opsForValue().increment(key)
            .flatMap(attempts -> {
                if (attempts == 1) {
                    return redisTemplate.expire(key, Duration.ofHours(24));
                }
                if (attempts >= MAX_ATTEMPTS) {
                    String lockKey = "login_lock:" + username;
                    return redisTemplate.opsForValue()
                        .set(lockKey, "locked", Duration.ofMinutes(LOCK_DURATION_MINUTES));
                }
                return Mono.just(true);
            })
            .then();
    }

    public Mono<Void> clearFailedAttempts(String username) {
        return redisTemplate.delete(ATTEMPT_PREFIX + username)
            .then(redisTemplate.delete("login_lock:" + username))
            .then();
    }

    public Mono<Boolean> isAccountLocked(String username) {
        return redisTemplate.hasKey("login_lock:" + username);
    }

    public Mono<Long> getFailedAttempts(String username) {
        return redisTemplate.opsForValue()
            .get(ATTEMPT_PREFIX + username)
            .map(Long::parseLong)
            .defaultIfEmpty(0L);
    }
} 