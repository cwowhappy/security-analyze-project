package com.example.securityanalyze.user.domain;

/**
 * Token 生成与校验接口（领域层抽象，屏蔽具体 JWT 实现）
 */
public interface TokenProvider {

    /**
     * 生成用户认证 Token
     *
     * @param username 用户名
     * @param role     角色
     * @return Token 字符串
     */
    String generateToken(String username, String role);

    /**
     * 从 Token 中提取用户名
     *
     * @param token Token 字符串
     * @return 用户名
     */
    String getUsernameFromToken(String token);

    /**
     * 从 Token 中提取角色
     *
     * @param token Token 字符串
     * @return 角色
     */
    String getRoleFromToken(String token);

    /**
     * 校验 Token 是否有效
     *
     * @param token Token 字符串
     * @return 是否有效
     */
    boolean validateToken(String token);
}
