package com.reader.domain.user.service.impl;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.reader.domain.user.Device;
import com.reader.domain.user.DeviceRepository;
import com.reader.domain.user.LoginLog;
import com.reader.domain.user.LoginLogRepository;
import com.reader.domain.user.User;
import com.reader.domain.user.UserFactory;
import com.reader.domain.user.UserRepository;
import com.reader.domain.user.UserStatus;
import com.reader.domain.user.service.LoginAttemptService;
import com.reader.domain.user.service.LoginNotificationService;
import com.reader.domain.user.vo.Password;
import com.reader.domain.user.vo.PhoneNumber;
import com.reader.domain.user.vo.Username;
import com.reader.infrastructure.security.JwtProperties;
import com.reader.infrastructure.security.JwtTokenProvider;
import com.reader.infrastructure.security.TokenBlacklistService;
import com.reader.infrastructure.utils.DeviceUtils;
import com.reader.interfaces.web.response.LoginResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
@ExtendWith(MockitoExtension.class)
class UserServiceImpl2Test {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserFactory userFactory;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private LoginLogRepository loginLogRepository;
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private LoginNotificationService loginNotificationService;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    class RegisterTests {
        @Test
        void register_WithValidData_ShouldCreateUser() {
            // Arrange
            String username = "testuser";
            String phoneNumber = "13812345678";
            String password = "Password123";
            User expectedUser = User.builder()
                    .username(username)
                    .phoneNumber(phoneNumber)
                    .status(UserStatus.ACTIVE)
                    .build();

            when(userFactory.createUser(any(), any(), any())).thenReturn(expectedUser);
            when(userRepository.save(any())).thenReturn(Mono.just(expectedUser));

            // Act & Assert
            StepVerifier.create(userService.register(username, phoneNumber, password))
                    .expectNext(expectedUser)
                    .verifyComplete();
        }

        @Test
        void register_WithInvalidUsername_ShouldThrowException() {
            // Arrange
            String invalidUsername = "u"; // 太短的用户名

            // Act & Assert
            StepVerifier.create(userService.register(invalidUsername, "13812345678", "Password123"))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }
    }

    @Nested
    class LoginTests {
        @Test
        void login_WithValidCredentials_ShouldReturnLoginResponse() {
            // Arrange
            String username = "testuser";
            String password = "Password123";
            String ip = "127.0.0.1";
            String userAgent = "Mozilla";
            User user = User.builder()
                    .id(1L)
                    .username(username)
                    .status(UserStatus.ACTIVE)
                    .build();
            String deviceId = DeviceUtils.generateDeviceId(ip, userAgent);
            Device device = DeviceUtils.createDevice(deviceId, ip, userAgent);
            device.setUserId(1L);

            when(loginAttemptService.isAccountLocked(username)).thenReturn(Mono.just(false));
            when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
            when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");
            when(loginAttemptService.clearFailedAttempts(username)).thenReturn(Mono.empty());
            when(deviceRepository.findByUserIdAndDeviceId(1L, deviceId)).thenReturn(Mono.empty());
            when(deviceRepository.countByUserId(1L)).thenReturn(Mono.just(0L));
            when(deviceRepository.save(any())).thenReturn(Mono.just(device));
            when(tokenBlacklistService.registerDevice(anyLong(), anyString(), anyString()))
                    .thenReturn(Mono.just(true));
            when(loginNotificationService.checkAndNotify(any(), any(), anyString()))
                    .thenReturn(Mono.empty());
            when(loginLogRepository.save(any())).thenReturn(Mono.just(LoginLog.builder()
                .userId(1L)
                .username("testuser")
                .ip("127.0.0.1")
                .userAgent("Mozilla")
                .success(true)
                .loginTime(LocalDateTime.now())
                .build()));
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600000L);
            when(jwtProperties.getRefreshTokenExpiration()).thenReturn(2592000000L);

            // Act & Assert
            StepVerifier.create(userService.login(username, password, ip, userAgent))
                    .expectNextMatches(response ->
                            response.getAccessToken().equals("access-token") &&
                                    response.getRefreshToken().equals("refresh-token") &&
                                    response.getAccessTokenExpiresIn() == 3600000L &&
                                    response.getRefreshTokenExpiresIn() == 2592000000L
                    )
                    .verifyComplete();
        }

        @Test
        void login_WithLockedAccount_ShouldThrowException() {
            // Arrange
            String username = "testuser";
            when(loginAttemptService.isAccountLocked(username)).thenReturn(Mono.just(true));
            when(loginLogRepository.save(any())).thenReturn(Mono.just(LoginLog.builder()
                .userId(1L)
                .username("testuser")
                .ip("127.0.0.1")
                .userAgent("Mozilla")
                .success(true)
                .loginTime(LocalDateTime.now())
                .build()));

            // Act & Assert
            StepVerifier.create(userService.login(username, "password", "127.0.0.1", "Mozilla"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                                    throwable.getMessage().equals("Account is locked due to too many failed attempts")
                    )
                    .verify();
        }
    }

    @Nested
    class PasswordManagementTests {
        @Test
        void changePassword_WithValidData_ShouldUpdatePassword() {
            // Arrange
            Long userId = 1L;
            String oldPassword = "oldPass123";
            String newPassword = "newPass123";
            User user = User.builder()
                    .id(userId)
                    .password("encoded-old-password")
                    .status(UserStatus.ACTIVE)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Mono.just(user));
            when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(true);
            when(passwordEncoder.encode(any())).thenReturn("encoded-new-password");
            when(userRepository.save(any())).thenReturn(Mono.just(user));

            // Act & Assert
            StepVerifier.create(userService.changePassword(userId, oldPassword, newPassword))
                    .expectNextMatches(updatedUser ->
                            updatedUser.getPassword().equals("encoded-new-password")
                    )
                    .verifyComplete();
        }

        @Test
        void changePassword_WithInvalidOldPassword_ShouldThrowException() {
            // Arrange
            Long userId = 1L;
            User user = User.builder()
                    .id(userId)
                    .password("encoded-password")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Mono.just(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            // Act & Assert
            StepVerifier.create(userService.changePassword(userId, "wrongPass", "newPass123"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                                    throwable.getMessage().equals("Invalid old password")
                    )
                    .verify();
        }
    }

    @Nested
    class TokenManagementTests {
        @Test
        void refreshToken_WithValidToken_ShouldReturnNewLoginResponse() {
            // Arrange
            String refreshToken = "valid-refresh-token";
            User user = User.builder()
                    .id(1L)
                    .username("testuser")
                    .status(UserStatus.ACTIVE)
                    .build();

            when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
            when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Mono.just(user));
            when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new-access-token");
            when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("new-refresh-token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600000L);
            when(jwtProperties.getRefreshTokenExpiration()).thenReturn(2592000000L);

            // Act & Assert
            StepVerifier.create(userService.refreshToken(refreshToken))
                    .expectNextMatches(response ->
                            response.getAccessToken().equals("new-access-token") &&
                                    response.getRefreshToken().equals("new-refresh-token")
                    )
                    .verifyComplete();
        }

        @Test
        void refreshToken_WithInvalidToken_ShouldThrowException() {
            // Arrange
            String invalidToken = "invalid-token";
            when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

            // Act & Assert
            StepVerifier.create(userService.refreshToken(invalidToken))
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                                    throwable.getMessage().equals("Invalid refresh token")
                    )
                    .verify();
        }
    }

    @Test
    void deactivateAccount_ShouldUpdateUserStatus() {
        // Arrange
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any())).thenReturn(Mono.just(user));

        // Act & Assert
        StepVerifier.create(userService.deactivateAccount(userId))
                .verifyComplete();

        verify(userRepository).save(argThat(savedUser ->
                savedUser.getStatus() == UserStatus.INACTIVE
        ));
    }

    // Device count exceeds limit and oldest device is removed
    @Test
    public void test_login_removes_oldest_device_when_limit_exceeded() {
        // Given
        String username = "testuser";
        String password = "Pass123";
        String ip = "127.0.0.1";
        String userAgent = "Mozilla/5.0";
        String deviceId = "device123";

        User user = User.builder()
                .id(1L)
                .username(username)
                .password("encodedPass")
                .status(UserStatus.ACTIVE)
                .build();

        Device oldDevice = Device.builder()
                .userId(1L)
                .deviceId("oldDevice")
                .lastLoginTime(LocalDateTime.now().minusDays(1))
                .build();

        when(userRepository.findByUsername(username))
                .thenReturn(Mono.just(user));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);
        when(deviceRepository.countByUserId(1L))
                .thenReturn(Mono.just(3L));
        when(deviceRepository.findByUserId(1L))
                .thenReturn(Flux.just(oldDevice));
        when(deviceRepository.deleteByUserIdAndDeviceId(1L, "oldDevice"))
                .thenReturn(Mono.empty());
        when(deviceRepository.save(any(Device.class)))
                .thenReturn(Mono.just(Device.builder().build()));
        when(loginAttemptService.isAccountLocked(username))
                .thenReturn(Mono.just(false));
        when(loginAttemptService.clearFailedAttempts(username))
                .thenReturn(Mono.empty());
        when(jwtTokenProvider.generateAccessToken(any()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any()))
                .thenReturn("refresh-token");
        when(tokenBlacklistService.registerDevice(any(), any(), any()))
                .thenReturn(Mono.just(true));
        when(loginNotificationService.checkAndNotify(any(), any(), any()))
                .thenReturn(Mono.empty());
        when(loginLogRepository.save(any()))
                .thenReturn(Mono.just(LoginLog.builder().build()));

        // When
        Mono<LoginResponse> result = userService.login(username, password, ip, userAgent);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.getAccessToken().equals("access-token") &&
                                response.getRefreshToken().equals("refresh-token"))
                .verifyComplete();

        verify(deviceRepository).deleteByUserIdAndDeviceId(1L, "oldDevice");
    }

    // User profile update with valid nickname succeeds
    @Test
    public void test_update_profile_with_valid_nickname_succeeds() {
        // Given
        Long userId = 1L;
        String newNickname = "NewNickname";
        User existingUser = User.builder()
                .id(userId)
                .nickname("OldNickname")
                .status(UserStatus.ACTIVE)
                .build();
        User updatedUser = User.builder()
                .id(userId)
                .nickname(newNickname)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Mono.just(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

        // When
        Mono<User> result = userService.updateProfile(userId, newNickname);

        // Then
        StepVerifier.create(result)
                .expectNext(updatedUser)
                .verifyComplete();
    }

    // User login with correct credentials creates tokens and device record
    @Test
    public void test_login_with_correct_credentials_creates_tokens_and_device_record() {
        // Given
        String username = "testuser";
        String password = "Pass123";
        String ip = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        String deviceId = DeviceUtils.generateDeviceId(ip, userAgent);

        User user = User.builder()
                .id(1L)
                .username(username)
                .password("encodedPassword")
                .status(UserStatus.ACTIVE)
                .build();

        String accessToken = "accessToken";
        String refreshToken = "refreshToken";

        when(loginAttemptService.isAccountLocked(username)).thenReturn(Mono.just(false));
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn(refreshToken);
        when(loginAttemptService.clearFailedAttempts(username)).thenReturn(Mono.empty());
        when(tokenBlacklistService.registerDevice(user.getId(), deviceId, accessToken)).thenReturn(Mono.just(true));
        when(loginNotificationService.checkAndNotify(any(User.class), any(Device.class), eq(ip))).thenReturn(Mono.empty());
        when(deviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId)).thenReturn(Mono.empty());
        when(deviceRepository.save(any(Device.class))).thenReturn(Mono.just(Device.builder().build()));
        when(loginLogRepository.save(any(LoginLog.class))).thenReturn(Mono.just(LoginLog.builder().build()));

        // When
        Mono<LoginResponse> result = userService.login(username, password, ip, userAgent);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(accessToken, response.getAccessToken());
                    assertEquals(refreshToken, response.getRefreshToken());
                    assertNotNull(response.getUser());
                })
                .verifyComplete();
    }

    // User registration with valid username, phone number and password succeeds
    @Test
    public void test_register_with_valid_username_phone_and_password() {
        // Given
        String username = "validUser";
        String phoneNumber = "13912345678";
        String password = "ValidPass123";

        User expectedUser = User.builder()
                .username(username)
                .phoneNumber(phoneNumber)
                .password("encodedPassword")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userFactory.createUser(any(Username.class), any(Password.class), any(PhoneNumber.class)))
                .thenReturn(expectedUser);
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(expectedUser));

        // When
        Mono<User> result = userService.register(username, phoneNumber, password);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedUser)
                .verifyComplete();
    }

    // Token refresh with valid refresh token generates new access and refresh tokens
    @Test
    public void test_refresh_token_with_valid_refresh_token_generates_new_tokens() {
        // Given
        String validRefreshToken = "validRefreshToken";
        String userId = "1";
        User user = User.builder()
                .id(Long.parseLong(userId))
                .username("testuser")
                .status(UserStatus.ACTIVE)
                .build();
        String newAccessToken = "newAccessToken";
        String newRefreshToken = "newRefreshToken";

        when(jwtTokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validRefreshToken)).thenReturn(userId);
        when(userRepository.findById(Long.parseLong(userId))).thenReturn(Mono.just(user));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn(newAccessToken);
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn(newRefreshToken);

        // When
        Mono<LoginResponse> result = userService.refreshToken(validRefreshToken);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(newAccessToken, response.getAccessToken());
                    assertEquals(newRefreshToken, response.getRefreshToken());
                })
                .verifyComplete();
    }

    // Password change with correct old password updates to new valid password
    @Test
    public void test_change_password_with_correct_old_password_updates_to_new_valid_password() {
        // Given
        Long userId = 1L;
        String oldPassword = "OldPass123";
        String newPassword = "NewPass123";
        User existingUser = User.builder()
                .id(userId)
                .password("encodedOldPassword")
                .status(UserStatus.ACTIVE)
                .build();
        User updatedUser = User.builder()
                .id(userId)
                .password("encodedNewPassword")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Mono.just(existingUser));
        when(passwordEncoder.matches(oldPassword, existingUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

        // When
        Mono<User> result = userService.changePassword(userId, oldPassword, newPassword);

        // Then
        StepVerifier.create(result)
                .expectNext(updatedUser)
                .verifyComplete();
    }

    // User deactivation changes status to inactive
    @Test
    public void test_deactivate_account_changes_status_to_inactive() {
        // Given
        Long userId = 1L;
        User activeUser = User.builder()
                .id(userId)
                .status(UserStatus.ACTIVE)
                .build();
        User inactiveUser = User.builder()
                .id(userId)
                .status(UserStatus.INACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Mono.just(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(inactiveUser));

        // When
        Mono<Void> result = userService.deactivateAccount(userId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(UserStatus.INACTIVE, activeUser.getStatus());
    }

    // Pre-registration validation passes for new username and phone number
    @Test
    public void test_validate_pre_registration_with_new_username_and_phone_number() {
        // Given
        Username username = new Username("newuser");
        PhoneNumber phoneNumber = new PhoneNumber("13912345678");

        when(userRepository.existsByUsername(username.getValue()))
                .thenReturn(Mono.just(false));
        when(userRepository.existsByPhoneNumber(phoneNumber.getValue()))
                .thenReturn(Mono.just(false));

        // When
        Mono<Void> result = userService.validatePreRegistration(username, phoneNumber);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    // Login with inactive user account fails
    @Test
    public void test_login_with_inactive_user_account_fails() {
        // Given
        String username = "inactiveUser";
        String password = "password123";
        String ip = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        User inactiveUser = User.builder()
                .username(username)
                .password("encodedPassword")
                .status(UserStatus.INACTIVE)
                .build();

        when(loginAttemptService.isAccountLocked(username))
                .thenReturn(Mono.just(false));
        when(userRepository.findByUsername(username))
                .thenReturn(Mono.just(inactiveUser));
        when(loginLogRepository.save(any(LoginLog.class)))
                .thenReturn(Mono.empty());

        // When
        Mono<LoginResponse> result = userService.login(username, password, ip, userAgent);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("User is not active"))
                .verify();
    }

    // Duplicate username or phone number fails pre-registration
    @Test
    public void test_validate_pre_registration_with_duplicate_username_or_phone_number_fails() {
        // Given
        String username = "duplicateUser";
        String phoneNumber = "13812345678";
        Username usernameVo = new Username(username);
        PhoneNumber phoneNumberVo = new PhoneNumber(phoneNumber);

        when(userRepository.existsByUsername(username))
                .thenReturn(Mono.just(true));
        when(userRepository.existsByPhoneNumber(phoneNumber))
                .thenReturn(Mono.just(false));

        // When
        Mono<Void> result = userService.validatePreRegistration(usernameVo, phoneNumberVo);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Username already exists"))
                .verify();
    }

    // Device count exceeds limit and oldest device is removed
    @Test
    public void test_device_count_exceeds_limit_and_oldest_device_is_removed() {
        // Given
        Long userId = 1L;
        String deviceId = "newDeviceId";
        String ip = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        Device newDevice = DeviceUtils.createDevice(deviceId, ip, userAgent);
        newDevice.setUserId(userId);

        Device oldestDevice = Device.builder()
                .id(1L)
                .userId(userId)
                .deviceId("oldDeviceId")
                .lastLoginTime(LocalDateTime.now().minusDays(1))
                .build();

        when(deviceRepository.findByUserIdAndDeviceId(userId, deviceId))
                .thenReturn(Mono.empty());
        when(deviceRepository.countByUserId(userId))
                .thenReturn(Mono.just(3L));
        when(deviceRepository.findByUserId(userId))
                .thenReturn(Flux.just(oldestDevice));
        when(deviceRepository.deleteByUserIdAndDeviceId(oldestDevice.getUserId(), oldestDevice.getDeviceId()))
                .thenReturn(Mono.empty());
        when(deviceRepository.save(any(Device.class)))
                .thenReturn(Mono.just(newDevice));

        // When
        Mono<Device> result = userService.updateOrCreateDevice(userId, deviceId, ip, userAgent);

        // Then
        StepVerifier.create(result)
                .expectNext(newDevice)
                .verifyComplete();

        verify(deviceRepository).deleteByUserIdAndDeviceId(oldestDevice.getUserId(), oldestDevice.getDeviceId());
    }

    // Invalid password format during registration fails
    @Test
    public void test_register_with_invalid_password_format_fails() {
        // Given
        String username = "testuser";
        String phoneNumber = "13812345678";
        String invalidPassword = "12345"; // Invalid password format

        when(userFactory.createUser(any(Username.class), any(Password.class), any(PhoneNumber.class)))
                .thenThrow(new IllegalArgumentException("Invalid password format"));

        // When
        Mono<User> result = userService.register(username, phoneNumber, invalidPassword);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Invalid password format"))
                .verify();
    }

    // User not found when performing operations
    @Test
    public void test_find_by_id_user_not_found() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        // When
        Mono<User> result = userService.findById(userId);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("User not found"))
                .verify();
    }

    // Login with locked account fails
    @Test
    public void test_login_with_locked_account_fails() {
        // Given
        String username = "lockedUser";
        String password = "password";
        String ip = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        when(loginAttemptService.isAccountLocked(username))
                .thenReturn(Mono.just(true));
        when(loginLogRepository.save(any(LoginLog.class)))
                .thenReturn(Mono.empty());

        // When
        Mono<LoginResponse> result = userService.login(username, password, ip, userAgent);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Account is locked due to too many failed attempts"))
                .verify();
    }

    // Invalid refresh token throws error
    @Test
    public void test_refresh_token_with_invalid_token_throws_error() {
        // Given
        String invalidRefreshToken = "invalidToken";
        when(jwtTokenProvider.validateToken(invalidRefreshToken)).thenReturn(false);

        // When
        Mono<LoginResponse> result = userService.refreshToken(invalidRefreshToken);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Invalid refresh token"))
                .verify();
    }

    // Token blacklisting on device limit exceeded
    @Test
    public void test_blacklist_token_on_device_limit_exceeded() {
        // Given
        Long userId = 1L;
        String deviceId = "device123";
        String ip = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        String accessToken = "accessToken";

        Device newDevice = Device.builder()
                .userId(userId)
                .deviceId(deviceId)
                .lastIp(ip)
                .lastLoginTime(LocalDateTime.now())
                .build();

        when(deviceRepository.findByUserIdAndDeviceId(userId, deviceId))
                .thenReturn(Mono.empty());
        when(deviceRepository.countByUserId(userId))
                .thenReturn(Mono.just(3L));
        when(deviceRepository.findByUserId(userId))
                .thenReturn(Flux.just(newDevice));
        when(deviceRepository.deleteByUserIdAndDeviceId(anyLong(), anyString()))
                .thenReturn(Mono.empty());
        when(deviceRepository.save(any(Device.class)))
                .thenReturn(Mono.just(newDevice));
        when(tokenBlacklistService.registerDevice(userId, deviceId, accessToken))
                .thenReturn(Mono.just(false));

        // When
        Mono<Device> result = userService.updateOrCreateDevice(userId, deviceId, ip, userAgent);

        // Then
        StepVerifier.create(result)
                .expectNext(newDevice)
                .verifyComplete();

        verify(tokenBlacklistService).registerDevice(userId, deviceId, accessToken);
    }

    // Failed login attempts are logged
    @Test
    public void test_failed_login_attempts_are_logged() {
        // Given
        String username = "testuser";
        String password = "wrongPassword";
        String ip = "192.168.1.1";
        String userAgent = "Mozilla/5.0";

        User user = User.builder()
                .id(1L)
                .username(username)
                .password("encodedCorrectPassword")
                .status(UserStatus.ACTIVE)
                .build();

        when(loginAttemptService.isAccountLocked(username))
                .thenReturn(Mono.just(false));
        when(userRepository.findByUsername(username))
                .thenReturn(Mono.just(user));
        when(passwordEncoder.matches(password, user.getPassword()))
                .thenReturn(false);
        when(loginAttemptService.recordFailedAttempt(username))
                .thenReturn(Mono.empty());
        when(loginLogRepository.save(any(LoginLog.class)))
                .thenReturn(Mono.just(LoginLog.builder().build()));

        // When
        Mono<LoginResponse> result = userService.login(username, password, ip, userAgent);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Invalid password"))
                .verify();

        verify(loginAttemptService).recordFailedAttempt(username);
        verify(loginLogRepository).save(argThat(log ->
                log.getUsername().equals(username) &&
                        log.getIp().equals(ip) &&
                        log.getUserAgent().equals(userAgent) &&
                        !log.isSuccess() &&
                        log.getFailureReason().equals("Invalid password")
        ));
    }

    // Login success/failure audit logging
    @Test
    public void test_login_success_audit_logging() {
        // Given
        String username = "testuser";
        String password = "Pass123";
        String ip = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        String deviceId = DeviceUtils.generateDeviceId(ip, userAgent);

        User user = User.builder()
                .id(1L)
                .username(username)
                .password("encodedPassword")
                .status(UserStatus.ACTIVE)
                .build();

        when(loginAttemptService.isAccountLocked(username)).thenReturn(Mono.just(false));
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refreshToken");
        when(loginAttemptService.clearFailedAttempts(username)).thenReturn(Mono.empty());
        when(deviceRepository.findByUserIdAndDeviceId(user.getId(), deviceId)).thenReturn(Mono.empty());
        when(deviceRepository.countByUserId(user.getId())).thenReturn(Mono.just(0L));
        when(deviceRepository.save(any(Device.class))).thenReturn(Mono.just(Device.builder().build()));
        when(tokenBlacklistService.registerDevice(user.getId(), deviceId, "accessToken")).thenReturn(Mono.just(true));
        when(loginNotificationService.checkAndNotify(any(User.class), any(Device.class), eq(ip))).thenReturn(Mono.empty());
        when(loginLogRepository.save(any(LoginLog.class))).thenReturn(Mono.just(LoginLog.builder().build()));

        // When
        Mono<LoginResponse> result = userService.login(username, password, ip, userAgent);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals("accessToken", response.getAccessToken());
                    assertEquals("refreshToken", response.getRefreshToken());
                })
                .verifyComplete();

        verify(loginLogRepository).save(argThat(log ->
                log.getUsername().equals(username) &&
                        log.isSuccess() &&
                        log.getFailureReason() == null
        ));
    }

    // Device tracking across multiple logins
    @Test
    public void test_device_tracking_across_multiple_logins() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String password = "Pass123";
        String ip = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        String deviceId = DeviceUtils.generateDeviceId(ip, userAgent);
        User user = User.builder()
                .id(userId)
                .username(username)
                .password(passwordEncoder.encode(password))
                .status(UserStatus.ACTIVE)
                .build();
        Device device = Device.builder()
                .userId(userId)
                .deviceId(deviceId)
                .lastIp(ip)
                .lastLoginTime(LocalDateTime.now())
                .build();

        when(loginAttemptService.isAccountLocked(username)).thenReturn(Mono.just(false));
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);
        when(deviceRepository.findByUserIdAndDeviceId(userId, deviceId)).thenReturn(Mono.empty());
        when(deviceRepository.countByUserId(userId)).thenReturn(Mono.just(0L));
        when(deviceRepository.save(any(Device.class))).thenReturn(Mono.just(device));
        when(tokenBlacklistService.registerDevice(userId, deviceId, anyString())).thenReturn(Mono.just(true));
        when(loginNotificationService.checkAndNotify(any(User.class), any(Device.class), anyString())).thenReturn(Mono.empty());
        when(loginLogRepository.save(any(LoginLog.class))).thenReturn(Mono.just(LoginLog.builder().build()));

        // When
        Mono<LoginResponse> result = userService.login(username, password, ip, userAgent);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response.getAccessToken());
                    assertNotNull(response.getRefreshToken());
                    assertEquals(user.getUsername(), response.getUser().getUsername());
                })
                .verifyComplete();
    }

    // Suspicious login triggers notification
    @Test
    public void test_suspicious_login_triggers_notification() {
        // Given
        String username = "testuser";
        String password = "Pass123";
        String ip = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        String deviceId = DeviceUtils.generateDeviceId(ip, userAgent);

        User user = User.builder()
                .id(1L)
                .username(username)
                .password("encodedPassword")
                .email("testuser@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        Device device = DeviceUtils.createDevice(deviceId, ip, userAgent);

        when(loginAttemptService.isAccountLocked(username)).thenReturn(Mono.just(false));
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refreshToken");
        when(loginAttemptService.clearFailedAttempts(username)).thenReturn(Mono.empty());
        when(tokenBlacklistService.registerDevice(user.getId(), deviceId, "accessToken")).thenReturn(Mono.just(true));
        when(loginNotificationService.checkAndNotify(user, device, ip)).thenReturn(Mono.empty());
        when(userService.createLoginLog(user.getId(), username, ip, userAgent, true, null)).thenReturn(Mono.empty());

        // When
        Mono<LoginResponse> result = userService.login(username, password, ip, userAgent);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> response.getUser().getUsername().equals(username))
                .verifyComplete();

        verify(loginNotificationService).checkAndNotify(user, device, ip);
    }


    // User registration with valid username, phone number and password succeeds
    @Test
    public void test_register_with_valid_inputs_succeeds() {
        // Given
        String username = "testuser";
        String phoneNumber = "13812345678";
        String password = "Pass123";

        User expectedUser = User.builder()
                .username(username)
                .phoneNumber(phoneNumber)
                .password("encodedPassword")
                .status(UserStatus.ACTIVE)
                .build();

        when(userFactory.createUser(any(Username.class), any(Password.class), any(PhoneNumber.class)))
                .thenReturn(expectedUser);
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(expectedUser));

        // When
        Mono<User> result = userService.register(username, phoneNumber, password);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedUser)
                .verifyComplete();
    }
}