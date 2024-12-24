package com.reader.interfaces.web;

import com.reader.application.service.RegistrationOrchestrator;
import com.reader.domain.user.service.CaptchaService;
import com.reader.domain.user.vo.*;
import com.reader.interfaces.web.request.*;
import com.reader.interfaces.web.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final RegistrationOrchestrator registrationOrchestrator;
    private final CaptchaService captchaService;

    @GetMapping("/captcha")
    public Mono<CaptchaResponse> generateCaptcha() {
        return captchaService.generateImageCaptcha()
            .map(CaptchaResponse::new);
    }

    @PostMapping("/register/initiate")
    public Mono<ResponseEntity<Void>> initiateRegistration(
            @Valid @RequestBody UserRegistrationRequest request) {
        return registrationOrchestrator.initiateRegistration(
                new Username(request.getUsername()),
                new Password(request.getPassword()),
                new PhoneNumber(request.getPhoneNumber()),
                request.getCaptchaId(),
                new CaptchaCode(request.getCaptchaCode())
            )
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping("/register/complete")
    public Mono<ResponseEntity<UserResponse>> completeRegistration(
            @Valid @RequestBody RegistrationVerificationRequest request) {
        return registrationOrchestrator.completeRegistration(
                new Username(request.getUsername()),
                new Password(request.getPassword()),
                new PhoneNumber(request.getPhoneNumber()),
                new SmsCode(request.getSmsCode())
            )
            .map(user -> ResponseEntity.ok(new UserResponse(user)));
    }
} 