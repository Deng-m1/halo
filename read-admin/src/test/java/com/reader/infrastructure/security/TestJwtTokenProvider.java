package com.reader.infrastructure.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.reader.domain.user.User;

@Component
@Profile("test")
public class TestJwtTokenProvider extends JwtTokenProvider {
    
    public TestJwtTokenProvider(JwtProperties jwtProperties) {
        super(jwtProperties);
    }
    
    // 可以添加一些测试专用的方法
    public String createTestToken(String userId) {
        User testUser = User.builder()
            .id(Long.parseLong(userId))
            .build();
        return generateAccessToken(testUser);
    }
} 