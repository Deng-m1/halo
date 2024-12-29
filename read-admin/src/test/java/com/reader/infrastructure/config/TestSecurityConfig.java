package com.reader.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.reader.infrastructure.security.JwtProperties;

@Configuration
@Profile("test")
@EnableConfigurationProperties(JwtProperties.class)
public class TestSecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("testSecretKeyForJwtTestingPurposesOnly123456789");
        properties.setAccessTokenExpiration(3600000L);
        properties.setRefreshTokenExpiration(2592000000L);
        properties.setIssuer("reader-admin-test");
        return properties;
    }
} 