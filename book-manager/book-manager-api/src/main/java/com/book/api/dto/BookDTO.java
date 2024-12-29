package com.book.api.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class BookDTO {
    private String id;
    private String title;
    private String author;
    private String description;
    private String coverUrl;
    private String status;
    private Set<String> categories = new HashSet<>();
    private Set<String> tags = new HashSet<>();
    private int wordCount;
    private int readCount;
    private double rating;
    private List<ReviewDTO> reviews = new ArrayList<>();
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime publishTime;
    private List<ChapterDTO> chapters = new ArrayList<>();
} 