package com.book.infrastructure.queue;

import com.book.common.service.FileService;
import lombok.Getter;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;

@Getter
public class BookPreProcessingTask extends ProcessingTask {
    private final FilePart file;
    private final FileService fileService;
    
    public BookPreProcessingTask(String identifier, FilePart file, FileService fileService) {
        super(identifier, file.filename(), 3); // 最多重试3次
        this.file = file;
        this.fileService = fileService;
    }

    @Override
    public Flux<Double> execute() {
        return Flux.create(sink -> {
            sink.next(0.0);
            
            // 1. 文件格式检查和转换
            sink.next(20.0);
            
            // 2. 提取元数据
            sink.next(40.0);
            
            // 3. 生成预览
            sink.next(60.0);
            
            // 4. 优化存储
            sink.next(80.0);
            
            // 5. 完成处理
            sink.next(100.0);
            sink.complete();
        });
    }
} 