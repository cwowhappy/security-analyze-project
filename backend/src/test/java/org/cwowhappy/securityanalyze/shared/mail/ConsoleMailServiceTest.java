package org.cwowhappy.securityanalyze.shared.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 控制台邮件服务单元测试。
 */
@ExtendWith(OutputCaptureExtension.class)
class ConsoleMailServiceTest {

    private final ConsoleMailService consoleMailService = new ConsoleMailService();

    @Test
    void shouldLogVerificationEmailContent(CapturedOutput output) {
        // Act
        consoleMailService.sendVerificationEmail("user@example.com", "testuser", "123456");

        // Assert
        assertThat(output.getOut()).contains("【模拟邮件发送】");
        assertThat(output.getOut()).contains("收件人: user@example.com");
        assertThat(output.getOut()).contains("主题: 【证券分析与投资】邮箱验证码");
        assertThat(output.getOut()).contains("testuser");
        assertThat(output.getOut()).contains("123456");
    }

    @Test
    void shouldLogPasswordResetEmailContent(CapturedOutput output) {
        // Act
        consoleMailService.sendPasswordResetEmail("user@example.com", "testuser", "http://reset.link/token");

        // Assert
        assertThat(output.getOut()).contains("【模拟邮件发送】");
        assertThat(output.getOut()).contains("收件人: user@example.com");
        assertThat(output.getOut()).contains("主题: 【证券分析与投资】密码重置");
        assertThat(output.getOut()).contains("testuser");
        assertThat(output.getOut()).contains("http://reset.link/token");
    }
}
