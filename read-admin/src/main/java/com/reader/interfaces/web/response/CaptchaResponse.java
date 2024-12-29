package com.reader.interfaces.web.response;

import com.reader.domain.user.vo.ImageCaptcha;
import lombok.Value;

@Value
public class CaptchaResponse {
    String id;
    String imageBase64;

    public CaptchaResponse(ImageCaptcha captcha) {
        this.id = captcha.getId();
        this.imageBase64 = convertImageToBase64(captcha.getImage());
    }

    private String convertImageToBase64(java.awt.image.BufferedImage image) {
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", out);
            return java.util.Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to convert image to Base64", e);
        }
    }
} 