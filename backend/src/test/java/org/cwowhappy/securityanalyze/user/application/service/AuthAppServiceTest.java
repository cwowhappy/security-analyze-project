package org.cwowhappy.securityanalyze.user.application.service;

import org.cwowhappy.securityanalyze.config.JwtTokenProvider;
import org.cwowhappy.securityanalyze.shared.exception.ApplicationException;
import org.cwowhappy.securityanalyze.shared.exception.ConflictException;
import org.cwowhappy.securityanalyze.shared.exception.UnauthorizedException;
import org.cwowhappy.securityanalyze.shared.mail.MailService;
import org.cwowhappy.securityanalyze.user.application.command.RegisterCommand;
import org.cwowhappy.securityanalyze.user.application.dto.LoginResult;
import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;
import org.cwowhappy.securityanalyze.user.application.service.impl.AuthAppServiceImpl;
import org.cwowhappy.securityanalyze.user.domain.model.PasswordReset;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.PasswordResetRepository;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 认证应用服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AuthAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private LoginAttemptRecorder loginAttemptRecorder;

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private MailService mailService;

    @Mock
    private LoginLogService loginLogService;

    @InjectMocks
    private AuthAppServiceImpl authAppService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        String validHash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123");
        sampleUser = User.builder()
                .id(UserId.of("user001"))
                .username("testuser")
                .email("test@example.com")
                .passwordHash(validHash)
                .displayName("testuser")
                .role("viewer")
                .avatarInitial("T")
                .active(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldReturnLoginResultWhenLoginSuccess() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateToken("user001", "testuser", "viewer")).thenReturn("jwt-token");

        // Act
        LoginResult result = authAppService.login("testuser", "password123", "127.0.0.1", "Mozilla/5.0");

        // Assert
        assertThat(result.getAccessToken()).isEqualTo("jwt-token");
        assertThat(result.getUser().getUsername()).isEqualTo("testuser");
        verify(loginAttemptRecorder, times(1)).recordSuccessfulLogin(any(UserId.class));
        verify(loginLogService, times(1)).record(any(), any(), eq("login_success"), any(), any(), any());
    }

    @Test
    void shouldThrowWhenLoginWithWrongPassword() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleUser));
        when(loginAttemptRecorder.recordFailedAttempt(any(UserId.class), anyString())).thenReturn(1);

        // Act & Assert
        assertThatThrownBy(() -> authAppService.login("testuser", "wrongpassword", "127.0.0.1", "Mozilla/5.0"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("用户名或密码错误");
        verify(loginLogService, times(1)).record(any(), any(), eq("login_failed"), any(), any(), any());
    }

    @Test
    void shouldRegisterUserAndSendVerification() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> ((User) inv.getArgument(0)).getId());

        RegisterCommand command = RegisterCommand.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .role("viewer")
                .build();

        // Act
        UserDTO result = authAppService.register(command);

        // Assert
        assertThat(result.getUsername()).isEqualTo("newuser");
        verify(emailVerificationService, times(1)).sendVerificationCode(any(), eq("new@example.com"), eq("newuser"));
    }

    @Test
    void shouldThrowWhenRegisterWithDuplicateUsername() {
        // Arrange
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        RegisterCommand command = RegisterCommand.builder()
                .username("existing")
                .email("new@example.com")
                .password("password123")
                .role("viewer")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> authAppService.register(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void shouldSendResetEmailWhenForgotPasswordWithExistingEmail() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        // Act
        authAppService.forgotPassword("test@example.com");

        // Assert
        verify(passwordResetRepository, times(1)).save(any(PasswordReset.class));
        verify(mailService, times(1)).sendPasswordResetEmail(eq("test@example.com"), eq("testuser"), anyString());
    }

    @Test
    void shouldSilentlyReturnWhenForgotPasswordWithNonExistingEmail() {
        // Arrange
        when(userRepository.findByEmail("none@example.com")).thenReturn(Optional.empty());

        // Act
        authAppService.forgotPassword("none@example.com");

        // Assert
        verify(passwordResetRepository, never()).save(any());
        verify(mailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void shouldPassWhenVerifyResetTokenValid() {
        // Arrange
        PasswordReset reset = PasswordReset.builder()
                .id(1L)
                .userId("user001")
                .resetToken("valid-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        when(passwordResetRepository.findByToken("valid-token")).thenReturn(Optional.of(reset));

        // Act & Assert（不抛异常即通过）
        authAppService.verifyResetToken("valid-token");
    }

    @Test
    void shouldThrowWhenVerifyResetTokenNotFound() {
        // Arrange
        when(passwordResetRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authAppService.verifyResetToken("invalid-token"))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("重置链接已失效");
    }

    @Test
    void shouldThrowWhenVerifyResetTokenExpired() {
        // Arrange
        PasswordReset reset = PasswordReset.builder()
                .id(1L)
                .userId("user001")
                .resetToken("expired-token")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();
        when(passwordResetRepository.findByToken("expired-token")).thenReturn(Optional.of(reset));

        // Act & Assert
        assertThatThrownBy(() -> authAppService.verifyResetToken("expired-token"))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    void shouldResetPasswordAndMarkTokenUsed() {
        // Arrange
        PasswordReset reset = PasswordReset.builder()
                .id(1L)
                .userId("user001")
                .resetToken("valid-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        when(passwordResetRepository.findByToken("valid-token")).thenReturn(Optional.of(reset));
        when(userRepository.findById(UserId.of("user001"))).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> ((User) inv.getArgument(0)).getId());

        // Act
        authAppService.resetPassword("valid-token", "newPassword123", "127.0.0.1", "Mozilla/5.0");

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordResetRepository, times(1)).markAsUsed(1L);
        verify(loginLogService, times(1)).record(any(), any(), eq("password_reset"), any(), any(), any());
    }

    @Test
    void shouldThrowWhenResetPasswordWithUsedToken() {
        // Arrange
        PasswordReset reset = PasswordReset.builder()
                .id(1L)
                .userId("user001")
                .resetToken("used-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(true)
                .build();
        when(passwordResetRepository.findByToken("used-token")).thenReturn(Optional.of(reset));

        // Act & Assert
        assertThatThrownBy(() -> authAppService.resetPassword("used-token", "newPassword123", "127.0.0.1", "Mozilla/5.0"))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("重置链接已失效");
    }

    @Test
    void shouldRecordLogoutLog() {
        // Arrange
        when(jwtTokenProvider.getUserIdFromToken("token")).thenReturn("user001");
        when(userRepository.findById(UserId.of("user001"))).thenReturn(Optional.of(sampleUser));

        // Act
        authAppService.logout("token", "127.0.0.1", "Mozilla/5.0");

        // Assert
        verify(tokenBlacklistService, times(1)).revokeToken("token");
        verify(loginLogService, times(1)).record(eq("user001"), eq("testuser"), eq("logout"), any(), any(), any());
    }
}
