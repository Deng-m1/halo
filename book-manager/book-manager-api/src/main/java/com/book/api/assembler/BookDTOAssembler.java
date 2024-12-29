package com.book.api.assembler;

import com.book.api.dto.BookDTO;
import com.book.api.dto.ChapterDTO;
import com.book.api.dto.ReviewDTO;
import com.book.api.command.AddChapterCommand;
import com.book.api.command.AddReviewCommand;
import com.book.domain.model.Book;
import com.book.domain.model.Chapter;
import com.book.domain.model.Review;
import org.springframework.stereotype.Component;

@Component
public class BookDTOAssembler {
    
    public BookDTO toDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setDescription(book.getDescription());
        dto.setCoverUrl(book.getCoverUrl());
        dto.setStatus(book.getStatus().name());
        dto.setCategories(book.getCategories());
        dto.setTags(book.getTags());
        dto.setWordCount(book.getWordCount());
        dto.setReadCount(book.getReadCount());
        dto.setRating(book.getRating());
        dto.setCreateTime(book.getCreateTime());
        dto.setUpdateTime(book.getUpdateTime());
        dto.setPublishTime(book.getPublishTime());
        
        // ת���½��б�
        dto.setChapters(book.getChapters().stream()
            .map(this::toChapterDTO)
            .toList());
            
        // ת�������б�
        dto.setReviews(book.getReviews().stream()
            .map(this::toReviewDTO)
            .toList());
            
        return dto;
    }
    
    public Chapter toChapter(AddChapterCommand command) {
        return new Chapter(
            command.getTitle(),
            command.getContent(),
            command.getChapterNumber()
        );
    }
    
    public Review toReview(AddReviewCommand command) {
        return new Review(
            command.getUserId(),
            command.getContent(),
            command.getRating()
        );
    }
    
    // ... ����ת������
} 