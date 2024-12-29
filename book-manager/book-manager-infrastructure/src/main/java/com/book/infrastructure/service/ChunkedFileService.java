package com.book.infrastructure.service;

import com.book.common.model.ChunkInfo;
import com.book.common.service.FileService;
import com.book.common.validation.FileValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChunkedFileService implements FileService {
    
    @Value("${app.upload.path}")
    private String uploadPath;
    
    @Value("${app.upload.temp-path}")
    private String tempPath;
    
    private final ConcurrentHashMap<String, ChunkInfo> chunkTracker = new ConcurrentHashMap<>();

    @Override
    public Mono<String> saveChunk(FilePart file, ChunkInfo chunkInfo) {
        FileValidator.validateFile(file);
        
        String tempFilePath = getTempFilePath(chunkInfo);
        
        return DataBufferUtils.write(file.content(), Path.of(tempFilePath), StandardOpenOption.CREATE)
                .then(Mono.defer(() -> {
                    chunkTracker.put(chunkInfo.getIdentifier(), chunkInfo);
                    
                    // 检查是否所有分片都已上传
                    if (isUploadComplete(chunkInfo.getIdentifier())) {
                        return mergeChunks(chunkInfo);
                    }
                    
                    return Mono.just(chunkInfo.getIdentifier());
                }));
    }

    private boolean isUploadComplete(String identifier) {
        ChunkInfo info = chunkTracker.get(identifier);
        if (info == null) return false;
        
        Path chunksDir = Path.of(tempPath, identifier);
        try {
            long uploadedChunks = Files.list(chunksDir).count();
            return uploadedChunks == info.getTotalChunks();
        } catch (Exception e) {
            return false;
        }
    }

    private Mono<String> mergeChunks(ChunkInfo chunkInfo) {
        String finalPath = Path.of(uploadPath, chunkInfo.getFilename()).toString();
        
        return Mono.fromCallable(() -> {
            Path chunksDir = Path.of(tempPath, chunkInfo.getIdentifier());
            
            // 按顺序合并所有分片
            try {
                try (var output = Files.newOutputStream(Path.of(finalPath), 
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    
                    for (int i = 1; i <= chunkInfo.getTotalChunks(); i++) {
                        Path chunkPath = chunksDir.resolve(String.valueOf(i));
                        Files.copy(chunkPath, output);
                    }
                }
                
                // 清理临时文件
                Files.walk(chunksDir)
                        .sorted((p1, p2) -> -p1.compareTo(p2))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception ignored) {}
                        });
                
                chunkTracker.remove(chunkInfo.getIdentifier());
                
                return finalPath;
            } catch (Exception e) {
                throw new RuntimeException("合并文件失败", e);
            }
        });
    }

    private String getTempFilePath(ChunkInfo chunkInfo) {
        return Path.of(tempPath, 
                chunkInfo.getIdentifier(), 
                String.valueOf(chunkInfo.getChunkNumber())).toString();
    }
} 