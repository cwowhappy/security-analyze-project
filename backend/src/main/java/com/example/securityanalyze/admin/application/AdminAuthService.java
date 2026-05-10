package com.example.securityanalyze.admin.application;

import com.example.securityanalyze.auth.api.AuthResponse;
import com.example.securityanalyze.user.application.AuthenticationService;
import com.example.securityanalyze.user.domain.Role;
import com.example.securityanalyze.user.domain.User;
import com.example.securityanalyze.user.domain.UserRepository;
import com.example.securityanalyze.user.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;

    public AuthResponse login(AdminLoginCommand command) {
        log.debug("管理员登录, username={}", command.getUsername());
        User user = authenticationService.authenticate(command.getUsername(), command.getPassword());

        if (user.getRole() != Role.ADMIN) {
            log.warn("管理员登录失败, 非管理员角色, username={}, role={}", command.getUsername(), user.getRole());
            throw new BadCredentialsException("您不是管理员，无法登录管理后台");
        }

        String token = authenticationService.generateToken(user);
        log.info("管理员登录成功, username={}", command.getUsername());
        return new AuthResponse(token);
    }

    @Transactional
    public void registerAdmin(AdminRegisterCommand command) {
        log.info("管理员注册, username={}", command.getUsername());
        if (userRepository.existsByUsername(command.getUsername())) {
            log.warn("管理员注册失败, 用户名已存在, username={}", command.getUsername());
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(command.getUsername());
        user.setPasswordHash(passwordEncoder.encode(command.getPassword()));
        user.setRealName(command.getRealName());
        user.setStatus(UserStatus.APPROVED);
        user.setRole(Role.ADMIN);

        userRepository.save(user);
        log.info("管理员注册成功, username={}", command.getUsername());
    }
}
