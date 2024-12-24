package com.reader.domain.user;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Builder
@Table("users")
public class User {
    @Id
    private Long id;
    private String username;
    private String email;
    private String password;
    private String nickname;
    private String phoneNumber;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public void activate() {
        if (this.status == UserStatus.ACTIVE) {
            throw new IllegalStateException("User is already active");
        }
        this.status = UserStatus.ACTIVE;
        this.updateTimestamp();
    }
    
    public void deactivate() {
        if (this.status == UserStatus.INACTIVE) {
            throw new IllegalStateException("User is already inactive");
        }
        this.status = UserStatus.INACTIVE;
        this.updateTimestamp();
    }
    
    public void updateNickname(String newNickname) {
        if (newNickname == null || newNickname.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be empty");
        }
        this.nickname = newNickname;
        this.updateTimestamp();
    }
    
    public void updatePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.password = encodedPassword;
        this.updateTimestamp();
    }
    
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }
} 