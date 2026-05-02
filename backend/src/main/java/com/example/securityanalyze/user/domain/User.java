package com.example.securityanalyze.user.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("sys_user")
public class User {

    @Id
    private Long id;

    private String username;

    private String passwordHash;

    private String realName;

    private UserStatus status;

    private Role role;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
