package org.cwowhappy.securityanalyze.user.application.service;

import java.time.LocalDateTime;

/**
 * Token 黑名单应用服务接口。
 */
public interface TokenBlacklistService {

    /**
     * 记录 Token（登录成功后调用）。
     */
    void recordToken(String userId, String token, LocalDateTime expiresAt);

    /**
     * 检查 Token 是否有效（存在于白名单中且未过期）。
     */
    boolean isTokenValid(String token);

    /**
     * 吊销 Token（登出时调用）。
     */
    void revokeToken(String token);
}
