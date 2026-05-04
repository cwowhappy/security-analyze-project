package com.example.securityanalyze.admin.api;

import com.example.securityanalyze.admin.application.AdminAuthService;
import com.example.securityanalyze.auth.api.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        log.info("管理员登录请求, username={}", request.getUsername());
        AuthResponse response = adminAuthService.login(request);
        log.info("管理员登录成功, username={}", request.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        log.info("管理员注册请求, username={}", request.getUsername());
        adminAuthService.registerAdmin(request);
        log.info("管理员注册成功, username={}", request.getUsername());
        return ResponseEntity.ok().build();
    }
}
