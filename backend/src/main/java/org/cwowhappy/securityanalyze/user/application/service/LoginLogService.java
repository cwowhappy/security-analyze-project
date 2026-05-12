package org.cwowhappy.securityanalyze.user.application.service;

/**
 * 登录日志应用服务接口。
 */
public interface LoginLogService {

    /**
     * 记录一条登录日志。
     *
     * @param userId    用户 ID（可能为 null，如登录失败时）
     * @param username  用户名
     * @param action    动作类型
     * @param ip        IP 地址
     * @param userAgent User-Agent
     * @param details   详情
     */
    void record(String userId, String username, String action, String ip, String userAgent, String details);
}
