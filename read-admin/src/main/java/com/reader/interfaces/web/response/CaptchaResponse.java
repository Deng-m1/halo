package com.reader.interfaces.web.response;

import lombok.Data;

@Data
public class CaptchaResponse {
    private String captchaId;
    private String imageBase64;

    public CaptchaResponse(String captchaId) {
        this.captchaId = captchaId;
    }
} 