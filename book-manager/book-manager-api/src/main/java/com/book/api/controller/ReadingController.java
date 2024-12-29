package com.book.api.controller;

import com.book.api.dto.PageContentDTO;
import com.book.api.dto.ReadingProgressDTO;
import com.book.domain.service.ReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reading")
@RequiredArgsConstructor
public class ReadingController {
    private final ReadingService readingService;
    private final ReadingDTOAssembler dtoAssembler;

    @PostMapping("/{bookId}/start")
    public Mono<ReadingProgressDTO> startReading(@PathVariable String bookId, 
                                               @RequestHeader("X-User-Id") String userId) {
        return readingService.startReading(userId, bookId)
                .map(this::toDTO);
    }

    @GetMapping("/{bookId}/progress")
    public Mono<ReadingProgressDTO> getProgress(@PathVariable String bookId, 
                                              @RequestHeader("X-User-Id") String userId) {
        return readingService.getReadingProgress(userId, bookId)
                .map(this::toDTO);
    }

    @GetMapping("/{bookId}/chapter/{chapterNumber}/page/{pageNumber}")
    public Mono<PageContentDTO> getPageContent(
            @PathVariable String bookId,
            @PathVariable int chapterNumber,
            @PathVariable int pageNumber,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "1000") int pageSize) {
        
        return Mono.zip(
                readingService.getPageContent(bookId, chapterNumber, pageNumber, pageSize),
                readingService.getReadingProgress(userId, bookId)
            )
            .map(tuple -> {
                PageContentDTO dto = dtoAssembler.toDTO(tuple.getT1());
                dto.setProgress(dtoAssembler.toDTO(tuple.getT2()));
                return dto;
            })
            .flatMap(dto -> 
                // �Զ������Ķ�����
                readingService.updateReadingProgress(
                    userId, 
                    bookId, 
                    dto.getChapterNumber(), 
                    dto.getPageNumber(), 
                    dto.getTotalPages()
                )
                .thenReturn(dto)
            );
    }

    @PutMapping("/{bookId}/progress")
    public Mono<ReadingProgressDTO> updateProgress(@PathVariable String bookId,
                                                 @RequestHeader("X-User-Id") String userId,
                                                 @RequestBody UpdateProgressRequest request) {
        return readingService.updateReadingProgress(userId, bookId, 
                request.getCurrentChapter(), request.getCurrentPage(), request.getTotalPages())
                .map(this::toDTO);
    }

    @GetMapping("/{bookId}/chapters")
    public Mono<ChapterListDTO> getChapterList(@PathVariable String bookId) {
        return readingService.getBook(bookId)
                .map(dtoAssembler::toChapterListDTO);
    }
} 