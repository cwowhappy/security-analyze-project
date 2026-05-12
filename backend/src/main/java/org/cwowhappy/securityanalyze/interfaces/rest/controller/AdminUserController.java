package org.cwowhappy.securityanalyze.interfaces.rest.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.cwowhappy.securityanalyze.interfaces.rest.response.ApiResponse;
import org.cwowhappy.securityanalyze.interfaces.rest.support.AuthContextHelper;
import org.cwowhappy.securityanalyze.shared.dto.PageResult;
import org.cwowhappy.securityanalyze.shared.exception.ApplicationException;
import org.cwowhappy.securityanalyze.user.application.dto.UserDetailDTO;
import org.cwowhappy.securityanalyze.user.application.dto.UserListItemDTO;
import org.cwowhappy.securityanalyze.user.application.service.AdminUserAppService;
import org.cwowhappy.securityanalyze.user.domain.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员用户管理 REST 控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserAppService adminUserAppService;
    private final AuthContextHelper authContextHelper;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<UserListItemDTO>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) Boolean locked,
            HttpServletRequest request) {

        assertAdmin(request);
        PageResult<User> result = adminUserAppService.listUsers(keyword, role, emailVerified, locked, page, size);
        PageResult<UserListItemDTO> dtoResult = PageResult.<UserListItemDTO>builder()
                .list(result.getList().stream().map(this::toListItemDTO).toList())
                .total(result.getTotal())
                .page(result.getPage())
                .size(result.getSize())
                .build();
        return ResponseEntity.ok(ApiResponse.success(dtoResult));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDetailDTO>> getUserDetail(
            @PathVariable String userId,
            HttpServletRequest request) {

        assertAdmin(request);
        User user = adminUserAppService.getUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success(toDetailDTO(user)));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @PathVariable String userId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        assertAdmin(request);
        adminUserAppService.updateUser(userId, body.get("displayName"), body.get("role"));
        return ResponseEntity.ok(ApiResponse.success("用户信息更新成功", null));
    }

    @PostMapping("/{userId}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable String userId,
            HttpServletRequest request) {

        assertAdmin(request);
        adminUserAppService.unlockUser(userId);
        return ResponseEntity.ok(ApiResponse.success("账户已解锁", null));
    }

    @PostMapping("/{userId}/force-password-reset")
    public ResponseEntity<ApiResponse<Void>> forcePasswordReset(
            @PathVariable String userId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {

        assertAdmin(request);
        String reason = body != null ? body.getOrDefault("reason", "安全策略要求") : "安全策略要求";
        adminUserAppService.forcePasswordReset(userId, reason);
        return ResponseEntity.ok(ApiResponse.success("已强制用户下次登录修改密码", null));
    }

    private void assertAdmin(HttpServletRequest request) {
        var user = authContextHelper.getCurrentUser(request);
        if (!"admin".equals(user.getRole())) {
            throw new ApplicationException("权限不足，仅管理员可访问");
        }
    }

    private UserListItemDTO toListItemDTO(User user) {
        return UserListItemDTO.builder()
                .id(user.getId().getValue())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .avatarInitial(user.getAvatarInitial())
                .emailVerified(user.isEmailVerified())
                .locked(user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.LocalDateTime.now()))
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserDetailDTO toDetailDTO(User user) {
        return UserDetailDTO.builder()
                .id(user.getId().getValue())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .avatarInitial(user.getAvatarInitial())
                .emailVerified(user.isEmailVerified())
                .locked(user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.LocalDateTime.now()))
                .lockedUntil(user.getLockedUntil())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
