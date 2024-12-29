package com.reader.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.reader.infrastructure.config.TestSecurityConfig;

@WebFluxTest
@Import({SecurityConfig.class, TestSecurityConfig.class})
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestJwtTokenProvider jwtTokenProvider;

    @Test
    void testSecurityConfiguration() {
        // 测试公开端点
        webTestClient.get()
                .uri("/api/users/login")
                .exchange()
                .expectStatus().isOk();

        // 测试需要认证的端点
        String token = jwtTokenProvider.createTestToken("1");
        webTestClient.get()
                .uri("/api/users/profile")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }
} 