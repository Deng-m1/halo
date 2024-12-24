package com.reader.domain.user;

import com.reader.domain.user.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserFactory {
    private final PasswordEncoder passwordEncoder;

    public User createUser(Username username, Password password, PhoneNumber phoneNumber) {
        validateNewUser(username, phoneNumber);
        
        return User.builder()
                .username(username.getValue())
                .phoneNumber(phoneNumber.getValue())
                .password(passwordEncoder.encode(password.getValue()))
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void validateNewUser(Username username, PhoneNumber phoneNumber) {
        if (username == null || phoneNumber == null) {
            throw new IllegalArgumentException("Username and phone number cannot be null");
        }
    }
} 