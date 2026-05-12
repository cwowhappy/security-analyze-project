# Feature-02 后端 API 补充实现

> 文档版本：v1.0
> 创建日期：2026-05-12
> 优先级：P1 / P2
> 说明：补充实现 P1/P2 功能所需的后端 API

---

## 一、后端现状分析

### 1.1 已有实现

| 组件 | 路径 | 状态 |
|------|------|------|
| AuthController | interfaces/rest/controller/AuthController.java | ✅ 已完成 |
| AuthAppService | user/application/service/AuthAppService.java | ✅ 已完成 |
| AuthAppServiceImpl | user/application/service/impl/AuthAppServiceImpl.java | ✅ 已完成 |
| UserRepository | user/domain/repository/UserRepository.java | ✅ 已完成 |
| JdbcUserRepository | user/infrastructure/persistence/repository/JdbcUserRepository.java | ✅ 已完成 |
| EmailVerificationService | user/application/service/EmailVerificationService.java | ⚠️ 待确认 |
| TokenBlacklistService | user/application/service/TokenBlacklistService.java | ✅ 已完成 |

### 1.2 待实现 API

| API | 方法 | 路径 | 优先级 |
|-----|------|------|--------|
| 检查用户名可用性 | GET | /api/v1/auth/check-username | P1 |
| 检查邮箱可用性 | GET | /api/v1/auth/check-email | P1 |
| 申请密码重置 | POST | /api/v1/auth/forgot-password | P1 |
| 验证重置令牌 | GET | /api/v1/auth/verify-reset-token | P1 |
| 重置密码 | POST | /api/v1/auth/reset-password | P1 |
| 获取用户列表（admin） | GET | /api/v1/admin/users | P2 |
| 获取用户详情（admin） | GET | /api/v1/admin/users/{userId} | P2 |
| 更新用户信息（admin） | PUT | /api/v1/admin/users/{userId} | P2 |
| 解锁账户（admin） | POST | /api/v1/admin/users/{userId}/unlock | P2 |
| 终止会话（admin） | DELETE | /api/v1/admin/users/{userId}/sessions/{sessionId} | P2 |
| 终止所有会话（admin） | DELETE | /api/v1/admin/users/{userId}/sessions | P2 |
| 强制改密（admin） | POST | /api/v1/admin/users/{userId}/force-password-reset | P2 |
| 获取登录日志（admin） | GET | /api/v1/admin/login-logs | P2 |

---

## 二、P1 - 唯一性检查与忘记密码

### 2.1 AuthAppService 新增方法

```java
// user/application/service/AuthAppService.java
public interface AuthAppService {
    // ... 已有方法 ...

    // ========== 新增方法 ==========

    /**
     * 检查用户名是否可用
     */
    boolean isUsernameAvailable(String username);

    /**
     * 检查邮箱是否可用
     */
    boolean isEmailAvailable(String email);

    /**
     * 申请密码重置
     */
    void requestPasswordReset(String email);

    /**
     * 验证密码重置令牌
     */
    PasswordResetTokenResult verifyResetToken(String token);

    /**
     * 重置密码
     */
    void resetPassword(String token, String newPassword);

    // 内部类
    class PasswordResetTokenResult {
        private final String userId;
        private final String email;

        public PasswordResetTokenResult(String userId, String email) {
            this.userId = userId;
            this.email = email;
        }

        public String getUserId() { return userId; }
        public String getEmail() { return email; }
    }
}
```

### 2.2 密码重置数据表（新增）

```sql
-- V5 tb_password_reset 表
CREATE TABLE tb_password_reset (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
    reset_token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tb_password_reset_token ON tb_password_reset(reset_token);
CREATE INDEX idx_tb_password_reset_user_id ON tb_password_reset(user_id);
```

### 2.3 PasswordResetService 实现

```java
// user/application/service/PasswordResetService.java
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final JdbcTemplate jdbcTemplate;

    // Token 有效期：1小时
    private static final long TOKEN_EXPIRY_MINUTES = 60;

    public void createResetToken(String userId, String email) {
        // 生成随机 token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 存储 token
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);
        jdbcTemplate.update(
            "INSERT INTO tb_password_reset (user_id, reset_token, expires_at) VALUES (?, ?, ?)",
            userId, token, expiresAt
        );

        // 发送邮件（通过 EmailService）
        // emailService.sendPasswordResetEmail(email, token);
    }

    public PasswordResetToken validateToken(String token) {
        List<PasswordResetToken> results = jdbcTemplate.query(
            "SELECT pr.*, u.email FROM tb_password_reset pr " +
            "JOIN tb_user u ON pr.user_id = u.id " +
            "WHERE pr.reset_token = ? AND pr.used = false AND pr.expires_at > NOW()",
            (rs, rowNum) -> new PasswordResetToken(
                rs.getString("user_id"),
                rs.getString("email"),
                rs.getTimestamp("expires_at").toLocalDateTime()
            ),
            token
        );

        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = validateToken(token);
        if (resetToken == null) {
            throw new ApplicationException("重置链接已失效，请重新申请");
        }

        // 更新密码
        String passwordHash = passwordEncoder.encode(newPassword);
        jdbcTemplate.update(
            "UPDATE tb_user SET password_hash = ?, updated_at = NOW() WHERE id = ?",
            passwordHash, resetToken.getUserId()
        );

        // 标记 token 已使用
        jdbcTemplate.update(
            "UPDATE tb_password_reset SET used = true WHERE reset_token = ?",
            token
        );
    }

    @Data
    public static class PasswordResetToken {
        private final String userId;
        private final String email;
        private final LocalDateTime expiresAt;
    }
}
```

### 2.4 AuthController 新增端点

```java
// 在 AuthController.java 中添加以下方法

/**
 * 检查用户名可用性
 */
@GetMapping("/check-username")
public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkUsername(
        @RequestParam("username") String username) {
    boolean available = authAppService.isUsernameAvailable(username);
    return ResponseEntity.ok(ApiResponse.success(Map.of("available", available)));
}

/**
 * 检查邮箱可用性
 */
@GetMapping("/check-email")
public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(
        @RequestParam("email") String email) {
    boolean available = authAppService.isEmailAvailable(email);
    return ResponseEntity.ok(ApiResponse.success(Map.of("available", available)));
}

/**
 * 申请密码重置
 */
@PostMapping("/forgot-password")
public ResponseEntity<ApiResponse<Void>> forgotPassword(
        @RequestBody Map<String, String> request) {
    String email = request.get("email");
    authAppService.requestPasswordReset(email);
    // 无论邮箱是否存在，都返回成功，防止用户探测
    return ResponseEntity.ok(ApiResponse.success("重置链接已发送至您的邮箱", null));
}

/**
 * 验证密码重置令牌
 */
@GetMapping("/verify-reset-token")
public ResponseEntity<ApiResponse<PasswordResetTokenResponse>> verifyResetToken(
        @RequestParam("token") String token) {
    var result = authAppService.verifyResetToken(token);
    return ResponseEntity.ok(ApiResponse.success(new PasswordResetTokenResponse(
        result.getUserId(), result.getEmail()
    )));
}

/**
 * 重置密码
 */
@PostMapping("/reset-password")
public ResponseEntity<ApiResponse<Void>> resetPassword(
        @RequestBody ResetPasswordRequest request) {
    authAppService.resetPassword(request.getToken(), request.getNewPassword());
    return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
}

// 内部类
public static class PasswordResetTokenResponse {
    private final String userId;
    private final String email;
    // getters...
}

public static class ResetPasswordRequest {
    private final String token;
    private final String newPassword;
    private final String confirmPassword;
    // getters...
}
```

---

## 三、P2 - 用户管理后台

### 3.1 AdminController（新增）

```java
// interfaces/rest/controller/AdminController.java
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserListResponse>> getUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "emailVerified", required = false) Boolean emailVerified,
            @RequestParam(value = "locked", required = false) Boolean locked) {

        var result = adminService.getUsers(page, size, role, keyword, emailVerified, locked);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetail(
            @PathVariable("userId") String userId) {
        var result = adminService.getUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @PathVariable("userId") String userId,
            @RequestBody UpdateUserRequest request) {
        adminService.updateUser(userId, request.getDisplayName(), request.getRole());
        return ResponseEntity.ok(ApiResponse.success("用户信息更新成功", null));
    }

    /**
     * 解锁账户
     */
    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable("userId") String userId) {
        adminService.unlockUser(userId);
        return ResponseEntity.ok(ApiResponse.success("账户已解锁", null));
    }

    /**
     * 终止指定会话
     */
    @DeleteMapping("/users/{userId}/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> terminateSession(
            @PathVariable("userId") String userId,
            @PathVariable("sessionId") String sessionId) {
        adminService.terminateSession(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success("会话已终止", null));
    }

    /**
     * 终止所有其他会话
     */
    @DeleteMapping("/users/{userId}/sessions")
    public ResponseEntity<ApiResponse<Void>> terminateAllSessions(
            @PathVariable("userId") String userId) {
        int count = adminService.terminateAllSessions(userId);
        return ResponseEntity.ok(ApiResponse.success("已终止 " + count + " 个其他会话", null));
    }

    /**
     * 强制用户修改密码
     */
    @PostMapping("/users/{userId}/force-password-reset")
    public ResponseEntity<ApiResponse<Void>> forcePasswordReset(
            @PathVariable("userId") String userId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        adminService.forcePasswordReset(userId, reason);
        return ResponseEntity.ok(ApiResponse.success("已强制用户下次登录修改密码", null));
    }
}
```

### 3.2 AdminService 接口与实现

```java
// user/application/service/AdminService.java
public interface AdminService {
    PageResult<UserDTO> getUsers(int page, int size, String role, String keyword,
                                  Boolean emailVerified, Boolean locked);

    UserDetailDTO getUserDetail(String userId);

    void updateUser(String userId, String displayName, String role);

    void unlockUser(String userId);

    void terminateSession(String userId, String sessionId);

    int terminateAllSessions(String userId);

    void forcePasswordReset(String userId, String reason);
}

// 实现类：user/application/service/impl/AdminServiceImpl.java
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final JdbcTemplate jdbcTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public PageResult<UserDTO> getUsers(int page, int size, String role, String keyword,
                                          Boolean emailVerified, Boolean locked) {
        // 动态 SQL 构建
        StringBuilder sql = new StringBuilder("SELECT * FROM tb_user WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (role != null && !role.isEmpty()) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (username LIKE ? OR email LIKE ? OR display_name LIKE ?)");
            String kw = "%" + keyword + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (emailVerified != null) {
            sql.append(" AND email_verified = ?");
            params.add(emailVerified);
        }
        if (locked != null) {
            if (locked) {
                sql.append(" AND locked_until > NOW()");
            } else {
                sql.append(" AND (locked_until IS NULL OR locked_until <= NOW())");
            }
        }

        // Count 查询
        String countSql = sql.toString().replace("SELECT *", "SELECT COUNT(*)");
        int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

        // 分页
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);

        List<UserDTO> users = jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> toUserDTO(rs),
            params.toArray()
        );

        return new PageResult<>(users, total, page, size);
    }

    @Override
    public UserDetailDTO getUserDetail(String userId) {
        // 查询用户信息
        UserDTO user = jdbcTemplate.queryForObject(
            "SELECT * FROM tb_user WHERE id = ?",
            (rs, rowNum) -> toUserDTO(rs),
            userId
        );

        // 查询会话信息
        List<SessionDTO> sessions = jdbcTemplate.query(
            "SELECT * FROM tb_user_session WHERE user_id = ? AND expires_at > NOW()",
            (rs, rowNum) -> toSessionDTO(rs),
            userId
        );

        return new UserDetailDTO(user, sessions);
    }

    @Override
    @Transactional
    public void updateUser(String userId, String displayName, String role) {
        List<String> updates = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (displayName != null) {
            updates.add("display_name = ?");
            params.add(displayName);
        }
        if (role != null) {
            updates.add("role = ?");
            params.add(role);
        }

        if (!updates.isEmpty()) {
            updates.add("updated_at = NOW()");
            params.add(userId);

            String sql = "UPDATE tb_user SET " + String.join(", ", updates) + " WHERE id = ?";
            jdbcTemplate.update(sql, params.toArray());
        }
    }

    @Override
    @Transactional
    public void unlockUser(String userId) {
        jdbcTemplate.update(
            "UPDATE tb_user SET locked_until = NULL, failed_login_attempts = 0 WHERE id = ?",
            userId
        );
    }

    @Override
    @Transactional
    public void terminateSession(String userId, String sessionId) {
        jdbcTemplate.update(
            "DELETE FROM tb_user_session WHERE id = ? AND user_id = ?",
            sessionId, userId
        );
    }

    @Override
    @Transactional
    public int terminateAllSessions(String userId) {
        // 排除当前会话（通过 token 判断）
        String currentToken = // 从 SecurityContext 获取
        return jdbcTemplate.update(
            "DELETE FROM tb_user_session WHERE user_id = ? AND token_hash != ?",
            userId, hashToken(currentToken)
        );
    }

    @Override
    @Transactional
    public void forcePasswordReset(String userId, String reason) {
        // 设置密码过期标志
        jdbcTemplate.update(
            "UPDATE tb_user SET password_expires_at = NOW() WHERE id = ?",
            userId
        );
        log.info("管理员强制用户 {} 修改密码，原因: {}", userId, reason);
    }

    private UserDTO toUserDTO(ResultSet rs) throws SQLException {
        return UserDTO.builder()
            .id(rs.getString("id"))
            .username(rs.getString("username"))
            .email(rs.getString("email"))
            .displayName(rs.getString("display_name"))
            .role(rs.getString("role"))
            .avatarInitial(rs.getString("avatar_initial"))
            .emailVerified(rs.getBoolean("email_verified"))
            .isLocked(rs.getTimestamp("locked_until") != null &&
                      rs.getTimestamp("locked_until").toLocalDateTime().isAfter(LocalDateTime.now()))
            .lockedUntil(rs.getTimestamp("locked_until"))
            .failedLoginAttempts(rs.getInt("failed_login_attempts"))
            .lastLoginAt(rs.getTimestamp("last_login_at"))
            .createdAt(rs.getTimestamp("created_at"))
            .build();
    }
}
```

---

## 四、P2 - 登录日志

### 4.1 LoginLogService（新增）

```java
// user/application/service/LoginLogService.java
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 记录登录日志
     */
    @Transactional
    public void record(String userId, String username, String action,
                       String ip, String userAgent, String details) {
        jdbcTemplate.update(
            "INSERT INTO tb_login_log (user_id, username, action, ip, user_agent, details) " +
            "VALUES (?, ?, ?, ?, ?, ?)",
            userId, username, action, ip, userAgent, details
        );
    }

    /**
     * 查询登录日志（分页）
     */
    public PageResult<LoginLogDTO> getLogs(int page, int size, String userId,
                                            String action, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT * FROM tb_login_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (userId != null && !userId.isEmpty()) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (startDate != null && !startDate.isEmpty()) {
            sql.append(" AND created_at >= ?");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND created_at <= ?");
            params.add(endDate + " 23:59:59");
        }

        // Count
        String countSql = sql.toString().replace("SELECT *", "SELECT COUNT(*)");
        int total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

        // 分页
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);

        List<LoginLogDTO> logs = jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> toLoginLogDTO(rs),
            params.toArray()
        );

        return new PageResult<>(logs, total, page, size);
    }

    /**
     * 导出 CSV
     */
    public void exportCSV(HttpServletResponse response, String userId,
                          String action, String startDate, String endDate) throws IOException {
        // 设置响应头
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
            "attachment; filename=login-logs-" + LocalDate.now() + ".csv");

        PrintWriter writer = response.getWriter();
        writer.println("时间,用户ID,用户名,操作,IP,设备,详情");

        // 游标查询（避免一次性加载大结果集）
        int offset = 0;
        int batchSize = 1000;

        while (true) {
            List<LoginLogDTO> batch = getLogs(1 + offset / batchSize, batchSize,
                userId, action, startDate, endDate).getList();

            if (batch.isEmpty()) break;

            for (LoginLogDTO log : batch) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                    log.getTimestamp(), log.getUserId(), log.getUsername(),
                    log.getAction(), log.getIp(), log.getUserAgent(), log.getDetails()
                );
            }

            if (batch.size() < batchSize) break;
            offset += batchSize;
        }

        writer.flush();
    }

    private LoginLogDTO toLoginLogDTO(ResultSet rs) throws SQLException {
        return LoginLogDTO.builder()
            .id(rs.getLong("id"))
            .userId(rs.getString("user_id"))
            .username(rs.getString("username"))
            .action(rs.getString("action"))
            .ip(rs.getString("ip"))
            .userAgent(rs.getString("user_agent"))
            .details(rs.getString("details"))
            .timestamp(rs.getTimestamp("created_at").toLocalDateTime())
            .build();
    }
}
```

### 4.2 LoginLogController（新增）

```java
// interfaces/rest/controller/LoginLogController.java
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResult<LoginLogDTO>>> getLogs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        var result = loginLogService.getLogs(page, size, userId, action, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/export")
    public void exportLogs(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            HttpServletResponse response) throws IOException {
        loginLogService.exportCSV(response, userId, action, startDate, endDate);
    }
}
```

---

## 五、安全考虑

### 5.1 Admin 权限检查

在所有 AdminController 方法上添加权限检查：

```java
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    // ...
}
```

### 5.2 防用户探测

`POST /api/v1/auth/forgot-password` 无论邮箱是否存在都返回成功，防止攻击者通过响应判断邮箱是否已注册。

### 5.3 密码重置 Token 安全

- Token 使用 UUID，确保不可预测
- Token 设置 1 小时有效期
- Token 使用后立即标记为已使用
- Token 一次性使用

### 5.4 登录日志记录

在 `AuthAppServiceImpl` 中集成 `LoginLogService`：

```java
// 登录成功时
loginLogService.record(user.getId().getValue(), user.getUsername(),
    "login_success", ip, userAgent, "登录成功");

// 登录失败时
loginLogService.record(null, attemptedUsername, "login_failed",
    ip, userAgent, String.format("密码错误（第%d次）", attempts));
```

---

## 六、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | 2026-05-12 | 初始版本，补充后端 API 实现设计 |
