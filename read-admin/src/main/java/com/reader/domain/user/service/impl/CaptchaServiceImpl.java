package com.reader.domain.user.service.impl;

import com.reader.domain.user.service.CaptchaService;
import com.reader.domain.user.vo.CaptchaCode;
import com.reader.domain.user.vo.ImageCaptcha;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Random;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;
    
    @Override
    public Mono<ImageCaptcha> generateImageCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String code = generateRandomCode();
        BufferedImage image = createCaptchaImage(code);
        
        return redisTemplate.opsForValue()
            .set(CAPTCHA_PREFIX + captchaId, code, Duration.ofMinutes(CAPTCHA_EXPIRE_MINUTES))
            .thenReturn(new ImageCaptcha(captchaId, image));
    }
    
    @Override
    public Mono<Boolean> validateImageCaptcha(String captchaId, CaptchaCode code) {
        return redisTemplate.opsForValue()
            .get(CAPTCHA_PREFIX + captchaId)
            .flatMap(savedCode -> {
                if (savedCode.equalsIgnoreCase(code.getValue())) {
                    return redisTemplate.delete(CAPTCHA_PREFIX + captchaId)
                        .thenReturn(true);
                }
                return Mono.just(false);
            })
            .defaultIfEmpty(false);
    }
    
    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
    
    private BufferedImage createCaptchaImage(String code) {
        int width = 100;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // 设置背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        
        // 绘制干扰线
        g.setColor(Color.LIGHT_GRAY);
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            g.drawLine(random.nextInt(width), random.nextInt(height),
                      random.nextInt(width), random.nextInt(height));
        }
        
        // 绘制验证码
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString(code, 20, 30);
        
        g.dispose();
        return image;
    }
} 