package com.reader.domain.user.service;

import com.reader.domain.user.Device;
import com.reader.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.reader.domain.user.service.EmailService;

@Service
@RequiredArgsConstructor
public class LoginNotificationService {
    private final EmailService emailService; // 需要实现邮件服务
    private static final int SUSPICIOUS_LOGIN_DISTANCE_KM = 100;

    public Mono<Void> checkAndNotify(User user, Device device, String ip) {
        return detectSuspiciousLogin(device, ip)
            .filter(suspicious -> suspicious)
            .flatMap(suspicious -> sendLoginNotification(user, device, ip));
    }

    private Mono<Boolean> detectSuspiciousLogin(Device device, String ip) {
        // 实现可疑登录检测逻辑，比如：
        // 1. 检查IP地址是否来自不同城市
        // 2. 检查是否是新设备
        // 3. 检查登录时间是否异常
        return Mono.just(true); // 示例实现
    }

    private Mono<Void> sendLoginNotification(User user, Device device, String ip) {
        String subject = "安全��醒：检测到新设备登录";
        String content = String.format(
            "您的账户于 %s 在新设备上登录\n设备信息：%s\nIP地址：%s",
            device.getLastLoginTime(),
            device.getDeviceName(),
            ip
        );
        return emailService.sendEmail(user.getEmail(), subject, content);
    }
} 