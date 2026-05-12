package org.cwowhappy.securityanalyze.user.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户领域模型。
 */
@Getter
@Builder
public class User {

    private final UserId id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final String role;
    private final String avatarInitial;
    private final boolean active;
    private final boolean emailVerified;
    private final int failedLoginAttempts;
    private final LocalDateTime lockedUntil;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
