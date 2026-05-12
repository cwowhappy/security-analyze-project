package org.cwowhappy.securityanalyze.user.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 邮箱验证码领域模型。
 */
@Getter
@Builder
public class EmailVerification {

    private final Long id;
    private final String userId;
    private final String verificationCode;
    private final LocalDateTime expiresAt;
    private final LocalDateTime verifiedAt;
    private final LocalDateTime createdAt;
    private final boolean used;
}
