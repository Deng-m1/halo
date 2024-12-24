package com.reader.domain.user.vo;

import lombok.Value;

@Value
public class SmsCode {
    String value;
    
    public SmsCode(String code) {
        if (!isValid(code)) {
            throw new IllegalArgumentException("Invalid SMS code format");
        }
        this.value = code;
    }
    
    private static boolean isValid(String code) {
        return code != null && code.matches("^\\d{6}$");
    }
} 