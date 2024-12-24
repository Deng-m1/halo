package com.reader.domain.user.vo;

import lombok.Value;

@Value
public class SmsSendResult {
    boolean success;
    String message;
    
    public static SmsSendResult success() {
        return new SmsSendResult(true, "SMS sent successfully");
    }
    
    public static SmsSendResult failure(String message) {
        return new SmsSendResult(false, message);
    }
} 