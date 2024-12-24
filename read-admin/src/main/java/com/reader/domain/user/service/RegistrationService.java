package com.reader.domain.user.service;

import com.reader.domain.user.User;
import com.reader.domain.user.vo.*;
import reactor.core.publisher.Mono;

public interface RegistrationService {
    // 第一步：验证图片验证码并发送短信验证码
    Mono<Void> initiateRegistration(
        Username username,
        Password password,
        PhoneNumber phoneNumber,
        String captchaId,
        CaptchaCode captchaCode
    );
    
    // 第二步：验证短信验证码并完成注册
    Mono<User> completeRegistration(
        Username username,
        Password password,
        PhoneNumber phoneNumber,
        SmsCode smsCode
    );
} 