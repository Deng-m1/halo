package com.reader.domain.user.service.impl;

import com.reader.domain.user.*;
import com.reader.domain.user.service.*;
import com.reader.domain.user.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final CaptchaService captchaService;
    private final SmsService smsService;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<Void> initiateRegistration(
            Username username,
            Password password,
            PhoneNumber phoneNumber,
            String captchaId,
            CaptchaCode captchaCode) {
        
        return validatePreRegistration(username, phoneNumber)
            .then(captchaService.validateImageCaptcha(captchaId, captchaCode))
            .filter(valid -> valid)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid captcha")))
            .then(smsService.sendVerificationCode(phoneNumber));
    }

    @Override
    public Mono<User> completeRegistration(
            Username username,
            Password password,
            PhoneNumber phoneNumber,
            SmsCode smsCode) {
        
        return validatePreRegistration(username, phoneNumber)
            .then(smsService.validateSmsCode(phoneNumber, smsCode))
            .filter(valid -> valid)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid SMS code")))
            .then(createUser(username, password, phoneNumber))
            .as(transactionalOperator::transactional);
    }

    private Mono<Void> validatePreRegistration(Username username, PhoneNumber phoneNumber) {
        return userRepository.existsByUsername(username.getValue())
            .flatMap(exists -> exists ? 
                Mono.error(new IllegalStateException("Username already exists")) :
                userRepository.existsByPhoneNumber(phoneNumber.getValue())
            )
            .flatMap(exists -> exists ? 
                Mono.error(new IllegalStateException("Phone number already exists")) :
                Mono.empty()
            );
    }

    private Mono<User> createUser(Username username, Password password, PhoneNumber phoneNumber) {
        User user = userFactory.createUser(username, password, phoneNumber);
        return userRepository.save(user);
    }
} 