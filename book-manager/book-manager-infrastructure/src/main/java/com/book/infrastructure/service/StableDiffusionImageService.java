package com.book.infrastructure.service;

import com.book.domain.model.CharacterFeatures;
import com.book.infrastructure.service.AIImageGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class StableDiffusionImageService implements AIImageGenerationService {
    private final WebClient webClient;
    
    @Override
    public Mono<String> generateCharacterImage(CharacterFeatures features) {
        String prompt = buildImagePrompt(features);
        
        return webClient.post()
                .uri("/v1/images/generations")
                .bodyValue(new GenerationRequest(prompt))
                .retrieve()
                .bodyToMono(GenerationResponse.class)
                .map(response -> response.getImageUrl());
    }
    
    private String buildImagePrompt(CharacterFeatures features) {
        // �����ʺ�AI��ͼ����ʾ��
        StringBuilder prompt = new StringBuilder();
        prompt.append("A person with ");
        prompt.append(features.getGender()).append(", ");
        prompt.append(features.getAge()).append(" years old, ");
        prompt.append("wearing ").append(features.getClothing()).append(", ");
        // ... ������������
        return prompt.toString();
    }
} 