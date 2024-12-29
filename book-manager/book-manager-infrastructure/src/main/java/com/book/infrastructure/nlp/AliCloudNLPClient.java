package com.book.infrastructure.nlp;

import com.aliyun.nlp20180408.Client;
import com.aliyun.nlp20180408.models.*;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.book.domain.model.CharacterFeatures;
import java.util.*;

@Component
public class AliCloudNLPClient {
    private final Client client;

    public AliCloudNLPClient(
            @Value("${aliyun.accessKeyId}") String accessKeyId,
            @Value("${aliyun.accessKeySecret}") String accessKeySecret,
            @Value("${aliyun.nlp.endpoint}") String endpoint
    ) throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint);
        
        this.client = new Client(config);
    }

    public List<NLPEntity> analyzeText(String text) {
        try {
            List<NLPEntity> entities = new ArrayList<>();
            
            // 使用阿里云词法分析
            GetWsCustomizedChGeneralRequest request = new GetWsCustomizedChGeneralRequest()
                    .setServiceCode("nlp")
                    .setText(text);
            
            GetWsCustomizedChGeneralResponse response = client.getWsCustomizedChGeneral(request);
            
            // 处理分析结果
            if (response.getBody().getData() != null) {
                // 使用自定义规则识别人物描写
                identifyCharacterDescriptions(text, response.getBody().getData(), entities);
            }
            
            return entities;
        } catch (Exception e) {
            throw new RuntimeException("阿里云NLP分析失败", e);
        }
    }

    public CharacterFeatures extractFeatures(String text) {
        try {
            CharacterFeatures features = new CharacterFeatures();
            
            // 使用阿里云实体识别
            GetNerChEcomRequest request = new GetNerChEcomRequest()
                    .setServiceCode("nlp")
                    .setText(text);
            
            GetNerChEcomResponse response = client.getNerChEcom(request);
            
            if (response.getBody().getData() != null) {
                // 解析实体识别结果
                parseEntityResults(response.getBody().getData(), features);
            }
            
            // 使用情感分析获取表情/情绪
            GetSentimentRequest sentimentRequest = new GetSentimentRequest()
                    .setServiceCode("nlp")
                    .setText(text);
            
            GetSentimentResponse sentimentResponse = client.getSentiment(sentimentRequest);
            
            if (sentimentResponse.getBody().getData() != null) {
                features.setExpression(mapSentimentToExpression(sentimentResponse.getBody().getData()));
            }
            
            return features;
        } catch (Exception e) {
            throw new RuntimeException("特征提取失败", e);
        }
    }

    private void identifyCharacterDescriptions(String text, String analysisResult, List<NLPEntity> entities) {
        // 定义人物描写关键词
        Set<String> descriptionKeywords = new HashSet<>(Arrays.asList(
            "相貌", "长相", "容貌", "面容", "外表", "穿着", "打扮",
            "身材", "体型", "个子", "头发", "眼睛", "鼻子", "嘴"
        ));
        
        // 分析词法结果，识别包含描写关键词的片段
        String[] sentences = text.split("[。！？]");
        int offset = 0;
        
        for (String sentence : sentences) {
            if (containsDescriptionKeywords(sentence, descriptionKeywords)) {
                entities.add(new NLPEntity(
                    sentence,
                    offset,
                    offset + sentence.length(),
                    "CHARACTER_DESCRIPTION"
                ));
            }
            offset += sentence.length() + 1; // +1 for punctuation
        }
    }

    private boolean containsDescriptionKeywords(String text, Set<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private void parseEntityResults(String entityResult, CharacterFeatures features) {
        // 解析实体识别结果，提取特征
        Map<String, String> featureMap = new HashMap<>();
        
        // 性别识别
        if (entityResult.contains("男")) {
            features.setGender("male");
        } else if (entityResult.contains("女")) {
            features.setGender("female");
        }
        
        // 年龄特征
        extractAgeFeature(entityResult, features);
        
        // 外貌特征
        extractAppearanceFeatures(entityResult, features);
        
        // 服装特征
        extractClothingFeatures(entityResult, features);
    }

    private void extractAgeFeature(String text, CharacterFeatures features) {
        Map<String, String> agePatterns = new HashMap<>();
        agePatterns.put("青年", "young");
        agePatterns.put("中年", "middle-aged");
        agePatterns.put("老年", "elderly");
        
        for (Map.Entry<String, String> entry : agePatterns.entrySet()) {
            if (text.contains(entry.getKey())) {
                features.setAge(entry.getValue());
                break;
            }
        }
    }

    private void extractAppearanceFeatures(String text, CharacterFeatures features) {
        // 面部特征
        StringBuilder faceFeatures = new StringBuilder();
        if (text.contains("圆脸")) faceFeatures.append("round face, ");
        if (text.contains("瓜子脸")) faceFeatures.append("oval face, ");
        features.setFace(faceFeatures.toString().trim());
        
        // 头发特征
        StringBuilder hairFeatures = new StringBuilder();
        if (text.contains("长发")) hairFeatures.append("long hair, ");
        if (text.contains("短发")) hairFeatures.append("short hair, ");
        features.setHair(hairFeatures.toString().trim());
        
        // 体型特征
        StringBuilder bodyFeatures = new StringBuilder();
        if (text.contains("高个")) bodyFeatures.append("tall, ");
        if (text.contains("矮小")) bodyFeatures.append("short, ");
        features.setBodyType(bodyFeatures.toString().trim());
    }

    private void extractClothingFeatures(String text, CharacterFeatures features) {
        StringBuilder clothingFeatures = new StringBuilder();
        
        // 服装类型
        if (text.contains("裙子")) clothingFeatures.append("dress, ");
        if (text.contains("西装")) clothingFeatures.append("suit, ");
        
        // 服装颜色
        if (text.contains("白色")) clothingFeatures.append("white ");
        if (text.contains("黑色")) clothingFeatures.append("black ");
        
        features.setClothing(clothingFeatures.toString().trim());
    }

    private String mapSentimentToExpression(String sentiment) {
        // 将情感分析结果映射为表情描述
        switch (sentiment.toLowerCase()) {
            case "positive":
                return "happy, smiling";
            case "negative":
                return "sad, frowning";
            case "neutral":
                return "neutral expression";
            default:
                return "";
        }
    }
}