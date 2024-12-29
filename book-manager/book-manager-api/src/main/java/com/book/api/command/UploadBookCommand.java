package com.book.api.command;

import lombok.Data;
import org.springframework.http.codec.multipart.FilePart;
import javax.validation.constraints.NotNull;

@Data
public class UploadBookCommand {
    @NotNull(message = "�ļ�����Ϊ��")
    private FilePart file;
    private String title;
    private String author;
    private String description;
    private String coverUrl;
} 