package com.reader.domain.user.vo;

import lombok.Value;

@Value
public class Password {
    String value;
    
    public Password(String password) {
        if (!isValid(password)) {
            throw new IllegalArgumentException("Invalid password format");
        }
        this.value = password;
    }
    
    private static boolean isValid(String password) {
        return password != null && 
               password.length() >= 6 && 
               password.length() <= 32 &&
               password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$");
    }
} 