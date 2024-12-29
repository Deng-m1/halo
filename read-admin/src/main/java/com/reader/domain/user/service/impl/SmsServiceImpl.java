package com.reader.domain.user.service.impl;

import com.reader.domain.user.service.SmsService;
import com.reader.domain.user.vo.PhoneNumber;
import com.reader.domain.user.vo.SmsCode;
import com.reader.infrastructure.sms.SmsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {
    @Qualifier("reactiveStringRedisTemplate")
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final SmsProvider smsProvider;
    private static final String SMS_PREFIX = "sms:";
    private static final int SMS_EXPIRE_MINUTES = 5;
    private static final int SMS_RESEND_SECONDS = 60;
    
    @Override
    public Mono<Void> sendVerificationCode(PhoneNumber phoneNumber) {
        String key = SMS_PREFIX + phoneNumber.getValue();
        
        return redisTemplate.opsForValue().get(key + ":lastSent")
            .flatMap(lastSent -> {
                long elapsed = System.currentTimeMillis() - Long.parseLong(lastSent);
                if (elapsed < SMS_RESEND_SECONDS * 1000) {
                    return Mono.error(new IllegalStateException(
                        "Please wait " + (SMS_RESEND_SECONDS - elapsed/1000) + " seconds before requesting new code"
                    ));
                }
                return generateAndSendCode(phoneNumber);
            })
            .switchIfEmpty(generateAndSendCode(phoneNumber));
    }
    
    @Override
    public Mono<Boolean> validateSmsCode(PhoneNumber phoneNumber, SmsCode code) {
        String key = SMS_PREFIX + phoneNumber.getValue();
        
        return redisTemplate.opsForValue().get(key)
            .flatMap(savedCode -> {
                if (savedCode.equals(code.getValue())) {
                    return redisTemplate.delete(key)
                        .then(redisTemplate.delete(key + ":lastSent"))
                        .thenReturn(true);
                }
                return Mono.just(false);
            })
            .defaultIfEmpty(false);
    }
    
    private Mono<Void> generateAndSendCode(PhoneNumber phoneNumber) {
        String code = generateRandomCode();
        String key = SMS_PREFIX + phoneNumber.getValue();
        
        return sendSmsToPhone(phoneNumber.getValue(), code)
            .then(redisTemplate.opsForValue()
                .set(key, code, Duration.ofMinutes(SMS_EXPIRE_MINUTES)))
            .then(redisTemplate.opsForValue()
                .set(key + ":lastSent", 
                     String.valueOf(System.currentTimeMillis()),
                     Duration.ofMinutes(SMS_EXPIRE_MINUTES)))
            .then();
    }
    
    private String generateRandomCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
    
    private Mono<Void> sendSmsToPhone(String phoneNumber, String code) {
        return smsProvider.sendSms(phoneNumber, code);
    }
} 