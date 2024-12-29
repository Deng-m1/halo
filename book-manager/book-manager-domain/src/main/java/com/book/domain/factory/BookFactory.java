package com.book.domain.factory;

import com.book.domain.model.Book;
import com.book.domain.model.Chapter;
import com.book.domain.model.Review;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class BookFactory {
    
    public Book createBook(String title, String author, String description, String coverUrl, 
                          Set<String> categories, Set<String> tags) {
        // 进行业务规则验证
        validateBookCreation(title, author, description);
        
        Book book = new Book(title, author, description);
        
        if (coverUrl != null && !coverUrl.trim().isEmpty()) {
            book.setCoverUrl(coverUrl);
        }
        
        if (categories != null && !categories.isEmpty()) {
            categories.forEach(book::addCategory);
        }
        
        if (tags != null && !tags.isEmpty()) {
            tags.forEach(book::addTag);
        }
        
        return book;
    }
    
    public Chapter createChapter(String title, String content, int chapterNumber) {
        validateChapterCreation(title, content, chapterNumber);
        return new Chapter(title, content, chapterNumber);
    }
    
    public Review createReview(String userId, String content, double rating) {
        validateReviewCreation(userId, content, rating);
        return new Review(userId, content, rating);
    }
    
    private void validateBookCreation(String title, String author, String description) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("书名不能为空");
        }
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("作者不能为空");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("描述不能为空");
        }
    }
    
    private void validateChapterCreation(String title, String content, int chapterNumber) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("章节标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("章节内容不能为空");
        }
        if (chapterNumber < 0) {
            throw new IllegalArgumentException("章节编号不能为负数");
        }
    }
    
    private void validateReviewCreation(String userId, String content, double rating) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("评分必须在0-5之间");
        }
    }
} 