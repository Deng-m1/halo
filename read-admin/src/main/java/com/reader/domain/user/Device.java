package com.reader.domain.user;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Builder
@Table("user_devices")
public class Device {
    @Id
    private Long id;
    private Long userId;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String lastIp;
    private LocalDateTime lastLoginTime;
    private boolean isCurrentDevice;
} 