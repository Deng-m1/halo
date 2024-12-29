package com.reader.interfaces.web.response;

import com.reader.domain.user.User;
import lombok.Data;

@Data
public class LoginResponse {
    private UserResponse user;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn;
    private long refreshTokenExpiresIn;
    
    public LoginResponse(User user, String accessToken, String refreshToken, 
                        long accessTokenExpiresIn, long refreshTokenExpiresIn) {
        this.user = new UserResponse(user);
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresIn = accessTokenExpiresIn;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }
} 