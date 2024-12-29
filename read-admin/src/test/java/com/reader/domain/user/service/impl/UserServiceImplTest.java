package com.reader.domain.user.service.impl;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.reader.domain.user.DeviceRepository;
import com.reader.domain.user.LoginLogRepository;
import com.reader.domain.user.User;
import com.reader.domain.user.UserFactory;
import com.reader.domain.user.UserRepository;
import com.reader.domain.user.UserStatus;
import com.reader.domain.user.service.LoginAttemptService;
import com.reader.domain.user.service.LoginNotificationService;
import com.reader.infrastructure.security.JwtProperties;
import com.reader.infrastructure.security.JwtTokenProvider;
import com.reader.infrastructure.security.TokenBlacklistService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserFactory userFactory;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private LoginLogRepository loginLogRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private JwtProperties jwtProperties;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private LoginNotificationService loginNotificationService;

    private UserServiceImpl userService;
    private User testUser;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
            userRepository, userFactory, passwordEncoder, jwtTokenProvider,
            loginAttemptService, loginLogRepository, deviceRepository,
            jwtProperties, tokenBlacklistService, loginNotificationService
        );

        testUser = createTestUser();
    }

    @Nested
    @DisplayName("注册测试")
    class RegisterTests {
        
        @Test
        @DisplayName("成功注册新用户")
        void register_ShouldSucceed_WhenValidInput() {
            // Arrange
            String username = "testUser";
            String phoneNumber = "13800138000";
            String password = "password123";
            
            when(userFactory.createUser(any(), any(), any())).thenReturn(testUser);
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

            // Act & Assert
            StepVerifier.create(userService.register(username, phoneNumber, password))
                .expectNext(testUser)
                .verifyComplete();
        }

        @Test
        @DisplayName("注册失败 - 无效输入")
        void register_ShouldFail_WhenInvalidInput() {
            // Arrange
            String invalidUsername = "";
            String phoneNumber = "13800138000";
            String password = "password123";

            // Act & Assert
            StepVerifier.create(userService.register(invalidUsername, phoneNumber, password))
                .expectError(IllegalArgumentException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("登录测试")
    class LoginTests {

        @Test
        @DisplayName("成功登录")
        void login_ShouldSucceed_WhenValidCredentials() {
            // Arrange
            String username = "testUser";
            String password = "password123";
            String ip = "127.0.0.1";
            String userAgent = "Mozilla";
            String deviceId = "device123";
            String accessToken = "access123";
            String refreshToken = "refresh123";

            when(loginAttemptService.isAccountLocked(anyString())).thenReturn(Mono.just(false));
            when(userRepository.findByUsername(username)).thenReturn(Mono.just(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn(accessToken);
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn(refreshToken);
            when(loginAttemptService.clearFailedAttempts(anyString())).thenReturn(Mono.empty());
            when(deviceRepository.findByUserIdAndDeviceId(any(), anyString())).thenReturn(Mono.empty());
            when(deviceRepository.countByUserId(any())).thenReturn(Mono.just(0L));
            when(deviceRepository.save(any())).thenReturn(Mono.empty());
            when(tokenBlacklistService.registerDevice(any(), anyString(), anyString())).thenReturn(Mono.empty());
            when(loginNotificationService.checkAndNotify(any(), any(), anyString())).thenReturn(Mono.empty());
            when(loginLogRepository.save(any())).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(userService.login(username, password, ip, userAgent))
                .expectNextMatches(response -> 
                    response.getAccessToken().equals(accessToken) &&
                    response.getRefreshToken().equals(refreshToken)
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("登录失败 - 账户锁定")
        void login_ShouldFail_WhenAccountLocked() {
            // Arrange
            when(loginAttemptService.isAccountLocked(anyString())).thenReturn(Mono.just(true));
            when(loginLogRepository.save(any())).thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(userService.login("testUser", "password", "127.0.0.1", "Mozilla"))
                .expectError(RuntimeException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("Token刷新测试")
    class RefreshTokenTests {

        @Test
        @DisplayName("成功刷新token")
        void refreshToken_ShouldSucceed_WhenValidToken() {
            // Arrange
            String refreshToken = "validRefreshToken";
            String userId = "1";
            
            when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
            when(userRepository.findById(anyLong())).thenReturn(Mono.just(testUser));
            when(jwtTokenProvider.generateAccessToken(any())).thenReturn("newAccessToken");
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("newRefreshToken");

            // Act & Assert
            StepVerifier.create(userService.refreshToken(refreshToken))
                .expectNextMatches(response -> 
                    response.getAccessToken().equals("newAccessToken") &&
                    response.getRefreshToken().equals("newRefreshToken")
                )
                .verifyComplete();
        }

        @Test
        @DisplayName("刷新失败 - 无效token")
        void refreshToken_ShouldFail_WhenInvalidToken() {
            // Arrange
            when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);

            // Act & Assert
            StepVerifier.create(userService.refreshToken("invalidToken"))
                .expectError(RuntimeException.class)
                .verify();
        }
    }

    @Nested
    @DisplayName("用户管理测试")
    class UserManagementTests {

        @Test
        @DisplayName("成功更新用户资料")
        void updateProfile_ShouldSucceed() {
            // Arrange
            when(userRepository.findById(anyLong())).thenReturn(Mono.just(testUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

            // Act & Assert
            StepVerifier.create(userService.updateProfile(1L, "newNickname"))
                .expectNext(testUser)
                .verifyComplete();
        }

        @Test
        @DisplayName("成功修改密码")
        void changePassword_ShouldSucceed() {
            // Arrange
            String validOldPassword = "OldPass123!@#";  // 符合密码规则的旧密码
            String validNewPassword = "NewPass123!@#";  // 符合密码规则的新密码
            
            when(userRepository.findById(anyLong())).thenReturn(Mono.just(testUser));
            when(passwordEncoder.matches(eq(validOldPassword), anyString())).thenReturn(true);
            when(passwordEncoder.encode(eq(validNewPassword))).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

            // Act & Assert
            StepVerifier.create(userService.changePassword(1L, validOldPassword, validNewPassword))
                .expectNext(testUser)
                .verifyComplete();
        }

        @Test
        @DisplayName("成功停用账户")
        void deactivateAccount_ShouldSucceed() {
            // Arrange
            when(userRepository.findById(anyLong())).thenReturn(Mono.just(testUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

            // Act & Assert
            StepVerifier.create(userService.deactivateAccount(1L))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("查询测试")
    class QueryTests {

        @Test
        @DisplayName("查找所有用户")
        void findAll_ShouldReturnAllUsers() {
            // Arrange
            when(userRepository.findAll()).thenReturn(Flux.just(testUser));

            // Act & Assert
            StepVerifier.create(userService.findAll())
                .expectNext(testUser)
                .verifyComplete();
        }

        @Test
        @DisplayName("根据ID查找用户")
        void findById_ShouldReturnUser() {
            // Arrange
            when(userRepository.findById(anyLong())).thenReturn(Mono.just(testUser));

            // Act & Assert
            StepVerifier.create(userService.findById(1L))
                .expectNext(testUser)
                .verifyComplete();
        }
    }

    private User createTestUser() {
        return User.builder()
            .id(1L)
            .username("testUser")
            .password("encodedPassword")
            .phoneNumber("13800138000")
            .nickname("Test User")
            .status(UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now())

            .build();
    }
} 