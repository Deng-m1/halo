package com.book.api.service;

import com.book.api.command.UploadBookCommand;
import com.book.common.event.UploadProgressEvent;
import com.book.common.service.FileService;
import com.book.infrastructure.queue.BookPreProcessingTask;
import com.book.infrastructure.queue.BookProcessingQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BookUploadService {
    private final FileService fileService;
    private final BookProcessingQueue processingQueue;

    public Mono<String> uploadBook(UploadBookCommand command) {
        String identifier = generateIdentifier(command);
        
        // 创建预处理任务
        BookPreProcessingTask task = new BookPreProcessingTask(
            identifier, 
            command.getFile(), 
            fileService
        );
        
        // 提交任务到处理队列
        processingQueue.submitTask(task);
        
        return Mono.just(identifier);
    }

    public Flux<UploadProgressEvent> getUploadProgress(String identifier) {
        return processingQueue.subscribeToProgress(identifier);
    }

    private String generateIdentifier(UploadBookCommand command) {
        return command.getFile().filename() + "_" + System.currentTimeMillis();
    }
} 