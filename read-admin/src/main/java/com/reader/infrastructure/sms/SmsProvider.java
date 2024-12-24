package com.reader.infrastructure.sms;

import reactor.core.publisher.Mono;

public interface SmsProvider {
    Mono<Void> sendSms(String phoneNumber, String code);
} 