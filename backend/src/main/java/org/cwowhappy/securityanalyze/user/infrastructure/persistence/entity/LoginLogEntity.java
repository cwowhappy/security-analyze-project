package org.cwowhappy.securityanalyze.user.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志 JDBC 持久化实体。
 */
@Data
public class LoginLogEntity {

    private Long id;
    private String userId;
    private String username;
    private String action;
    private String ip;
    private String userAgent;
    private String details;
    private LocalDateTime createdAt;
}
