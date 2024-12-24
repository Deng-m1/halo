package com.reader.interfaces.web.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class RegistrationVerificationRequest {
    @NotBlank(message = "Username cannot be empty")
    private String username;
    
    @NotBlank(message = "Password cannot be empty")
    private String password;
    
    @NotBlank(message = "Phone number cannot be empty")
    private String phoneNumber;
    
    @NotBlank(message = "SMS code cannot be empty")
    private String smsCode;
} 