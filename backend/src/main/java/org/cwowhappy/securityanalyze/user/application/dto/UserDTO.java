package org.cwowhappy.securityanalyze.user.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 用户 DTO。
 */
@Getter
@Builder
public class UserDTO {

    private final String id;
    private final String username;
    private final String email;
    private final String displayName;
    private final String role;
    private final String avatarInitial;
}
