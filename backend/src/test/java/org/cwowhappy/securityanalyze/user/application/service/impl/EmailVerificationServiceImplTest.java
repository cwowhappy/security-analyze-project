package org.cwowhappy.securityanalyze.user.application.service.impl;

import org.cwowhappy.securityanalyze.shared.exception.ApplicationException;
import org.cwowhappy.securityanalyze.shared.mail.MailService;
import org.cwowhappy.securityanalyze.user.domain.model.EmailVerification;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.EmailVerificationRepository;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * EmailVerificationServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    private EmailVerificationRepository verificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private EmailVerificationServiceImpl emailVerificationService;

    private EmailVerification sampleVerification;

    @BeforeEach
    void setUp() {
        sampleVerification = EmailVerification.builder()
                .id(1L)
                .userId("user001")
                .verificationCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldSendVerificationEmailAndSaveCode() {
        // Act
        emailVerificationService.sendVerificationCode("user001", "test@example.com", "testuser");

        // Assert
        ArgumentCaptor<EmailVerification> captor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(verificationRepository, times(1)).save(captor.capture());
        EmailVerification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user001");
        assertThat(saved.getVerificationCode()).hasSize(6);
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(mailService, times(1)).sendVerificationEmail(eq("test@example.com"), eq("testuser"), anyString());
    }

    @Test
    void shouldReturnTrueWhenVerifyEmailSuccess() {
        // Arrange
        when(verificationRepository.findLatestByUserId(UserId.of("user001")))
                .thenReturn(Optional.of(sampleVerification));

        // Act
        boolean result = emailVerificationService.verifyEmail("user001", "123456");

        // Assert
        assertThat(result).isTrue();
        verify(verificationRepository, times(1)).markAsUsed(1L);
        verify(userRepository, times(1)).updateEmailVerified(UserId.of("user001"), true);
    }

    @Test
    void shouldThrowWhenVerificationNotFound() {
        // Arrange
        when(verificationRepository.findLatestByUserId(UserId.of("user001")))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> emailVerificationService.verifyEmail("user001", "123456"))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("验证码不存在或已过期");
    }

    @Test
    void shouldThrowWhenCodeAlreadyUsed() {
        // Arrange
        EmailVerification used = EmailVerification.builder()
                .id(1L)
                .userId("user001")
                .verificationCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(verificationRepository.findLatestByUserId(UserId.of("user001")))
                .thenReturn(Optional.of(used));

        // Act & Assert
        assertThatThrownBy(() -> emailVerificationService.verifyEmail("user001", "123456"))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("验证码已被使用");
    }

    @Test
    void shouldThrowWhenCodeExpired() {
        // Arrange
        EmailVerification expired = EmailVerification.builder()
                .id(1L)
                .userId("user001")
                .verificationCode("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(verificationRepository.findLatestByUserId(UserId.of("user001")))
                .thenReturn(Optional.of(expired));

        // Act & Assert
        assertThatThrownBy(() -> emailVerificationService.verifyEmail("user001", "123456"))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("验证码已过期");
    }

    @Test
    void shouldThrowWhenCodeMismatch() {
        // Arrange
        when(verificationRepository.findLatestByUserId(UserId.of("user001")))
                .thenReturn(Optional.of(sampleVerification));

        // Act & Assert
        assertThatThrownBy(() -> emailVerificationService.verifyEmail("user001", "999999"))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("验证码错误");
    }
}
