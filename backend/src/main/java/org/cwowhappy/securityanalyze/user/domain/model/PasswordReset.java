package org.cwowhappy.securityanalyze.user.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 密码重置令牌领域模型。
 */
@Getter
@Builder
public class PasswordReset {

    private final Long id;
    private final String userId;
    private final String resetToken;
    private final LocalDateTime expiresAt;
    private final boolean used;
    private final LocalDateTime createdAt;
}
