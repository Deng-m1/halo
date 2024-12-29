package com.reader.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    @Qualifier("reactiveStringRedisTemplate")
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "token_blacklist:";
    private static final String DEVICE_PREFIX = "user_device:";
    private static final int MAX_DEVICES = 3;

    public Mono<Void> blacklistToken(String token, long expirationMs) {
        return redisTemplate.opsForValue()
            .set(BLACKLIST_PREFIX + token, "revoked", Duration.ofMillis(expirationMs))
            .then();
    }

    public Mono<Boolean> isBlacklisted(String token) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + token);
    }

    public Mono<Boolean> registerDevice(Long userId, String deviceId, String token) {
        return redisTemplate.opsForSet().add(DEVICE_PREFIX + userId, deviceId)
            .then(redisTemplate.opsForSet().size(DEVICE_PREFIX + userId))
            .flatMap(size -> {
                if (size > MAX_DEVICES) {
                    return redisTemplate.opsForSet()
                        .pop(DEVICE_PREFIX + userId)
                        .flatMap(oldDeviceId -> blacklistToken(token, 0L))
                        .thenReturn(false);
                }
                return Mono.just(true);
            });
    }
} 