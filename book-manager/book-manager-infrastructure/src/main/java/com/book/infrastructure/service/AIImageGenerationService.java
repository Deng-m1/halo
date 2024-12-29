package com.book.infrastructure.service;

import com.book.domain.model.CharacterFeatures;
import reactor.core.publisher.Mono;

public interface AIImageGenerationService {
    Mono<String> generateCharacterImage(CharacterFeatures features);
} 