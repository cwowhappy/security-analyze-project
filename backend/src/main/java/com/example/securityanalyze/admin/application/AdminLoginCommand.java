package com.example.securityanalyze.admin.application;

import lombok.Data;

/**
 * 管理员登录指令（应用层输入对象，屏蔽 api 层 Request DTO）
 */
@Data
public class AdminLoginCommand {

    private String username;
    private String password;
}
