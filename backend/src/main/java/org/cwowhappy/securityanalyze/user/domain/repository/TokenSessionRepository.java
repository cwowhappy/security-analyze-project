package org.cwowhappy.securityanalyze.user.domain.repository;

import java.time.LocalDateTime;

/**
 * Token 会话领域仓库接口。
 */
public interface TokenSessionRepository {

    void save(String userId, String tokenHash, LocalDateTime expiresAt);

    boolean existsByTokenHash(String tokenHash);

    int deleteByTokenHash(String tokenHash);

    int deleteExpiredSessions();
}
