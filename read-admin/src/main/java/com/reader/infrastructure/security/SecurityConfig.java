package com.reader.infrastructure.security;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;

import java.util.Collections;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/users/login", "/api/users/register/**", "/api/users/captcha").permitAll()
                .anyExchange().authenticated()
            )
            .addFilterAt(bearerAuthenticationFilter(), AUTHENTICATION)
            .build();
    }

        @Bean
    public AuthenticationWebFilter bearerAuthenticationFilter() {
        ReactiveAuthenticationManager authManager = authentication -> {
            String token = (String) authentication.getCredentials();
            if (jwtTokenProvider.validateToken(token)) {
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                return Mono.just(new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
            }
            return Mono.error(new BadCredentialsException("Invalid token"));
        };

        AuthenticationWebFilter filter = new AuthenticationWebFilter(authManager);
        filter.setServerAuthenticationConverter(exchange -> {
            String token = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                return Mono.just(new UsernamePasswordAuthenticationToken(null, token.substring(7)));
            }
            return Mono.empty();
        });

        filter.setSecurityContextRepository(securityContextRepository());
        return filter;
    }

    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }
} 