package com.book.domain.model;

import lombok.Data;

import java.util.Map;

@Data
public class CharacterDescription {
    private String originalText;     // 原始描写文本
    private int startOffset;         // 开始位置
    private int endOffset;          // 结束位置
    private CharacterFeatures features;  // 提取的特征
    private String generatedImageUrl;    // 生成的图片URL
}

@Data
public class CharacterFeatures {
    private String gender;           // 性别
    private String age;             // 年龄
    private String face;            // 面部特征
    private String hair;            // 发型发色
    private String clothing;        // 服装
    private String bodyType;        // 体型
    private String expression;      // 表情
    private Map<String, String> additionalFeatures;  // 其他特征
} 