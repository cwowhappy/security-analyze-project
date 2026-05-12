package org.cwowhappy.securityanalyze.user.application.service;

/**
 * 邮箱验证应用服务接口。
 */
public interface EmailVerificationService {

    /**
     * 发送邮箱验证码。
     */
    void sendVerificationCode(String userId, String email, String username);

    /**
     * 验证邮箱验证码。
     *
     * @return 验证是否成功
     */
    boolean verifyEmail(String userId, String code);
}
