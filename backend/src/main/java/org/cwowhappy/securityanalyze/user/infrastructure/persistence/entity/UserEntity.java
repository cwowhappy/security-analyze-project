package org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 JDBC 持久化实体，与数据库表 tb_user 映射。
 */
@Data
public class UserEntity {

    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private String role;
    private String avatarInitial;
    private boolean active;
    private boolean emailVerified;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
