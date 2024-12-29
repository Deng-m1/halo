package com.book.infrastructure.service;

import com.book.domain.model.CharacterDescription;
import com.book.domain.model.CharacterFeatures;
import com.book.domain.service.CharacterRecognitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NLPCharacterRecognitionService implements CharacterRecognitionService {
    private final NLPClient nlpClient;  // ʹ��NLP����
    
    @Override
    public Flux<CharacterDescription> recognizeCharacters(String content) {
        return Flux.fromIterable(nlpClient.analyzeText(content))
                .filter(this::isCharacterDescription)
                .map(this::createCharacterDescription);
    }
    
    @Override
    public Mono<CharacterFeatures> extractFeatures(CharacterDescription description) {
        return Mono.fromCallable(() -> 
            nlpClient.extractCharacterFeatures(description.getOriginalText())
        );
    }
    
    private boolean isCharacterDescription(NLPEntity entity) {
        // ʹ�ù���ͻ���ѧϰģ���ж��Ƿ�Ϊ������д
        return entity.getType().equals("CHARACTER_DESCRIPTION");
    }
} 