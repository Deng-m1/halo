package com.reader.domain.user.vo;

import lombok.Value;
import java.util.regex.Pattern;

@Value
public class Email {
    String value;
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    public Email(String email) {
        if (!isValid(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.value = email;
    }
    
    private static boolean isValid(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
} 