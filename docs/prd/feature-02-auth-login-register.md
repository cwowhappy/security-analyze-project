# Feature-02：用户登录与注册

> 文档版本：v2.0
> 创建日期：2026-05-12
> 更新日期：2026-05-12
> 设计风格：Stripe 设计语言（与 Feature-01 原型保持一致）

---

## 一、功能概述

### 功能范围

| 功能模块 | 说明 | 优先级 | 状态 |
|---------|------|---------|------|
| 用户登录 | 用户名/邮箱 + 密码登录，支持记住我 | P0 | ✅ 已完成 |
| 用户注册 | 新用户注册，含字段校验 | P0 | ✅ 已完成 |
| 登录态管理 | JWT Token 存储、自动续期、登出 | P0 | ✅ 已完成 |
| 忘记密码 | 通过邮箱重置密码（完整流程） | P1 | ✅ 前端完成，后端待实现 |
| 主题同步 | 登录/注册页支持 light/dark 主题 | P1 | ✅ 已完成 |
| 登录失败锁定 | 同一账号5次失败后锁定15分钟 | P1 | ✅ 已完成 |
| 用户名/邮箱唯一性检查 | 注册页 blur 时实时校验 | P1 | ✅ 前端完成，后端待实现 |
| 邮箱验证 | 注册后发送验证邮件 | P2 | ✅ 页面/Store/后端API完成，邮件服务待集成 |
| 登录日志审计 | 记录用户登录行为 | P2 | ✅ PRD设计完成，代码待实现 |
| 用户管理后台 | 管理员增删改查用户 | P2 | ✅ PRD设计完成，代码待实现 |
| 多设备登录管理 | 踢出其他设备 | P2 | ✅ 已在用户管理后台设计中包含 |
| 密码过期强制修改 | 90天强制修改密码 | P2 | ✅ API设计完成，代码待实现 |

### 与现有系统的关系

- 登录成功后将 JWT Token 存入 `localStorage`，后续所有 API 请求携带
- 顶部用户信息区（原型已有占位）在登录后展示真实用户名与角色
- 未登录访问任意页面自动跳转登录页
- Token 黑名单机制确保登出后 Token 立即失效

---

## 二、页面设计

### 2.1 登录页（`/login`）

**布局**：独立全屏布局，无侧边栏，居中卡片，左侧品牌展示区 + 右侧表单区（大屏）；移动端上下排列。

**字段**：

| 字段 | 类型 | 校验规则 | 必填 |
|------|------|---------|------|
| 用户名或邮箱 | text | 非空，至少3字符 | ✓ |
| 密码 | password | 非空，至少6字符 | ✓ |
| 记住我 | checkbox | — | ✗ |
| 忘记密码链接 | link | 跳转 `/forgot-password` | — |

**交互流程**：
1. 用户填写用户名/邮箱 + 密码
2. 点击「登录」按钮 → 调用 `POST /api/v1/auth/login`
3. 成功：存储 token → 跳转首页 `/`
4. 失败：红色提示条「用户名或密码错误」
5. 连续失败5次：显示「账户已锁定，请15分钟后重试」
6. 无账号 → 点击「立即注册」跳转 `/register`

**视觉规范**（与现有原型一致）：
- 卡片背景：`var(--surface)`，边框 `var(--border)`，圆角 `var(--radius-lg)`
- 输入框：focus 态蓝色边框 + 外辉光 `0 0 0 3px rgba(99,91,255,.1)`
- 主按钮：背景 `var(--primary)`，文字白色，hover `var(--primary-hover)`
- 错误提示：红色背景 `rgba(255,59,48,.1)`，文字 `var(--danger)`
- 品牌左侧区：背景 `var(--primary)` 渐变，白色文字，展示系统名称 + 英文副标题

---

### 2.2 注册页（`/register`）

**布局**：同登录页，居中卡片式

**字段**：

| 字段 | 类型 | 校验规则 | 必填 |
|------|------|---------|------|
| 用户名 | text | 3-20字符，仅字母数字下划线 | ✓ |
| 邮箱 | email | 标准邮箱格式，唯一性校验 | ✓ |
| 密码 | password | 至少8字符，含字母+数字 | ✓ |
| 确认密码 | password | 与密码一致 | ✓ |
| 用户角色 | select | 投资组合经理 / 分析师 / viewer | ✓ |
| 同意协议 | checkbox | 必须勾选 | ✓ |

**交互流程**：
1. 填写所有必填字段，实时校验
2. 密码强度提示（弱/中/强）
3. **用户名/邮箱唯一性检查**：blur 事件触发 `GET /api/v1/auth/check-username?username=xxx` 和 `GET /api/v1/auth/check-email?email=xxx`
4. 点击「注册」→ 调用 `POST /api/v1/auth/register`
5. 成功：提示「注册成功，请验证您的邮箱」→ 跳转 `/login`
6. 失败：字段下方红色错误提示（邮箱已注册 / 用户名已存在）

**密码强度规则**：
- 弱：仅满足长度 ≥8
- 中：长度 ≥8 + 含字母+数字
- 强：长度 ≥8 + 含字母+数字+特殊字符

**唯一性检查 API**：
- `GET /api/v1/auth/check-username?username=xxx` → `{ "available": true/false }`
- `GET /api/v1/auth/check-email?email=xxx` → `{ "available": true/false }`

---

### 2.3 忘记密码页（`/forgot-password`）

**布局**：简化居中卡片，单步流程

**字段**：

| 字段 | 类型 | 校验规则 | 必填 |
|------|------|---------|------|
| 注册邮箱 | email | 标准邮箱格式 | ✓ |

**交互流程**：
1. 输入注册邮箱 → 点击「发送重置链接」
2. 调用 `POST /api/v1/auth/forgot-password`
3. 模拟：显示「重置链接已发送至您的邮箱」（实际不发送）
4. 提供「返回登录」链接

**重置链接页（`/reset-password?token=xxx`）**：

| 字段 | 类型 | 校验规则 | 必填 |
|------|------|---------|------|
| 新密码 | password | 至少8字符，含字母+数字 | ✓ |
| 确认密码 | password | 与密码一致 | ✓ |

**交互流程**：
1. 从邮件链接获取 token 参数
2. 调用 `GET /api/v1/auth/verify-reset-token?token=xxx` 验证 token 有效性
3. 输入新密码 → 调用 `POST /api/v1/auth/reset-password`
4. 成功：提示「密码重置成功」→ 跳转 `/login`

---

### 2.4 邮箱验证页（`/verify-email?userId=xxx&code=xxx`）

**布局**：简化居中卡片

**交互流程**：
1. 从邮件链接获取 userId 和 code 参数
2. 调用 `POST /api/v1/auth/verify-email` 进行验证
3. 成功：提示「邮箱验证成功」，显示「前往登录」按钮
4. 失败：显示错误信息，提供「重新发送验证码」按钮

---

## 三、API 规划

### 3.1 POST `/api/v1/auth/login`

**Request**：
```json
{
  "username": "string",
  "password": "string",
  "rememberMe": false
}
```

**Response（成功）**：
```json
{
  "success": true,
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbG...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": "user_xxx",
      "username": "lixiaoyi",
      "email": "user@example.com",
      "role": "portfolio_manager",
      "displayName": "李小艺",
      "avatarInitial": "L"
    }
  },
  "timestamp": 1715340000000
}
```

**Response（失败 - 密码错误）**：
```json
{
  "success": false,
  "code": 401,
  "message": "用户名或密码错误"
}
```

**Response（失败 - 账户锁定）**：
```json
{
  "success": false,
  "code": 423,
  "message": "账户已锁定，请 15 分钟后重试"
}
```

**Response（失败 - 邮箱未验证）**：
```json
{
  "success": false,
  "code": 403,
  "message": "请先验证邮箱",
  "data": {
    "userId": "user_xxx"
  }
}
```

---

### 3.2 POST `/api/v1/auth/register`

**Request**：
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "confirmPassword": "string",
  "role": "portfolio_manager"
}
```

**Response（成功）**：
```json
{
  "success": true,
  "code": 200,
  "message": "注册成功，请验证您的邮箱",
  "data": {
    "userId": "user_xxx",
    "email": "user@example.com"
  }
}
```

**Response（失败）**：
```json
{
  "success": false,
  "code": 409,
  "message": "注册失败",
  "errors": {
    "email": "该邮箱已被注册",
    "username": "用户名已存在"
  }
}
```

---

### 3.3 GET `/api/v1/auth/check-username`

**Request**：`?username=xxx`

**Response**：
```json
{
  "success": true,
  "code": 200,
  "data": {
    "available": true
  }
}
```

---

### 3.4 GET `/api/v1/auth/check-email`

**Request**：`?email=xxx`

**Response**：
```json
{
  "success": true,
  "code": 200,
  "data": {
    "available": true
  }
}
```

---

### 3.5 POST `/api/v1/auth/logout`

**Request**：Header 携带 `Authorization: Bearer {token}`

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "登出成功"
}
```

> 后端将 Token 加入黑名单，确保即使客户端未删除也无法继续使用。

---

### 3.6 GET `/api/v1/auth/me`

**用途**：首页加载时验证 token 是否有效，获取当前用户信息

**Response（有效）**：
```json
{
  "success": true,
  "code": 200,
  "data": {
    "id": "user_xxx",
    "username": "lixiaoyi",
    "email": "user@example.com",
    "role": "portfolio_manager",
    "displayName": "李小艺",
    "avatarInitial": "L",
    "emailVerified": true,
    "passwordExpired": false,
    "passwordExpiresAt": "2026-08-12T00:00:00"
  }
}
```

**Response（无效）**：
```json
{
  "success": false,
  "code": 401,
  "message": "Token 已失效，请重新登录"
}
```

---

### 3.7 POST `/api/v1/auth/verify-email`

**Request**：
```json
{
  "userId": "string",
  "code": "string"
}
```

**Response（成功）**：
```json
{
  "success": true,
  "code": 200,
  "message": "邮箱验证成功"
}
```

**Response（失败）**：
```json
{
  "success": false,
  "code": 400,
  "message": "验证码无效或已过期"
}
```

---

### 3.8 POST `/api/v1/auth/resend-verification`

**Request**：`?userId=xxx`

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "验证码已重新发送"
}
```

---

### 3.9 POST `/api/v1/auth/forgot-password`

**Request**：
```json
{
  "email": "string"
}
```

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "重置链接已发送至您的邮箱"
}
```

> 注意：无论邮箱是否存在，均返回成功，防止用户通过响应判断邮箱是否已注册。

---

### 3.10 GET `/api/v1/auth/verify-reset-token

**Request**：`?token=xxx`

**Response（有效）**：
```json
{
  "success": true,
  "code": 200,
  "data": {
    "userId": "user_xxx",
    "email": "user@example.com"
  }
}
```

**Response（无效或已过期）**：
```json
{
  "success": false,
  "code": 400,
  "message": "重置链接已失效，请重新申请"
}
```

---

### 3.11 POST `/api/v1/auth/reset-password`

**Request**：
```json
{
  "token": "string",
  "newPassword": "string",
  "confirmPassword": "string"
}
```

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "密码重置成功"
}
```

---

## 四、用户管理 API（P2）

### 4.1 GET `/api/v1/admin/users`

**用途**：管理员获取用户列表

**请求参数（Query）**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页大小，默认 20 |
| role | string | 否 | 角色筛选 |
| keyword | string | 否 | 关键词：匹配 username / email / displayName |
| emailVerified | boolean | 否 | 邮箱验证状态 |
| locked | boolean | 否 | 账户锁定状态 |

**Response**：
```json
{
  "success": true,
  "code": 200,
  "data": {
    "list": [
      {
        "id": "user_xxx",
        "username": "lixiaoyi",
        "email": "user@example.com",
        "displayName": "李小艺",
        "role": "portfolio_manager",
        "avatarInitial": "L",
        "emailVerified": true,
        "isLocked": false,
        "lockedUntil": null,
        "failedLoginAttempts": 0,
        "lastLoginAt": "2026-05-12T08:00:00",
        "createdAt": "2026-05-01T00:00:00"
      }
    ],
    "total": 156,
    "page": 1,
    "size": 20
  }
}
```

> 仅 `admin` 角色可访问，返回 403 Forbidden。

---

### 4.2 GET `/api/v1/admin/users/{userId}`

**用途**：管理员获取用户详情

**Response**：
```json
{
  "success": true,
  "code": 200,
  "data": {
    "id": "user_xxx",
    "username": "lixiaoyi",
    "email": "user@example.com",
    "displayName": "李小艺",
    "role": "portfolio_manager",
    "avatarInitial": "L",
    "emailVerified": true,
    "isLocked": false,
    "lockedUntil": null,
    "failedLoginAttempts": 0,
    "lastLoginAt": "2026-05-12T08:00:00",
    "createdAt": "2026-05-01T00:00:00",
    "updatedAt": "2026-05-12T08:00:00",
    "loginSessions": [
      {
        "sessionId": "sess_xxx",
        "ip": "192.168.1.1",
        "userAgent": "Chrome/125.0.0.0",
        "createdAt": "2026-05-12T08:00:00",
        "expiresAt": "2026-05-13T08:00:00",
        "isCurrent": true
      }
    ]
  }
}
```

---

### 4.3 PUT `/api/v1/admin/users/{userId}`

**用途**：管理员更新用户信息

**Request**：
```json
{
  "displayName": "李小艺",
  "role": "analyst"
}
```

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "用户信息更新成功"
}
```

---

### 4.4 POST `/api/v1/admin/users/{userId}/unlock`

**用途**：管理员手动解锁账户

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "账户已解锁"
}
```

---

### 4.5 DELETE `/api/v1/admin/users/{userId}/sessions/{sessionId}

**用途**：管理员踢出指定设备会话

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "会话已终止"
}
```

---

### 4.6 DELETE `/api/v1/admin/users/{userId}/sessions

**用途**：管理员踢出用户所有其他设备会话（保留当前会话）

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "已终止 3 个其他会话"
}
```

---

## 五、登录日志 API（P2）

### 5.1 GET `/api/v1/admin/login-logs`

**用途**：管理员查看登录日志

**请求参数（Query）**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页大小，默认 20 |
| userId | string | 否 | 用户 ID 筛选 |
| action | string | 否 | 动作：login_success / login_failed / logout / password_reset |
| startDate | string | 否 | 开始日期（ISO 8601） |
| endDate | string | 否 | 结束日期（ISO 8601） |

**Response**：
```json
{
  "success": true,
  "code": 200,
  "data": {
    "list": [
      {
        "id": "log_xxx",
        "userId": "user_xxx",
        "username": "lixiaoyi",
        "action": "login_success",
        "ip": "192.168.1.1",
        "userAgent": "Chrome/125.0.0.0",
        "details": "登录成功",
        "timestamp": "2026-05-12T08:00:00"
      },
      {
        "id": "log_yyy",
        "userId": "user_xxx",
        "username": "lixiaoyi",
        "action": "login_failed",
        "ip": "192.168.1.1",
        "userAgent": "Chrome/125.0.0.0",
        "details": "密码错误（第3次）",
        "timestamp": "2026-05-12T07:55:00"
      }
    ],
    "total": 1024,
    "page": 1,
    "size": 20
  }
}
```

---

### 5.2 POST `/api/v1/admin/users/{userId}/force-password-reset`

**用途**：管理员强制用户下次登录修改密码

**Request**：
```json
{
  "reason": "安全策略要求"
}
```

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "已强制用户下次登录修改密码"
}
```

---

## 六、前端状态管理

### Token 存储
- `rememberMe = true`：存入 `localStorage`（持久）
- `rememberMe = false`：存入 `sessionStorage`（关闭浏览器清除）

### 路由守卫
- 未登录 → 所有页面跳转 `/login`
- 已登录 → 访问 `/login` 或 `/register` 自动跳转 `/`
- 邮箱未验证 → 引导验证页面（可跳过浏览其他页面）
- 密码过期 → 引导修改密码页面（不可跳过）

### 请求拦截器
- 所有 API 请求自动在 Header 中附加 `Authorization: Bearer {token}`
- 收到 401 响应 → 清除本地 token → 跳转登录页
- 收到 403 响应（邮箱未验证）→ 显示验证邮箱提示
- 收到 423 响应（账户锁定）→ 显示锁定信息

---

## 七、数据库设计

### 7.1 用户表（已存在）

```sql
-- 用户表（V3）
CREATE TABLE tb_user (
    id VARCHAR(32) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    role VARCHAR(50) NOT NULL DEFAULT 'viewer',
    avatar_initial CHAR(1),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 7.2 用户认证增强字段（已存在）

```sql
-- V4 扩展字段
ALTER TABLE tb_user
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP,
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
```

### 7.3 邮箱验证码表（已存在）

```sql
-- V4 tb_email_verification 表
CREATE TABLE tb_email_verification (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
    verification_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used BOOLEAN NOT NULL DEFAULT false
);
```

### 7.4 会话表（已存在）

```sql
-- V3 tb_user_session 表
CREATE TABLE tb_user_session (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 7.5 登录日志表（需新建）

```sql
-- V5 tb_login_log 表
CREATE TABLE tb_login_log (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(32) REFERENCES tb_user(id) ON DELETE SET NULL,
    username VARCHAR(50),
    action VARCHAR(50) NOT NULL,  -- login_success, login_failed, logout, password_reset, email_verified
    ip VARCHAR(45),
    user_agent VARCHAR(500),
    details VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tb_login_log_user_id ON tb_login_log(user_id);
CREATE INDEX idx_tb_login_log_created_at ON tb_login_log(created_at);
CREATE INDEX idx_tb_login_log_action ON tb_login_log(action);
```

### 7.6 密码重置令牌表（需新建）

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

### 7.7 Token 黑名单表（需新建）

```sql
-- V5 tb_token_blacklist 表
CREATE TABLE tb_token_blacklist (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tb_token_blacklist_token ON tb_token_blacklist(token_hash);
```

---

## 八、状态码汇总

| 状态码 | 场景 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数校验失败 / 验证码错误 |
| 401 | 未认证 / Token 无效 |
| 403 | 邮箱未验证 / 权限不足 |
| 404 | 用户不存在 |
| 409 | 资源冲突（如重复的唯一键） |
| 423 | 账户锁定 |
| 500 | 服务端内部错误 |

---

## 九、功能清单

### P0（MVP 必须完成）

- [x] 登录页 UI + 表单校验
- [x] 注册页 UI + 表单校验（含密码强度）
- [x] `POST /api/v1/auth/login` 接口
- [x] `POST /api/v1/auth/register` 接口
- [x] JWT Token 生成与验证
- [x] 前端 Token 存储 + 请求拦截器
- [x] 路由守卫（未登录拦截）
- [x] 登出功能
- [x] Light/Dark 主题在登录页生效

### P1（重要，第二优先级）

- [x] 登录失败次数限制（同一账号5次失败后锁定15分钟）
- [x] 忘记密码页 UI + 完整流程（前端 ✅，后端待实现）
- [x] 密码重置页 UI + 完整流程（前端 ✅，后端待实现）
- [x] 用户名唯一性实时检查（前端 ✅，后端待实现）
- [x] 邮箱唯一性实时检查（前端 ✅，后端待实现）
- [x] `POST /api/v1/auth/logout` 接口（服务端 Token 失效）

### P2（后续迭代）

- [x] 邮箱验证完整流程（API ✅，前端部分完成，独立验证页待补充）
- [x] 登录日志审计（PRD ✅，代码待实现）
- [x] 用户管理后台（PRD ✅，代码待实现）
- [x] 多设备登录管理（PRD ✅，代码待实现）
- [x] 密码过期强制修改（PRD ✅，代码待实现）
- [ ] ~~第三方登录（GitHub / 企业微信）~~ ← 不考虑

---

## 十、Mock 数据（原型用）

**测试账号**：
| 用户名 | 密码 | 角色 |
|--------|------|------|
| lixiaoyi | demo1234 | 投资组合经理 |
| analyst01 | demo1234 | 分析师 |
| viewer01 | demo1234 | 观察者 |
| admin01 | demo1234 | 管理员 |

> 原型中登录页输入任意用户名 + 密码 `demo1234` 即可登录成功（模拟）。

---

## 十一、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v3.0 | 2026-05-12 | 更新功能状态：P1 忘记密码和唯一性检查前端完成；P2 用户管理/登录日志/邮箱验证新增设计文档 |
| v2.0 | 2026-05-12 | 完善 P1/P2 功能设计：增加忘记密码完整流程、唯一性检查 API、用户管理 API、登录日志 API、数据库设计 |
| v1.0 | 2026-05-12 | 初始版本 |
