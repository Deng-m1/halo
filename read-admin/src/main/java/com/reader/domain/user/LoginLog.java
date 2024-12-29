package com.reader.domain.user;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Builder
@Table("login_logs")
public class LoginLog {
    @Id
    private Long id;
    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private boolean success;
    private String failureReason;
    private LocalDateTime loginTime;
} 