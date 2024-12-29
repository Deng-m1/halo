package com.reader.interfaces.web.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Old password cannot be empty")
    private String oldPassword;
    
    @NotBlank(message = "New password cannot be empty")
    private String newPassword;
} 