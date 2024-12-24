package com.reader.domain.user.vo;

import lombok.Value;

@Value
public class CaptchaCode {
    String value;
    
    public CaptchaCode(String code) {
        if (!isValid(code)) {
            throw new IllegalArgumentException("Invalid captcha code format");
        }
        this.value = code;
    }
    
    private static boolean isValid(String code) {
        return code != null && code.matches("^[0-9A-Za-z]{4,6}$");
    }
} 