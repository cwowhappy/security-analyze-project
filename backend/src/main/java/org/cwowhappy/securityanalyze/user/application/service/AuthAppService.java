package org.cwowhappy.securityanalyze.user.application.service;

import org.cwowhappy.securityanalyze.user.application.command.RegisterCommand;
import org.cwowhappy.securityanalyze.user.application.dto.LoginResult;
import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;

/**
 * 认证应用服务接口。
 */
public interface AuthAppService {

    LoginResult login(String usernameOrEmail, String password, String ip, String userAgent);

    UserDTO register(RegisterCommand command);

    UserDTO getCurrentUser(String userId);

    void verifyEmail(String userId, String code, String ip, String userAgent);

    void resendVerification(String userId);

    void forgotPassword(String email);

    void verifyResetToken(String token);

    void resetPassword(String token, String newPassword, String ip, String userAgent);

    void logout(String token, String ip, String userAgent);
}
