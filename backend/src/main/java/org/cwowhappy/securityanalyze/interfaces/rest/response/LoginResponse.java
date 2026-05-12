package org.cwowhappy.securityanalyze.interfaces.rest.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 登录响应 DTO。
 */
@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserInfoResponse user;
}
