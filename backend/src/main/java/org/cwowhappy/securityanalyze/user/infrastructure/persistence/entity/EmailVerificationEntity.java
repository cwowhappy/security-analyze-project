package org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮箱验证码 JDBC 持久化实体。
 */
@Data
public class EmailVerificationEntity {

    private Long id;
    private String userId;
    private String verificationCode;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private boolean used;
}
