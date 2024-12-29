package com.book.api.controller;

import com.book.api.command.UploadBookCommand;
import com.book.api.dto.BookDTO;
import com.book.api.service.BookUploadService;
import com.book.common.model.ChunkInfo;
import com.book.common.event.UploadProgressEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookUploadController {
    private final BookUploadService bookUploadService;
    private final BookDTOAssembler bookDTOAssembler;

    @PostMapping(value = "/upload/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> uploadChunk(
            @RequestPart("file") FilePart file,
            @RequestPart("chunkInfo") ChunkInfo chunkInfo) {
        return bookUploadService.saveChunk(file, chunkInfo);
    }

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<BookDTO> uploadBooks(
            @RequestPart("files") Flux<FilePart> files,
            @RequestPart("titles") String[] titles,
            @RequestPart("authors") String[] authors) {
        
        return files.zipWith(Flux.range(0, titles.length))
                .flatMap(tuple -> {
                    FilePart file = tuple.getT1();
                    int index = tuple.getT2();
                    
                    UploadBookCommand command = new UploadBookCommand();
                    command.setFile(file);
                    command.setTitle(titles[index]);
                    command.setAuthor(authors[index]);
                    
                    return bookUploadService.uploadBook(command);
                })
                .map(bookDTOAssembler::toDTO);
    }

    // 检查分片上传状态
    @GetMapping("/upload/status/{identifier}")
    public Mono<UploadStatus> checkUploadStatus(@PathVariable String identifier) {
        return bookUploadService.getUploadStatus(identifier);
    }

    @GetMapping(value = "/upload/progress/{identifier}", 
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<UploadProgressEvent> getUploadProgress(@PathVariable String identifier) {
        return bookUploadService.getUploadProgress(identifier);
    }
} 