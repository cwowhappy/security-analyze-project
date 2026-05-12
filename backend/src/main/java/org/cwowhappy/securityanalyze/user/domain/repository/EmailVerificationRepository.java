package org.cwowhappy.securityanalyze.user.domain.repository;

import org.cwowhappy.securityanalyze.user.domain.model.EmailVerification;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;

import java.util.Optional;

/**
 * 邮箱验证码领域仓库接口。
 */
public interface EmailVerificationRepository {

    void save(EmailVerification verification);

    Optional<EmailVerification> findLatestByUserId(UserId userId);

    void markAsUsed(long id);
}
