package com.reader.interfaces.web.response;

import com.reader.domain.user.User;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String phoneNumber;
    private String nickname;
    private String status;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.phoneNumber = user.getPhoneNumber();
        this.nickname = user.getNickname();
        this.status = user.getStatus().name();
    }
} 