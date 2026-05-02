package com.example.securityanalyze.auth.api;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileResponse {

    private Long id;
    private String username;
    private String realName;
    private String status;
    private String role;
    private LocalDateTime createdAt;
}
