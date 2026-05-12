package org.cwowhappy.securityanalyze.user.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.shared.exception.ApplicationException;
import org.cwowhappy.securityanalyze.shared.mail.MailService;
import org.cwowhappy.securityanalyze.user.application.service.EmailVerificationService;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.model.EmailVerification;
import org.cwowhappy.securityanalyze.user.domain.repository.EmailVerificationRepository;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * 邮箱验证应用服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final Random random = new Random();

    @Override
    @Transactional
    public void sendVerificationCode(String userId, String email, String username) {
        String code = String.format("%06d", random.nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        EmailVerification verification = EmailVerification.builder()
                .userId(userId)
                .verificationCode(code)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        verificationRepository.save(verification);

        mailService.sendVerificationEmail(email, username, code);
        log.info("邮箱验证码已生成: userId={}, email={}", userId, email);
    }

    @Override
    @Transactional
    public boolean verifyEmail(String userId, String code) {
        EmailVerification record = verificationRepository
                .findLatestByUserId(UserId.of(userId))
                .orElseThrow(() -> new ApplicationException("验证码不存在或已过期"));

        if (record.isUsed()) {
            throw new ApplicationException("验证码已被使用");
        }
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApplicationException("验证码已过期");
        }
        if (!record.getVerificationCode().equals(code)) {
            throw new ApplicationException("验证码错误");
        }

        verificationRepository.markAsUsed(record.getId());
        userRepository.updateEmailVerified(UserId.of(userId), true);
        log.info("邮箱验证成功: userId={}", userId);
        return true;
    }
}
