package org.cwowhappy.securityanalyze.user.domain.repository;

import org.cwowhappy.securityanalyze.user.domain.model.PasswordReset;

import java.util.Optional;

/**
 * 密码重置令牌领域仓库接口。
 */
public interface PasswordResetRepository {

    void save(PasswordReset passwordReset);

    Optional<PasswordReset> findByToken(String token);

    void markAsUsed(Long id);
}
