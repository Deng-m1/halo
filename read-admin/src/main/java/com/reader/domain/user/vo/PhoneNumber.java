package com.reader.domain.user.vo;

import lombok.Value;

@Value
public class PhoneNumber {
    String value;
    
    public PhoneNumber(String phone) {
        if (!isValid(phone)) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        this.value = phone;
    }
    
    private static boolean isValid(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
} 