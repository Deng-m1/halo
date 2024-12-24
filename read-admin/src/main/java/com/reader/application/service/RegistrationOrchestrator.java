package com.reader.application.service;

import com.reader.domain.user.*;
import com.reader.domain.user.service.*;
import com.reader.domain.user.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RegistrationOrchestrator {
    private final UserService userService;
    private final CaptchaService captchaService;
    private final SmsService smsService;
    private final TransactionalOperator transactionalOperator;

    public Mono<Void> initiateRegistration(
            Username username, 
            Password password,
            PhoneNumber phoneNumber,
            String captchaId,
            CaptchaCode captchaCode) {
        
        return userService.validatePreRegistration(username, phoneNumber)
            .then(captchaService.validateImageCaptcha(captchaId, captchaCode))
            .filter(valid -> valid)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid captcha")))
            .then(smsService.sendVerificationCode(phoneNumber));
    }

    public Mono<User> completeRegistration(
            Username username,
            Password password,
            PhoneNumber phoneNumber,
            SmsCode smsCode) {
        
        return userService.validatePreRegistration(username, phoneNumber)
            .then(smsService.validateSmsCode(phoneNumber, smsCode))
            .filter(valid -> valid)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid SMS code")))
            .then(userService.register(username.getValue(), phoneNumber.getValue(), password.getValue()))
            .as(transactionalOperator::transactional);
    }
} 