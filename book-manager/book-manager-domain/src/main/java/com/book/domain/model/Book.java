package com.book.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Document(collection = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {
    @Id
    private String id;
    private String title;
    private String author;
    private String content;
    private String description;    // 书籍描述
    private String coverUrl;       // 封面图片URL
    private BookStatus status;     // 图书状态
    private Set<String> categories = new HashSet<>();  // 图书分类
    private Set<String> tags = new HashSet<>();       // 标签
    
    private int wordCount;         // 字数统计
    private int readCount;         // 阅读次数
    private double rating;         // 评分
    private List<Review> reviews = new ArrayList<>();  // 评论列表
    
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
    private LocalDateTime publishTime;   // 发布时间
    
    // 版权信息
    private String copyright;
    private String isbn;
    
    // 章节信息
    private List<Chapter> chapters = new ArrayList<>();

    private Book(String title, String author, String description) {
        this.title = title;
        this.author = author;
        this.description = description;
        this.status = BookStatus.DRAFT;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    // 业务方法
    public void addChapter(Chapter chapter) {
        chapters.add(chapter);
        updateWordCount();
        this.updateTime = LocalDateTime.now();
    }
    
    public void updateContent(String content) {
        this.content = content;
        updateWordCount();
        this.updateTime = LocalDateTime.now();
    }
    
    public void publish() {
        if (isReadyToPublish()) {
            this.status = BookStatus.PUBLISHED;
            this.publishTime = LocalDateTime.now();
        } else {
            throw new IllegalStateException("图书未满足发布条件");
        }
    }
    
    public void addReview(Review review) {
        this.reviews.add(review);
        updateRating();
    }
    
    public void incrementReadCount() {
        this.readCount++;
    }
    
    public void addCategory(String category) {
        this.categories.add(category);
    }
    
    public void addTag(String tag) {
        this.tags.add(tag);
    }
    
    // 私有辅助方法
    private boolean isReadyToPublish() {
        return title != null && 
               author != null && 
               !chapters.isEmpty() && 
               status == BookStatus.DRAFT;
    }
    
    private void updateWordCount() {
        this.wordCount = chapters.stream()
                .mapToInt(Chapter::getWordCount)
                .sum();
    }
    
    private void updateRating() {
        this.rating = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);
    }
} 