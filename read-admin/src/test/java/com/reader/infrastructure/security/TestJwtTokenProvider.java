package com.reader.infrastructure.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestJwtTokenProvider extends JwtTokenProvider {
    
    public TestJwtTokenProvider(JwtProperties jwtProperties) {
        super(jwtProperties);
    }
    
    // 可以添加一些测试专用的方法
    public String createTestToken(String userId) {
        return generateAccessToken(userId);
    }
} 