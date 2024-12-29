package com.reader.infrastructure.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.security.jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenExpiration = 3600000; // 1小时
    private long refreshTokenExpiration = 2592000000L; // 30天
    private String issuer = "reader-admin";
} 