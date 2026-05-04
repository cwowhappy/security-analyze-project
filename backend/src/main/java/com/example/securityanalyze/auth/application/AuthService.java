package com.example.securityanalyze.auth.application;

import com.example.securityanalyze.auth.api.AuthResponse;
import com.example.securityanalyze.auth.api.LoginRequest;
import com.example.securityanalyze.auth.api.RegisterRequest;
import com.example.securityanalyze.auth.api.UserProfileResponse;
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
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;

    @Transactional
    public void register(RegisterRequest request) {
        log.info("用户注册, username={}", request.getUsername());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("注册失败, 两次输入的密码不一致, username={}", request.getUsername());
            throw new IllegalArgumentException("两次输入的密码不一致");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("注册失败, 用户名已存在, username={}", request.getUsername());
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setStatus(UserStatus.PENDING);
        user.setRole(Role.USER);

        userRepository.save(user);
        log.info("用户注册成功, username={}", request.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        log.debug("用户登录, username={}", request.getUsername());
        User user = authenticationService.authenticate(request.getUsername(), request.getPassword());

        if (user.getStatus() == UserStatus.PENDING) {
            log.warn("登录失败, 账号待审批, username={}", request.getUsername());
            throw new PendingApprovalException("账号待审批，请联系管理员");
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            log.warn("登录失败, 账号已禁用, username={}", request.getUsername());
            throw new AccountDisabledException("账号已禁用，请联系管理员");
        }

        String token = authenticationService.generateToken(user);
        log.info("用户登录成功, username={}", request.getUsername());
        return new AuthResponse(token);
    }

    public UserProfileResponse getCurrentUser(String username) {
        log.debug("获取用户信息, username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("用户不存在"));

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setStatus(user.getStatus().name());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
