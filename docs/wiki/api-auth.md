# 用户认证与权限模块 — API 契约

> 本文档描述用户注册、登录、管理员审批等相关接口的字段定义与约束。
> 认证方式：JWT Bearer Token，通过请求头 `Authorization: Bearer <token>` 传递。

---

## 一、普通用户认证接口

### 1.1 用户注册

- **POST** `/api/auth/register`
- **权限**：公开

**请求体（Request Body）**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名，3-50 字符 |
| password | string | 是 | 密码，最少 6 字符 |
| confirmPassword | string | 是 | 确认密码，须与 password 一致 |
| realName | string | 是 | 真实姓名 |

**响应**：
- `200 OK` — 注册成功（无响应体）
- `400 Bad Request` — 参数校验失败或用户名已存在

---

### 1.2 用户登录

- **POST** `/api/auth/login`
- **权限**：公开

**请求体（Request Body）**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**响应**：
- `200 OK` — 登录成功

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

- `401 Unauthorized` — 用户名或密码错误
- `403 Forbidden` — 账号待审批 / 账号已禁用

---

### 1.3 获取当前用户信息

- **GET** `/api/auth/me`
- **权限**：已认证（需携带 JWT Token）

**响应**：

```json
{
  "id": 1,
  "username": "testuser",
  "realName": "测试用户",
  "status": "APPROVED",
  "role": "USER",
  "createdAt": "2026-05-02T12:00:00"
}
```

---

## 二、管理员认证接口

### 2.1 管理员登录

- **POST** `/api/admin/auth/login`
- **权限**：公开

**请求体（Request Body）**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 管理员用户名 |
| password | string | 是 | 密码 |

**响应**：
- `200 OK` — 登录成功，返回 JWT Token（同普通用户格式）
- `401 Unauthorized` — 用户名或密码错误 / 该账号不是管理员

---

### 2.2 创建管理员账号

- **POST** `/api/admin/auth/register`
- **权限**：公开（建议在生产环境限制为仅已有管理员可调用）

**请求体（Request Body）**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |
| realName | string | 是 | 真实姓名 |

**响应**：
- `200 OK` — 创建成功（无响应体）
- `400 Bad Request` — 用户名已存在

---

## 三、用户管理接口（管理员）

### 3.1 获取全部用户列表

- **GET** `/api/admin/users`
- **权限**：ADMIN

**响应**：用户列表数组

```json
[
  {
    "id": 1,
    "username": "admin",
    "realName": "系统管理员",
    "role": "ADMIN",
    "status": "APPROVED",
    "createdAt": "2026-05-02T12:00:00"
  }
]
```

---

### 3.2 审批用户

- **PUT** `/api/admin/users/{id}/approve`
- **权限**：ADMIN

**响应**：
- `200 OK` — 审批成功（状态由 PENDING → APPROVED）

---

### 3.3 禁用用户

- **PUT** `/api/admin/users/{id}/disable`
- **权限**：ADMIN

**响应**：
- `200 OK` — 禁用成功（状态由 APPROVED → DISABLED）

---

### 3.4 启用用户

- **PUT** `/api/admin/users/{id}/enable`
- **权限**：ADMIN

**响应**：
- `200 OK` — 启用成功（状态由 DISABLED → APPROVED）

---

## 四、认证流程说明

### 4.1 普通用户注册审批流程

```
注册（POST /api/auth/register）
    └── 状态 = PENDING
        └── 管理员审批（PUT /api/admin/users/{id}/approve）
            └── 状态 = APPROVED
                └── 可正常登录
```

### 4.2 管理员账号

系统首次启动时，若 `sys_user` 表为空，自动创建默认管理员：
- 用户名：`admin`
- 密码：`admin123`
- **建议首次登录后立即修改密码或创建新管理员账号。**

---

## 五、全局错误码

| HTTP 状态码 | 场景 |
|-------------|------|
| 400 | 参数校验失败 / 用户名已存在 / 两次密码不一致 |
| 401 | 用户名或密码错误 |
| 403 | 账号待审批 / 账号已禁用 / 权限不足（非管理员访问管理接口） |
| 500 | 服务器内部错误 |
