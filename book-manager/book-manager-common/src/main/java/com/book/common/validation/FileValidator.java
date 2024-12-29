package com.book.common.validation;

import com.book.common.exception.BusinessException;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;

import java.util.Set;

public class FileValidator {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "epub", "pdf");
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public static void validateFile(FilePart file) {
        // 验证文件名
        String filename = file.filename();
        String extension = StringUtils.getFilenameExtension(filename);
        
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("INVALID_FILE_TYPE", 
                "不支持的文件类型，仅支持: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    public static void validateFileSize(long size) {
        if (size > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", 
                "文件大小超过限制，最大支持: " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
    }
} 