package org.cwowhappy.securityanalyze.user.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 登录日志 DTO。
 */
@Getter
@Builder
public class LoginLogDTO {

    private final Long id;
    private final String userId;
    private final String username;
    private final String action;
    private final String ip;
    private final String userAgent;
    private final String details;
    private final String timestamp;
}
