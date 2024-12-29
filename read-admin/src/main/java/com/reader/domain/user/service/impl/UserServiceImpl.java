package com.reader.domain.user.service.impl;

import com.reader.domain.user.*;
import com.reader.domain.user.service.*;
import com.reader.domain.user.vo.*;
import com.reader.infrastructure.security.JwtProperties;
import com.reader.infrastructure.security.JwtTokenProvider;
import com.reader.infrastructure.security.TokenBlacklistService;
import com.reader.infrastructure.utils.DeviceUtils;
import com.reader.interfaces.web.response.LoginResponse;
import lombok.RequiredArgsConstructor;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final LoginLogRepository loginLogRepository;
    private final DeviceRepository deviceRepository;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;
    private final LoginNotificationService loginNotificationService;

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
            .filter(exists -> !exists)
            .switchIfEmpty(Mono.error(new RuntimeException("Username already exists")))
            .then(userRepository.existsByPhoneNumber(phoneNumber.getValue()))
            .filter(exists -> !exists)
            .switchIfEmpty(Mono.error(new RuntimeException("Phone number already exists")))
            .then();
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

    @Override
    public Mono<LoginResponse> login(String username, String password, String ip, String userAgent) {
        String deviceId = DeviceUtils.generateDeviceId(ip, userAgent);

        return loginAttemptService.isAccountLocked(username)
            .flatMap(locked -> {
                if (locked) {
                    return createLoginLog(null, username, ip, userAgent, false, "Account locked")
                        .then(Mono.error(new RuntimeException("Account is locked due to too many failed attempts")));
                }
                
                return userRepository.findByUsername(username)
                    .switchIfEmpty(Mono.defer(() -> 
                        createLoginLog(null, username, ip, userAgent, false, "User not found")
                            .then(Mono.error(new RuntimeException("User not found")))
                    ))
                    .flatMap(user -> {
                        if (!user.isActive()) {
                            return createLoginLog(user.getId(), username, ip, userAgent, false, "User not active")
                                .then(Mono.error(new RuntimeException("User is not active")));
                        }
                        
                        if (!passwordEncoder.matches(password, user.getPassword())) {
                            return loginAttemptService.recordFailedAttempt(username)
                                .then(createLoginLog(user.getId(), username, ip, userAgent, false, "Invalid password"))
                                .then(Mono.error(new RuntimeException("Invalid password")));
                        }
                        
                        String accessToken = jwtTokenProvider.generateAccessToken(user);
                        
                        return loginAttemptService.clearFailedAttempts(username)
                            .then(updateOrCreateDevice(user.getId(), deviceId, ip, userAgent))
                            .then(tokenBlacklistService.registerDevice(user.getId(), deviceId, accessToken))
                            .then(loginNotificationService.checkAndNotify(user, DeviceUtils.createDevice(deviceId, ip, userAgent), ip))
                            .then(createLoginLog(user.getId(), username, ip, userAgent, true, null))
                            .thenReturn(new LoginResponse(
                                user,
                                accessToken,
                                jwtTokenProvider.generateRefreshToken(user),
                                jwtProperties.getAccessTokenExpiration(),
                                jwtProperties.getRefreshTokenExpiration()
                            ));
                    });
            });
    }

    @Override
    public Mono<LoginResponse> refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return Mono.error(new RuntimeException("Invalid refresh token"));
        }
        
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        return userRepository.findById(Long.parseLong(userId))
            .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
            .filter(User::isActive)
            .switchIfEmpty(Mono.error(new RuntimeException("User is not active")))
            .map(user -> new LoginResponse(
                user,
                jwtTokenProvider.generateAccessToken(user),
                jwtTokenProvider.generateRefreshToken(user),
                jwtProperties.getAccessTokenExpiration(),
                jwtProperties.getRefreshTokenExpiration()
            ));
    }

    protected Mono<LoginLog> createLoginLog(Long userId, String username, String ip,
                                            String userAgent, boolean success, String failureReason) {
        LoginLog log = LoginLog.builder()
            .userId(userId)
            .username(username)
            .ip(ip)
            .userAgent(userAgent)
            .success(success)
            .failureReason(failureReason)
            .loginTime(LocalDateTime.now())
            .build();
            
        return loginLogRepository.save(log);
    }

    @Override
    public Mono<User> changePassword(Long userId, String oldPassword, String newPassword) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
            .filter(user -> passwordEncoder.matches(oldPassword, user.getPassword()))
            .switchIfEmpty(Mono.error(new RuntimeException("Invalid old password")))
            .flatMap(user -> {
                try {
                    Password newPasswordVo = new Password(newPassword);
                    user.updatePassword(passwordEncoder.encode(newPasswordVo.getValue()));
                    return userRepository.save(user);
                } catch (IllegalArgumentException e) {
                    return Mono.error(e);
                }
            });
    }

    @Override
    public Mono<User> findById(Long id) {
        return userRepository.findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("User not found")));
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Mono<Void> deactivateAccount(Long userId) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
            .flatMap(user -> {
                user.deactivate();
                return userRepository.save(user);
            })
            .then();
    }

    public Mono<Device> updateOrCreateDevice(Long userId, String deviceId, String ip, String userAgent) {
        Device newDevice = DeviceUtils.createDevice(deviceId, ip, userAgent);
        newDevice.setUserId(userId);
        
        return deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
            .flatMap(existingDevice -> {
                existingDevice.setLastIp(ip);
                existingDevice.setLastLoginTime(LocalDateTime.now());
                return deviceRepository.save(existingDevice);
            })
            .switchIfEmpty(
                deviceRepository.countByUserId(userId)
                    .flatMap(count -> {
                        if (count >= 3) {
                            // 如果设备数超过限制，删除最旧的设备
                            return deviceRepository.findByUserId(userId)
                                .sort((d1, d2) -> d1.getLastLoginTime().compareTo(d2.getLastLoginTime()))
                                .take(1)
                                .flatMap(oldestDevice -> 
                                    deviceRepository.deleteByUserIdAndDeviceId(
                                        oldestDevice.getUserId(), 
                                        oldestDevice.getDeviceId()
                                    )
                                )
                                .then(deviceRepository.save(newDevice));
                        }
                        return deviceRepository.save(newDevice);
                    })
            );
    }

    // ... 其他方法实现
} 