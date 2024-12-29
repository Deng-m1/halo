package com.book.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {
    private String userId;
    private String content;
    private double rating;
    private LocalDateTime createTime;

    public Review(String userId, String content, double rating) {
        validateRating(rating);
        this.userId = userId;
        this.content = content;
        this.rating = rating;
        this.createTime = LocalDateTime.now();
    }

    private void validateRating(double rating) {
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("评分必须在0-5之间");
        }
    }
} 