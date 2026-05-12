package org.cwowhappy.securityanalyze.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录尝试记录器，使用独立事务记录失败尝试，避免被外层事务回滚。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptRecorder {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordFailedAttempt(UserId userId, String username) {
        int attempts = userRepository.incrementFailedAttempts(userId);
        log.warn("登录失败: username={}, attempts={}", username, attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
            userRepository.lockUser(userId, lockUntil);
            userRepository.resetFailedAttempts(userId);
            log.warn("账户已锁定: username={}, lockedUntil={}", username, lockUntil);
        }
        return attempts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccessfulLogin(UserId userId) {
        userRepository.resetFailedAttempts(userId);
        userRepository.updateLastLoginAt(userId);
    }
}
