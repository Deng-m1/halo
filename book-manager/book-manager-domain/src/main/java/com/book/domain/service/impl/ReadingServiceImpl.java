package com.book.domain.service.impl;

import com.book.domain.model.*;
import com.book.domain.repository.BookRepository;
import com.book.domain.repository.ReadingProgressRepository;
import com.book.domain.service.ReadingService;
import com.book.domain.service.CharacterRecognitionService;
import com.book.domain.service.AIImageGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReadingServiceImpl implements ReadingService {
    private final BookRepository bookRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final CharacterRecognitionService characterRecognitionService;
    private final AIImageGenerationService imageGenerationService;

    @Override
    public Mono<ReadingProgress> startReading(String userId, String bookId) {
        return readingProgressRepository.findByUserIdAndBookId(userId, bookId)
                .switchIfEmpty(Mono.defer(() -> {
                    ReadingProgress progress = new ReadingProgress(userId, bookId);
                    return readingProgressRepository.save(progress);
                }));
    }

    @Override
    public Mono<ReadingProgress> getReadingProgress(String userId, String bookId) {
        return readingProgressRepository.findByUserIdAndBookId(userId, bookId)
                .switchIfEmpty(Mono.error(new IllegalStateException("未找到阅读进度")));
    }

    @Override
    public Mono<ReadingProgress> updateReadingProgress(String userId, String bookId, 
                                                     int currentChapter, int currentPage, int totalPages) {
        return readingProgressRepository.findByUserIdAndBookId(userId, bookId)
                .switchIfEmpty(Mono.error(new IllegalStateException("未找到阅读进度")))
                .map(progress -> {
                    progress.updateProgress(currentChapter, currentPage, totalPages);
                    return progress;
                })
                .flatMap(readingProgressRepository::save);
    }

    @Override
    public Mono<String> getChapterContent(String bookId, int chapterNumber) {
        return bookRepository.findById(bookId)
                .map(book -> book.getChapters().get(chapterNumber))
                .map(chapter -> chapter.getContent())
                .switchIfEmpty(Mono.error(new IllegalStateException("未找到章节内容")));
    }

    @Override
    public Mono<PageContent> getPageContent(String bookId, int chapterNumber, 
                                          int pageNumber, int pageSize) {
        return bookRepository.findById(bookId)
                .switchIfEmpty(Mono.error(new IllegalStateException("未找到图书")))
                .map(book -> {
                    // 验证章节号
                    if (chapterNumber >= book.getChapters().size()) {
                        throw new IllegalArgumentException("章节号无效");
                    }

                    Chapter chapter = book.getChapters().get(chapterNumber);
                    String content = chapter.getContent();
                    
                    // 计算分页
                    int start = pageNumber * pageSize;
                    int end = Math.min(start + pageSize, content.length());
                    
                    if (start >= content.length()) {
                        throw new IllegalArgumentException("页码超出范围");
                    }
                    
                    // 获取当前页内容
                    String pageContent = content.substring(start, end);
                    
                    // 计算分页信息
                    boolean hasNextPage = end < content.length();
                    boolean hasNextChapter = chapterNumber < book.getChapters().size() - 1;
                    int totalPages = (int) Math.ceil((double) content.length() / pageSize);
                    
                    return new PageContent(
                        pageContent,
                        pageNumber,
                        chapterNumber,
                        chapter.getTitle(),
                        hasNextPage,
                        hasNextChapter,
                        totalPages
                    );
                });
    }


    @Override
    public Mono<EnhancedPageContent> getPageContent1(String bookId, int chapterNumber, 
                                                   int pageNumber, int pageSize) {
        return getPageContent(bookId, chapterNumber, pageNumber, pageSize)
                .flatMap(pageContent -> 
                    // 识别人物描写
                    characterRecognitionService.recognizeCharacters(pageContent.getContent())
                        .flatMap(description -> 
                            // 提取特征
                            characterRecognitionService.extractFeatures(description)
                                .flatMap(features -> {
                                    description.setFeatures(features);
                                    // 生成图片
                                    return imageGenerationService.generateCharacterImage(features)
                                            .map(imageUrl -> {
                                                description.setGeneratedImageUrl(imageUrl);
                                                return description;
                                            });
                                })
                        )
                        .collectList()
                        .map(descriptions -> {
                            EnhancedPageContent enhancedContent = new EnhancedPageContent(pageContent);
                            enhancedContent.setCharacterDescriptions(descriptions);
                            return enhancedContent;
                        })
                );
    }
} 