package org.cwowhappy.securityanalyze.shared.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.shared.infrastructure.mail.MailProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP 邮件服务实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mail", name = "enabled", havingValue = "true")
public class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendVerificationEmail(String to, String username, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(to);
        message.setSubject("【证券分析与投资】邮箱验证码");
        message.setText(String.format("""
                您好，%s：

                您的邮箱验证码为：%s
                验证码有效期为 30 分钟，请勿泄露给他人。

                如非本人操作，请忽略此邮件。
                """, username, code));
        mailSender.send(message);
        log.info("验证码邮件已发送至: {}", to);
    }

    @Override
    public void sendPasswordResetEmail(String to, String username, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(to);
        message.setSubject("【证券分析与投资】密码重置");
        message.setText(String.format("""
                您好，%s：

                您申请了密码重置，请点击以下链接重置密码（1小时内有效）：
                %s

                如非本人操作，请忽略此邮件。
                """, username, resetLink));
        mailSender.send(message);
        log.info("密码重置邮件已发送至: {}", to);
    }
}
