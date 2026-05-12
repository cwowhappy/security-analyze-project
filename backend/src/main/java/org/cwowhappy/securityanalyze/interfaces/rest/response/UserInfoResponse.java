package org.cwowhappy.securityanalyze.interfaces.rest.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 用户信息响应 DTO。
 */
@Getter
@Builder
public class UserInfoResponse {

    private final String id;
    private final String username;
    private final String email;
    private final String role;
    private final String displayName;
    private final String avatarInitial;
}
