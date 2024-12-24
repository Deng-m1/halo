package com.reader.domain.user.service.impl;

import com.reader.domain.user.*;
import com.reader.domain.user.service.*;
import com.reader.domain.user.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final TransactionalOperator transactionalOperator;
    private final CaptchaService captchaService;
    private final SmsService smsService;

    @Override
    public Mono<User> register(String username, String phoneNumber, String password) {
        try {
            Username usernameVo = new Username(username);
            PhoneNumber phoneNumberVo = new PhoneNumber(phoneNumber);
            Password passwordVo = new Password(password);

            return userRepository.save(
                userFactory.createUser(usernameVo, passwordVo, phoneNumberVo)
            );
        } catch (IllegalArgumentException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> validatePreRegistration(Username username, PhoneNumber phoneNumber) {
        return userRepository.existsByUsername(username.getValue())
            .flatMap(exists -> exists ? 
                Mono.error(new RuntimeException("Username already exists")) :
                userRepository.existsByPhoneNumber(phoneNumber.getValue())
            )
            .flatMap(exists -> exists ? 
                Mono.error(new RuntimeException("Phone number already exists")) :
                Mono.empty()
            );
    }

    @Override
    public Mono<User> updateProfile(Long userId, String nickname) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
            .flatMap(user -> {
                try {
                    user.updateNickname(nickname);
                    return userRepository.save(user);
                } catch (IllegalArgumentException e) {
                    return Mono.error(e);
                }
            });
    }

    // ... 其他方法实现
} 