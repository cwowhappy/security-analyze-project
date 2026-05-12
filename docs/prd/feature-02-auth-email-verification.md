# Feature-02.2：邮箱验证流程完善

> 文档版本：v1.0
> 创建日期：2026-05-12
> 优先级：P2
> 预计工时：1 天
> 依赖：Feature-02 P1（用户注册）

---

## 一、功能概述

邮箱验证是用户注册后的安全增强流程，确保用户邮箱真实有效。系统将在用户注册后发送验证码邮件，用户点击链接或输入验证码完成验证。

### 当前状态

| 组件 | 状态 | 说明 |
|------|------|------|
| RegisterView.vue 注册后验证 | ✅ 已完成 | 注册成功后显示验证码输入界面 |
| AuthController.verifyEmail | ✅ 已完成 | POST /api/v1/auth/verify-email |
| AuthController.resendVerification | ✅ 已完成 | POST /api/v1/auth/resend-verification |
| EmailVerificationService | ⚠️ 待确认 | 邮件发送服务 |
| 独立验证页 `/verify-email` | ❌ 缺失 | 邮件链接跳转页 |

---

## 二、页面设计

### 2.1 独立邮箱验证页（`/verify-email?userId=xxx&code=xxx`）

**入口**：
- 用户点击注册邮件中的验证链接
- 注册成功后手动验证

**布局**：简化居中卡片，单步流程

**页面状态**：

#### 状态 1：验证成功
```
┌────────────────────────────────────┐
│  ✅                                │
│                                    │
│  邮箱验证成功                       │
│                                    │
│  您的邮箱 verification@example.com │
│  已验证成功                        │
│                                    │
│  ┌──────────────────────────────┐ │
│  │       前往登录                 │ │
│  └──────────────────────────────┘ │
│                                    │
│  没有收到邮件？                    │
│  · 尝试重新发送                   │
│  · 检查垃圾邮件文件夹             │
└────────────────────────────────────┘
```

#### 状态 2：验证失败
```
┌────────────────────────────────────┐
│  ❌                                │
│                                    │
│  验证失败                          │
│                                    │
│  验证码无效或已过期                 │
│  请尝试重新发送验证码              │
│                                    │
│  ┌──────────────────────────────┐ │
│  │       重新发送验证码            │ │
│  └──────────────────────────────┘ │
│                                    │
│  ┌──────────────────────────────┐ │
│  │       返回注册                 │ │
│  └──────────────────────────────┘ │
└────────────────────────────────────┘
```

#### 状态 3：链接参数不完整
```
┌────────────────────────────────────┐
│  缺少验证信息                      │
│                                    │
│  验证链接不完整，请检查邮件内容      │
│  或重新发起注册流程                │
│                                    │
│  ┌──────────────────────────────┐ │
│  │       返回注册                 │ │
│  └──────────────────────────────┘ │
└────────────────────────────────────┘
```

**API 交互**：
1. 页面加载时解析 `userId` 和 `code` 参数
2. 如果参数完整，自动调用 `POST /api/v1/auth/verify-email`
3. 如果参数不完整，显示状态 3
4. 成功：显示状态 1，自动跳转登录页
5. 失败：显示状态 2，允许重新发送

---

## 三、组件规范

### 3.1 验证状态展示组件

```vue
<!-- VerifyStatus.vue -->
<script setup lang="ts">
defineProps<{
  status: 'loading' | 'success' | 'error' | 'invalid'
  email?: string
}>()
</script>

<template>
  <div class="verify-status">
    <!-- 加载态 -->
    <div v-if="status === 'loading'" class="verify-loading">
      <div class="spinner" />
      <span>正在验证邮箱...</span>
    </div>

    <!-- 成功态 -->
    <div v-else-if="status === 'success'" class="verify-success">
      <div class="icon-success">✅</div>
      <h2>邮箱验证成功</h2>
      <p v-if="email">您的邮箱 {{ email }} 已验证成功</p>
    </div>

    <!-- 失败态 -->
    <div v-else-if="status === 'error'" class="verify-error">
      <div class="icon-error">❌</div>
      <h2>验证失败</h2>
      <p>验证码无效或已过期</p>
    </div>

    <!-- 无效链接态 -->
    <div v-else class="verify-invalid">
      <h2>缺少验证信息</h2>
      <p>验证链接不完整，请检查邮件内容</p>
    </div>
  </div>
</template>
```

### 3.2 验证结果卡片样式

```css
.verify-card {
  width: 100%;
  max-width: 400px;
  text-align: center;
}

.verify-icon {
  font-size: 64px;
  margin-bottom: 24px;
}

.verify-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.verify-message {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 32px;
}

.verify-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
```

---

## 四、后端接口确认

### 4.1 验证邮箱（已有）

```
POST /api/v1/auth/verify-email
Content-Type: application/json

{
  "userId": "user_xxx",
  "code": "123456"
}
```

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "邮箱验证成功"
}
```

### 4.2 重新发送验证码（已有）

```
POST /api/v1/auth/resend-verification?userId=xxx
```

**Response**：
```json
{
  "success": true,
  "code": 200,
  "message": "验证码已重新发送"
}
```

---

## 五、邮件模板设计

### 5.1 验证邮件模板

```
主题：Security Analyze - 请验证您的邮箱

亲爱的 {username}：

感谢您注册 Security Analyze 账号！

请在 24 小时内点击以下链接完成邮箱验证：
{verification_link}

如果按钮无法点击，请复制以下链接到浏览器打开：
{verification_link}

此链接将在 24 小时后失效。

---
Security Analyze Team
```

### 5.2 验证码邮件模板

```
主题：Security Analyze - 您的邮箱验证码

您的验证码是：{code}

验证码将在 10 分钟后失效，请尽快使用。

如果这不是您的操作，请忽略此邮件。

---
Security Analyze Team
```

---

## 六、路由配置

```typescript
// frontend/src/router/index.ts
{
  path: '/verify-email',
  name: 'VerifyEmail',
  component: () => import('@/views/auth/VerifyEmailView.vue'),
  meta: { public: true, title: '验证邮箱' },
}
```

---

## 七、功能清单

| 功能点 | 状态 | 说明 |
|--------|------|------|
| 独立验证页 UI | ❌ 待补充 | 本文档设计 |
| 验证链接自动验证 | ❌ 待补充 | 需实现 |
| 验证码输入验证 | ✅ 已完成 | RegisterView.vue |
| 重新发送验证码 | ✅ 已完成 | Store + API |
| 邮件模板设计 | ⚠️ 待实现 | 需要邮件服务集成 |

---

## 八、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | 2026-05-12 | 初始版本，设计独立验证页 |
