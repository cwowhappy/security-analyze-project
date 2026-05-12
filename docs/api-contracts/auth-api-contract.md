# 认证模块 API 契约 v2.0

> 版本：v2.0 | 日期：2026-05-12
> 本文档定义认证与用户管理 REST API 的请求/响应格式、状态码与字段说明。
> 变更：新增 P1/P2 功能 API，包括唯一性检查、忘记密码、用户管理、登录日志。

---

## 一、统一响应格式

所有接口返回统一包装：

```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1715340000000
}
```

**错误响应示例（业务错误）**：

```json
{
  "success": false,
  "code": 400,
  "message": "验证码无效或已过期",
  "timestamp": 1715340000000
}
```

**错误响应示例（字段校验错误）**：

```json
{
  "success": false,
  "code": 409,
  "message": "注册失败",
  "errors": {
    "email": "该邮箱已被注册",
    "username": "用户名已存在"
  },
  "timestamp": 1715340000000
}
```

---

## 二、认证 API

### 2.1 用户登录

```
POST /api/v1/auth/login
```

**Request**：

```json
{
  "username": "lixiaoyi",
  "password": "demo1234",
  "rememberMe": false
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名或邮箱 |
| password | string | 是 | 密码 |
| rememberMe | boolean | 否 | 是否记住我，默认 false |

**Response（成功）**：

```json
{
  "success": true,
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
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
  }
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

### 2.2 用户注册

```
POST /api/v1/auth/register
```

**Request**：

```json
{
  "username": "lixiaoyi",
  "email": "user@example.com",
  "password": "demo1234",
  "confirmPassword": "demo1234",
  "role": "portfolio_manager"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名，3-20字符，仅字母数字下划线 |
| email | string | 是 | 邮箱地址 |
| password | string | 是 | 密码，至少8位，含字母+数字 |
| confirmPassword | string | 是 | 确认密码，与 password 一致 |
| role | string | 是 | 角色：portfolio_manager / analyst / viewer |

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

**Response（失败 - 字段冲突）**：

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

### 2.3 检查用户名可用性

```
GET /api/v1/auth/check-username?username=xxx
```

**Request（Query）**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 待检查的用户名 |

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

### 2.4 检查邮箱可用性

```
GET /api/v1/auth/check-email?email=xxx
```

**Request（Query）**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 待检查的邮箱 |

**Response**：

```json
{
  "success": true,
  "code": 200,
  "data": {
    "available": false
  }
}
```

---

### 2.5 用户登出

```
POST /api/v1/auth/logout
```

**Request Header**：`Authorization: Bearer {token}`

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

### 2.6 获取当前用户信息

```
GET /api/v1/auth/me
```

**Request Header**：`Authorization: Bearer {token}`

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

### 2.7 验证邮箱

```
POST /api/v1/auth/verify-email
```

**Request**：

```json
{
  "userId": "user_xxx",
  "code": "123456"
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

### 2.8 重新发送验证邮件

```
POST /api/v1/auth/resend-verification?userId=xxx
```

**Request（Query）**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | string | 是 | 用户 ID |

**Response**：

```json
{
  "success": true,
  "code": 200,
  "message": "验证码已重新发送"
}
```

---

### 2.9 申请密码重置

```
POST /api/v1/auth/forgot-password
```

**Request**：

```json
{
  "email": "user@example.com"
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

### 2.10 验证密码重置链接

```
GET /api/v1/auth/verify-reset-token?token=xxx
```

**Request（Query）**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | string | 是 | 重置令牌 |

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

**Response（无效）**：

```json
{
  "success": false,
  "code": 400,
  "message": "重置链接已失效，请重新申请"
}
```

---

### 2.11 重置密码

```
POST /api/v1/auth/reset-password
```

**Request**：

```json
{
  "token": "xxx",
  "newPassword": "newPass123",
  "confirmPassword": "newPass123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| token | string | 是 | 重置令牌 |
| newPassword | string | 是 | 新密码，至少8位，含字母+数字 |
| confirmPassword | string | 是 | 确认密码 |

**Response**：

```json
{
  "success": true,
  "code": 200,
  "message": "密码重置成功"
}
```

---

## 三、用户管理 API（需 admin 角色）

### 3.1 获取用户列表

```
GET /api/v1/admin/users
```

**Request Header**：`Authorization: Bearer {token}`（需 admin 角色）

**Request（Query）**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页大小，默认 20，最大 100 |
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

---

### 3.2 获取用户详情

```
GET /api/v1/admin/users/{userId}
```

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

### 3.3 更新用户信息

```
PUT /api/v1/admin/users/{userId}
```

**Request**：

```json
{
  "displayName": "李小艺",
  "role": "analyst"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| displayName | string | 否 | 显示名称 |
| role | string | 否 | 角色：portfolio_manager / analyst / viewer / admin |

**Response**：

```json
{
  "success": true,
  "code": 200,
  "message": "用户信息更新成功"
}
```

---

### 3.4 解锁用户账户

```
POST /api/v1/admin/users/{userId}/unlock
```

**Response**：

```json
{
  "success": true,
  "code": 200,
  "message": "账户已解锁"
}
```

---

### 3.5 终止指定会话

```
DELETE /api/v1/admin/users/{userId}/sessions/{sessionId}
```

**Response**：

```json
{
  "success": true,
  "code": 200,
  "message": "会话已终止"
}
```

---

### 3.6 终止用户所有其他会话

```
DELETE /api/v1/admin/users/{userId}/sessions
```

**Response**：

```json
{
  "success": true,
  "code": 200,
  "message": "已终止 3 个其他会话"
}
```

---

### 3.7 强制用户修改密码

```
POST /api/v1/admin/users/{userId}/force-password-reset
```

**Request**：

```json
{
  "reason": "安全策略要求"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | string | 否 | 强制原因 |

**Response**：

```json
{
  "success": true,
  "code": 200,
  "message": "已强制用户下次登录修改密码"
}
```

---

## 四、登录日志 API（需 admin 角色）

### 4.1 获取登录日志列表

```
GET /api/v1/admin/login-logs
```

**Request Header**：`Authorization: Bearer {token}`（需 admin 角色）

**Request（Query）**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页大小，默认 20 |
| userId | string | 否 | 用户 ID 筛选 |
| action | string | 否 | 动作类型 |
| startDate | string | 否 | 开始日期（ISO 8601） |
| endDate | string | 否 | 结束日期（ISO 8601） |

**action 类型说明**：

| 值 | 说明 |
|----|------|
| login_success | 登录成功 |
| login_failed | 登录失败 |
| logout | 登出 |
| password_reset | 密码重置 |
| email_verified | 邮箱验证 |

**Response**：

```json
{
  "success": true,
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "userId": "user_xxx",
        "username": "lixiaoyi",
        "action": "login_success",
        "ip": "192.168.1.1",
        "userAgent": "Chrome/125.0.0.0",
        "details": "登录成功",
        "timestamp": "2026-05-12T08:00:00"
      },
      {
        "id": 2,
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

## 五、状态码汇总

| 状态码 | 场景 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数校验失败 / 验证码错误 / Token 无效 |
| 401 | 未认证 / Token 无效 |
| 403 | 邮箱未验证 / 权限不足（非 admin） |
| 404 | 用户不存在 |
| 409 | 资源冲突（如重复的唯一键） |
| 423 | 账户锁定 |
| 500 | 服务端内部错误 |

---

## 六、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v2.0 | 2026-05-12 | 新增唯一性检查、忘记密码、用户管理、登录日志 API |
| v1.0 | 2026-05-12 | 初始版本 |
