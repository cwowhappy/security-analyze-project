package org.cwowhappy.securityanalyze.user.application.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 用户注册命令。
 */
@Getter
@Builder
public class RegisterCommand {

    private final String username;
    private final String email;
    private final String password;
    private final String role;
}
