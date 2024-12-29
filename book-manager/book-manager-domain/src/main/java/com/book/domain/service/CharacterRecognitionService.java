package com.book.domain.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CharacterRecognitionService {
    // 识别文本中的人物描写
    Flux<CharacterDescription> recognizeCharacters(String content);
    
    // 提取人物描写的关键特征
    Mono<CharacterFeatures> extractFeatures(CharacterDescription description);
} 