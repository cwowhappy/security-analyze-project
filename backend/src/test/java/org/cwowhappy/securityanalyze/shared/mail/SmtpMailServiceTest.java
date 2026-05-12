package org.cwowhappy.securityanalyze.shared.mail;

import org.cwowhappy.securityanalyze.shared.infrastructure.mail.MailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * SMTP 邮件服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SmtpMailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailProperties mailProperties;

    @InjectMocks
    private SmtpMailService smtpMailService;

    @BeforeEach
    void setUp() {
        when(mailProperties.getFrom()).thenReturn("noreply@security-analyze.com");
    }

    @Test
    void shouldSendVerificationEmailWithCorrectContent() {
        // Act
        smtpMailService.sendVerificationEmail("user@example.com", "testuser", "123456");

        // Assert
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@security-analyze.com");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).isEqualTo("【证券分析与投资】邮箱验证码");
        assertThat(message.getText()).contains("testuser");
        assertThat(message.getText()).contains("123456");
        assertThat(message.getText()).contains("30 分钟");
    }

    @Test
    void shouldSendPasswordResetEmailWithCorrectContent() {
        // Act
        smtpMailService.sendPasswordResetEmail("user@example.com", "testuser", "http://reset.link/token");

        // Assert
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@security-analyze.com");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).isEqualTo("【证券分析与投资】密码重置");
        assertThat(message.getText()).contains("testuser");
        assertThat(message.getText()).contains("http://reset.link/token");
        assertThat(message.getText()).contains("1小时内有效");
    }
}
