package com.reader.interfaces.web.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshRequest {
    @NotBlank(message = "Refresh token cannot be empty")
    private String refreshToken;
} 