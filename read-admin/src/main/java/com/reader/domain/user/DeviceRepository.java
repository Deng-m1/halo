package com.reader.domain.user;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeviceRepository extends ReactiveCrudRepository<Device, Long> {
    Flux<Device> findByUserId(Long userId);
    Mono<Device> findByUserIdAndDeviceId(Long userId, String deviceId);
    Mono<Long> countByUserId(Long userId);
    Mono<Void> deleteByUserIdAndDeviceId(Long userId, String deviceId);
} 