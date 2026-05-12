package org.cwowhappy.securityanalyze.user.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 登录日志领域模型。
 */
@Getter
@Builder
public class LoginLog {

    private final Long id;
    private final String userId;
    private final String username;
    private final String action;
    private final String ip;
    private final String userAgent;
    private final String details;
    private final LocalDateTime createdAt;
}
