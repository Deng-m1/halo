package com.reader.domain.user.service;

import com.reader.domain.user.User;
import com.reader.domain.user.vo.*;
import com.reader.interfaces.web.response.LoginResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface UserService {
    Mono<User> register(String username, String phoneNumber, String password);
    Mono<Void> validatePreRegistration(Username username, PhoneNumber phoneNumber);
    Mono<LoginResponse> login(String username, String password, String ip, String userAgent);
    Mono<User> updateProfile(Long userId, String nickname);

    Mono<LoginResponse> refreshToken(String refreshToken);

    Mono<User> changePassword(Long userId, String oldPassword, String newPassword);
    Mono<User> findById(Long id);
    Flux<User> findAll();
    Mono<Void> deactivateAccount(Long userId);
} 