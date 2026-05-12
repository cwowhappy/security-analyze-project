package org.cwowhappy.securityanalyze.user.domain.repository;

import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户领域仓库接口。
 */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    UserId save(User user);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void updateLastLoginAt(UserId id);

    int incrementFailedAttempts(UserId id);

    void resetFailedAttempts(UserId id);

    void lockUser(UserId id, LocalDateTime until);

    void updateEmailVerified(UserId id, boolean verified);

    java.util.List<User> findAllWithConditions(String keyword, String role, Boolean emailVerified,
                                                  Boolean locked, int offset, int limit);

    long countWithConditions(String keyword, String role, Boolean emailVerified, Boolean locked);

    void updateDisplayNameAndRole(UserId id, String displayName, String role);

    void unlock(UserId id);

    void updatePasswordExpiredAt(UserId id, java.time.LocalDateTime expiredAt);
}
