package org.cwowhappy.securityanalyze.shared.mail;

/**
 * 邮件服务接口。
 */
public interface MailService {

    /**
     * 发送邮箱验证码邮件。
     *
     * @param to       收件人邮箱
     * @param username 用户名
     * @param code     验证码
     */
    void sendVerificationEmail(String to, String username, String code);

    /**
     * 发送密码重置邮件。
     *
     * @param to        收件人邮箱
     * @param username  用户名
     * @param resetLink 重置链接
     */
    void sendPasswordResetEmail(String to, String username, String resetLink);
}
