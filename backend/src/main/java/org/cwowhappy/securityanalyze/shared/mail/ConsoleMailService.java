package org.cwowhappy.securityanalyze.shared.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 控制台邮件服务实现（开发环境用，将邮件内容打印到日志）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class ConsoleMailService implements MailService {

    @Override
    public void sendVerificationEmail(String to, String username, String code) {
        log.info("========================================");
        log.info("【模拟邮件发送】");
        log.info("收件人: {}", to);
        log.info("主题: 【证券分析与投资】邮箱验证码");
        log.info("内容: 您好 {}，您的邮箱验证码为: {}，有效期 30 分钟", username, code);
        log.info("========================================");
    }

    @Override
    public void sendPasswordResetEmail(String to, String username, String resetLink) {
        log.info("========================================");
        log.info("【模拟邮件发送】");
        log.info("收件人: {}", to);
        log.info("主题: 【证券分析与投资】密码重置");
        log.info("内容: 您好 {}，请点击以下链接重置密码（1小时内有效）：{}", username, resetLink);
        log.info("========================================");
    }
}
