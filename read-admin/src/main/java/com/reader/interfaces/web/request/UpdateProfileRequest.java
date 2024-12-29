package com.reader.interfaces.web.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Nickname cannot be empty")
    private String nickname;
} 