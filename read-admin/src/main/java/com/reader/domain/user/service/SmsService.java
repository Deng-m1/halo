package com.reader.domain.user.service;

import com.reader.domain.user.vo.PhoneNumber;
import com.reader.domain.user.vo.SmsCode;
import reactor.core.publisher.Mono;

public interface SmsService {
    Mono<Void> sendVerificationCode(PhoneNumber phoneNumber);
    Mono<Boolean> validateSmsCode(PhoneNumber phoneNumber, SmsCode code);
} 