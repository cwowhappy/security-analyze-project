package org.cwowhappy.securityanalyze.user.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.config.JwtTokenProvider;
import org.cwowhappy.securityanalyze.shared.exception.ApplicationException;
import org.cwowhappy.securityanalyze.shared.exception.ConflictException;
import org.cwowhappy.securityanalyze.shared.exception.UnauthorizedException;
import org.cwowhappy.securityanalyze.shared.mail.MailService;
import org.cwowhappy.securityanalyze.user.application.command.RegisterCommand;
import org.cwowhappy.securityanalyze.user.application.dto.LoginResult;
import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;
import org.cwowhappy.securityanalyze.user.application.service.AuthAppService;
import org.cwowhappy.securityanalyze.user.application.service.EmailVerificationService;
import org.cwowhappy.securityanalyze.user.application.service.LoginAttemptRecorder;
import org.cwowhappy.securityanalyze.user.application.service.LoginLogService;
import org.cwowhappy.securityanalyze.user.application.service.TokenBlacklistService;
import org.cwowhappy.securityanalyze.user.domain.model.PasswordReset;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.cwowhappy.securityanalyze.user.domain.model.UserId;
import org.cwowhappy.securityanalyze.user.domain.repository.PasswordResetRepository;
import org.cwowhappy.securityanalyze.user.domain.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 认证应用服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAppServiceImpl implements AuthAppService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailVerificationService emailVerificationService;
    private final LoginAttemptRecorder loginAttemptRecorder;
    private final PasswordResetRepository passwordResetRepository;
    private final MailService mailService;
    private final LoginLogService loginLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public LoginResult login(String usernameOrEmail, String password, String ip, String userAgent) {
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }

        // 用户不存在
        if (userOpt.isEmpty()) {
            loginLogService.record(null, usernameOrEmail, "login_failed", ip, userAgent, "用户名或密码错误");
            throw new UnauthorizedException("用户名或密码错误");
        }

        User user = userOpt.get();

        // 检查账户锁定
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            loginLogService.record(user.getId().getValue(), user.getUsername(), "login_failed", ip, userAgent,
                    "账户已锁定，请 " + remainingMinutes + " 分钟后重试");
            throw new UnauthorizedException("账户已锁定，请 " + remainingMinutes + " 分钟后重试");
        }

        // 检查邮箱验证
        if (!user.isEmailVerified()) {
            loginLogService.record(user.getId().getValue(), user.getUsername(), "login_failed", ip, userAgent,
                    "请先验证邮箱");
            throw new UnauthorizedException("请先验证邮箱");
        }

        // 校验密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int attempts = loginAttemptRecorder.recordFailedAttempt(user.getId(), user.getUsername());
            loginLogService.record(user.getId().getValue(), user.getUsername(), "login_failed", ip, userAgent,
                    "密码错误（第" + attempts + "次）");
            throw new UnauthorizedException("用户名或密码错误");
        }

        // 登录成功
        loginAttemptRecorder.recordSuccessfulLogin(user.getId());
        loginLogService.record(user.getId().getValue(), user.getUsername(), "login_success", ip, userAgent, "登录成功");

        String token = jwtTokenProvider.generateToken(
                user.getId().getValue(), user.getUsername(), user.getRole());
        tokenBlacklistService.recordToken(user.getId().getValue(), token,
                LocalDateTime.now().plusSeconds(86400));

        UserDTO userDTO = toDTO(user);
        return LoginResult.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(userDTO)
                .build();
    }

    @Override
    @Transactional
    public UserDTO register(RegisterCommand command) {
        if (userRepository.existsByUsername(command.getUsername())) {
            throw new ConflictException("用户名已存在",
                    Map.of("username", "该用户名已被注册"));
        }
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new ConflictException("邮箱已被注册",
                    Map.of("email", "该邮箱已被注册"));
        }

        String displayName = command.getUsername();
        String avatarInitial = displayName.substring(0, 1).toUpperCase();
        String passwordHash = passwordEncoder.encode(command.getPassword());

        User user = User.builder()
                .id(UserId.of(UUID.randomUUID().toString().replace("-", "")))
                .username(command.getUsername())
                .email(command.getEmail())
                .passwordHash(passwordHash)
                .displayName(displayName)
                .role(command.getRole())
                .avatarInitial(avatarInitial)
                .active(true)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("用户注册成功: username={}, email={}", command.getUsername(), command.getEmail());

        // 发送邮箱验证码
        emailVerificationService.sendVerificationCode(
                user.getId().getValue(), user.getEmail(), user.getUsername());

        return toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(String userId) {
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new UnauthorizedException("Token 已失效，请重新登录"));
        return toDTO(user);
    }

    @Override
    @Transactional
    public void verifyEmail(String userId, String code, String ip, String userAgent) {
        emailVerificationService.verifyEmail(userId, code);
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new UnauthorizedException("用户不存在"));
        loginLogService.record(userId, user.getUsername(), "email_verified", ip, userAgent, "邮箱验证成功");
    }

    @Override
    @Transactional
    public void resendVerification(String userId) {
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new UnauthorizedException("用户不存在"));
        emailVerificationService.sendVerificationCode(
                userId, user.getEmail(), user.getUsername());
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // 无论邮箱是否存在，均不报错，防止用户枚举
            log.warn("密码重置请求：邮箱不存在，email={}", email);
            return;
        }
        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        PasswordReset reset = PasswordReset.builder()
                .userId(user.getId().getValue())
                .resetToken(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
        passwordResetRepository.save(reset);
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        mailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetLink);
        log.info("密码重置链接已生成: userId={}", user.getId().getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyResetToken(String token) {
        PasswordReset reset = passwordResetRepository.findByToken(token)
                .orElseThrow(() -> new ApplicationException("重置链接已失效，请重新申请"));
        if (reset.isUsed()) {
            throw new ApplicationException("重置链接已失效，请重新申请");
        }
        if (reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApplicationException("重置链接已过期，请重新申请");
        }
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, String ip, String userAgent) {
        PasswordReset reset = passwordResetRepository.findByToken(token)
                .orElseThrow(() -> new ApplicationException("重置链接已失效，请重新申请"));
        if (reset.isUsed()) {
            throw new ApplicationException("重置链接已失效，请重新申请");
        }
        if (reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApplicationException("重置链接已过期，请重新申请");
        }
        User user = userRepository.findById(UserId.of(reset.getUserId()))
                .orElseThrow(() -> new ApplicationException("用户不存在"));
        String newPasswordHash = passwordEncoder.encode(newPassword);
        userRepository.save(User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .passwordHash(newPasswordHash)
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .avatarInitial(user.getAvatarInitial())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lockedUntil(user.getLockedUntil())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build());
        passwordResetRepository.markAsUsed(reset.getId());
        loginLogService.record(user.getId().getValue(), user.getUsername(), "password_reset", ip, userAgent, "邮箱重置");
        log.info("密码重置成功: userId={}", user.getId().getValue());
    }

    @Override
    @Transactional
    public void logout(String token, String ip, String userAgent) {
        tokenBlacklistService.revokeToken(token);
        try {
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            User user = userRepository.findById(UserId.of(userId)).orElse(null);
            if (user != null) {
                loginLogService.record(userId, user.getUsername(), "logout", ip, userAgent, "正常登出");
            }
        } catch (Exception e) {
            // 解析 token 失败时静默处理，不影响登出
            log.warn("登出时解析 token 失败");
        }
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId().getValue())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .avatarInitial(user.getAvatarInitial())
                .build();
    }
}
