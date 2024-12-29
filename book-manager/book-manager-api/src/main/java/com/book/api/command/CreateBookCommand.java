package com.book.api.command;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.util.Set;

@Data
public class CreateBookCommand {
    @NotBlank(message = "书名不能为空")
    private String title;
    
    @NotBlank(message = "作者不能为空")
    private String author;
    
    @NotBlank(message = "描述不能为空")
    private String description;
    
    private String coverUrl;
    private Set<String> categories;
    private Set<String> tags;
} 