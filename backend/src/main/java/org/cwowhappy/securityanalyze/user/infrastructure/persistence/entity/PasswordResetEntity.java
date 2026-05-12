package org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 密码重置令牌 JDBC 持久化实体。
 */
@Data
public class PasswordResetEntity {

    private Long id;
    private String userId;
    private String resetToken;
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;
}
