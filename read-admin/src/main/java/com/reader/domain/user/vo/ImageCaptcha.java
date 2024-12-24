package com.reader.domain.user.vo;

import lombok.Value;
import java.awt.image.BufferedImage;

@Value
public class ImageCaptcha {
    String id;
    BufferedImage image;
} 