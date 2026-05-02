package com.example.securityanalyze.admin.api;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserListItem {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private String status;
    private LocalDateTime createdAt;
}
