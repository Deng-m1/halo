package com.reader.domain.user.vo;

import lombok.Value;

@Value
public class Username {
    String value;
    
    public Username(String username) {
        if (!isValid(username)) {
            throw new IllegalArgumentException("Invalid username format");
        }
        this.value = username;
    }
    
    private static boolean isValid(String username) {
        return username != null && 
               username.length() >= 3 && 
               username.length() <= 20 &&
               username.matches("^[a-zA-Z0-9_-]+$");
    }
} 