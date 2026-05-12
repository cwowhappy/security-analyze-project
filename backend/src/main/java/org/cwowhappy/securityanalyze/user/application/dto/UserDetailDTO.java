package org.cwowhappy.securityanalyze.user.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户详情 DTO（管理员视角）。
 */
@Getter
@Builder
public class UserDetailDTO {

    private final String id;
    private final String username;
    private final String email;
    private final String displayName;
    private final String role;
    private final String avatarInitial;
    private final boolean emailVerified;
    private final boolean locked;
    private final LocalDateTime lockedUntil;
    private final int failedLoginAttempts;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
