package com.book.infrastructure.service;

import com.book.common.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class LocalFileService implements FileService {
    
    @Value("${app.upload.path}")
    private String uploadPath;

    @Override
    public Mono<String> saveFile(FilePart file) {
        String fileName = UUID.randomUUID().toString() + "_" + file.filename();
        Path path = Path.of(uploadPath, fileName);
        
        return DataBufferUtils.write(file.content(), path, StandardOpenOption.CREATE_NEW)
                .then(Mono.just(fileName));
    }

    @Override
    public Mono<String> parseBookContent(FilePart file) {
        return DataBufferUtils.join(file.content())
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                });
    }

    @Override
    public Mono<Void> deleteFile(String path) {
        return Mono.fromRunnable(() -> {
            try {
                java.nio.file.Files.deleteIfExists(Path.of(uploadPath, path));
            } catch (Exception e) {
                throw new RuntimeException("ɾ���ļ�ʧ��", e);
            }
        });
    }
} 