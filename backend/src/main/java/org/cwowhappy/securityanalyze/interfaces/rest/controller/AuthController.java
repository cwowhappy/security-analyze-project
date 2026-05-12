package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cwowhappy.securityanalyze.config.JwtTokenProvider;
import org.cwowhappy.securityanalyze.interfaces.rest.request.ForgotPasswordRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.request.LoginRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.request.RegisterRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.request.ResetPasswordRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.request.VerifyEmailRequest;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.cwowhappy.securityanalyze.interfaces.rest.response.LoginResponse;
import org.cwowhappy.securityanalyze.interfaces.rest.response.UserInfoResponse;
import org.cwowhappy.securityanalyze.shared.exception.ApplicationException;
import org.cwowhappy.securityanalyze.shared.exception.UnauthorizedException;
import org.cwowhappy.securityanalyze.user.application.command.RegisterCommand;
import org.cwowhappy.securityanalyze.user.application.dto.LoginResult;
import org.cwowhappy.securityanalyze.user.application.dto.UserDTO;
import org.cwowhappy.securityanalyze.user.application.service.AuthAppService;
import org.cwowhappy.securityanalyze.user.application.service.TokenBlacklistService;
import org.cwowhappy.securityanalyze.user.application.service.UserAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证 REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;
    private final UserAppService userAppService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResult result = authAppService.login(request.getUsername(), request.getPassword(), ip, userAgent);

        LoginResponse response = LoginResponse.builder()
                .accessToken(result.getAccessToken())
                .tokenType(result.getTokenType())
                .expiresIn(result.getExpiresIn())
                .user(toUserInfoResponse(result.getUser()))
                .build();

        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserInfoResponse>> register(@Valid @RequestBody RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApplicationException("两次输入的密码不一致");
        }

        RegisterCommand command = RegisterCommand.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .role(request.getRole())
                .build();

        UserDTO user = authAppService.register(command);
        return ResponseEntity.ok(ApiResponse.success("注册成功，请验证您的邮箱", toUserInfoResponse(user)));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        authAppService.verifyEmail(request.getUserId(), request.getCode(), ip, userAgent);
        return ResponseEntity.ok(ApiResponse.success("邮箱验证成功", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam("userId") String userId) {
        authAppService.resendVerification(userId);
        return ResponseEntity.ok(ApiResponse.success("验证码已重新发送", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String ip = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            authAppService.logout(token, ip, userAgent);
        }
        return ResponseEntity.ok(ApiResponse.success("登出成功", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("缺少有效的认证令牌");
        }

        String token = authHeader.substring(7);
        String userId = jwtTokenProvider.getUserIdFromToken(token);

        if (jwtTokenProvider.validateToken(token).isEmpty()) {
            throw new UnauthorizedException("Token 已失效，请重新登录");
        }

        if (!tokenBlacklistService.isTokenValid(token)) {
            throw new UnauthorizedException("Token 已被吊销，请重新登录");
        }

        UserDTO user = authAppService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success(toUserInfoResponse(user)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authAppService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("重置链接已发送至您的邮箱", null));
    }

    @GetMapping("/verify-reset-token")
    public ResponseEntity<ApiResponse<Void>> verifyResetToken(
            @RequestParam("token") String token) {
        authAppService.verifyResetToken(token);
        return ResponseEntity.ok(ApiResponse.success("令牌有效", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApplicationException("两次输入的密码不一致");
        }
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        authAppService.resetPassword(request.getToken(), request.getNewPassword(), ip, userAgent);
        return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkUsername(
            @RequestParam("username") String username) {
        boolean available = userAppService.isUsernameAvailable(username);
        return ResponseEntity.ok(ApiResponse.success(Map.of("available", available)));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(
            @RequestParam("email") String email) {
        boolean available = userAppService.isEmailAvailable(email);
        return ResponseEntity.ok(ApiResponse.success(Map.of("available", available)));
    }

    private UserInfoResponse toUserInfoResponse(UserDTO dto) {
        return UserInfoResponse.builder()
                .id(dto.getId())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .role(dto.getRole())
                .displayName(dto.getDisplayName())
                .avatarInitial(dto.getAvatarInitial())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
