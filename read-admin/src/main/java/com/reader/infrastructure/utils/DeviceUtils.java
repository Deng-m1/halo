package com.reader.infrastructure.utils;

import com.reader.domain.user.Device;
import eu.bitwalker.useragentutils.UserAgent;
import org.apache.commons.codec.digest.DigestUtils;
import java.time.LocalDateTime;

public class DeviceUtils {
    
    public static String generateDeviceId(String ip, String userAgent) {
        return DigestUtils.sha256Hex(ip + userAgent);
    }
    
    public static Device createDevice(String deviceId, String ip, String userAgent) {
        UserAgent agent = UserAgent.parseUserAgentString(userAgent);
        
        return Device.builder()
            .deviceId(deviceId)
            .deviceName(parseDeviceName(agent))
            .deviceType(parseDeviceType(agent))
            .lastIp(ip)
            .lastLoginTime(LocalDateTime.now())
            .isCurrentDevice(true)
            .build();
    }
    
    public static String parseDeviceName(UserAgent agent) {
        if (agent == null || agent.getOperatingSystem() == null) {
            return "Unknown Device";
        }
        
        StringBuilder deviceName = new StringBuilder();
        
        // 添加操作系统信息
        String os = agent.getOperatingSystem().getName();
        deviceName.append(os);
        
        // 添加浏览器信息
        if (agent.getBrowser() != null) {
            deviceName.append(" - ").append(agent.getBrowser().getName());
            
            // 如果有浏览器版本信息，也添加上
            if (agent.getBrowserVersion() != null) {
                deviceName.append(" ").append(agent.getBrowserVersion());
            }
        }
        
        return deviceName.toString();
    }
    
    public static String parseDeviceType(UserAgent agent) {
        if (agent == null || agent.getOperatingSystem() == null) {
            return "unknown";
        }
        
        if (agent.getOperatingSystem().isMobileDevice()) {
            return "mobile";
        } else if (agent.getOperatingSystem().getDeviceType().toString().contains("TABLET")) {
            return "tablet";
        } else if (agent.getOperatingSystem().getDeviceType().toString().contains("GAME_CONSOLE")) {
            return "game_console";
        } else if (agent.getOperatingSystem().getDeviceType().toString().contains("SMART_TV")) {
            return "smart_tv";
        } else {
            return "desktop";
        }
    }
}
