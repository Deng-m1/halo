package com.book.common.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface FileService {
    Mono<String> saveFile(FilePart file);
    Mono<String> parseBookContent(FilePart file);
    Mono<Void> deleteFile(String path);
} 