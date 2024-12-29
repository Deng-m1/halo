package com.reader.domain.user.service;

import com.reader.domain.user.vo.CaptchaCode;
import com.reader.domain.user.vo.ImageCaptcha;
import reactor.core.publisher.Mono;

public interface CaptchaService {
    Mono<ImageCaptcha> generateImageCaptcha();
    Mono<Boolean> validateImageCaptcha(String captchaId, CaptchaCode code);
} 