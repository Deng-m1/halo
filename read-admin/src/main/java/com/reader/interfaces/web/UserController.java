package com.reader.interfaces.web;

import com.reader.application.service.RegistrationOrchestrator;
import com.reader.domain.user.LoginLogRepository;
import com.reader.domain.user.service.CaptchaService;
import com.reader.domain.user.service.LoginAttemptService;
import com.reader.domain.user.service.UserService;
import com.reader.domain.user.vo.*;
import com.reader.interfaces.web.request.*;
import com.reader.interfaces.web.response.*;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final RegistrationOrchestrator registrationOrchestrator;
    private final CaptchaService captchaService;
    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final LoginLogRepository loginLogRepository;

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

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            ServerWebExchange exchange) {
        
        String ip = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
            .map(address -> address.getHostString())
            .orElse("unknown");
            
        String userAgent = exchange.getRequest().getHeaders()
            .getFirst(HttpHeaders.USER_AGENT);

        return userService.login(request.getUsername(), request.getPassword(), ip, userAgent)
            .map(ResponseEntity::ok);
    }

    @PutMapping("/{userId}/profile")
    public Mono<ResponseEntity<UserResponse>> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request.getNickname())
            .map(user -> ResponseEntity.ok(new UserResponse(user)));
    }


    @PutMapping("/{userId}/password")
    public Mono<ResponseEntity<Void>> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(userId, request.getOldPassword(), request.getNewPassword())
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<UserResponse>> getUser(@PathVariable Long userId) {
        return userService.findById(userId)
            .map(user -> ResponseEntity.ok(new UserResponse(user)));
    }

    @GetMapping
    public Flux<UserResponse> getAllUsers() {
        return userService.findAll()
            .map(user -> new UserResponse(user));
    }

    @DeleteMapping("/{userId}")
    public Mono<ResponseEntity<Void>> deactivateAccount(@PathVariable Long userId) {
        return userService.deactivateAccount(userId)
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/login-attempts")
    public Mono<ResponseEntity<Map<String, Object>>> getLoginAttempts(@RequestParam String username) {
        return loginAttemptService.getFailedAttempts(username)
            .map(attempts -> {
                Map<String, Object> response = new HashMap<>();
                response.put("username", username);
                response.put("failedAttempts", attempts);
                return ResponseEntity.ok(response);
            });
    }

    @PostMapping("/token/refresh")
    public Mono<ResponseEntity<LoginResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        return userService.refreshToken(request.getRefreshToken())
            .map(ResponseEntity::ok);
    }
} 