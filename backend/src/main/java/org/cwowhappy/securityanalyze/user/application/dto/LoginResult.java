package org.cwowhappy.securityanalyze.user.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 登录结果 DTO。
 */
@Getter
@Builder
public class LoginResult {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserDTO user;
}
