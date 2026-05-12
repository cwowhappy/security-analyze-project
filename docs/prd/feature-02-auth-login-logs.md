# Feature-02.4：登录日志

> 文档版本：v1.0
> 创建日期：2026-05-12
> 优先级：P2
> 预计工时：1.5 天
> 角色要求：admin

---

## 一、功能概述

登录日志记录用户在系统中的所有认证相关行为，包括登录成功、登录失败、登出、密码重置、邮箱验证等操作。管理员可在后台查看完整的登录审计日志。

### 数据库表设计（已在 auth-api-contract.md 中定义）

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

---

## 二、页面设计

### 2.1 登录日志页（`/admin/login-logs`）

**布局**：管理后台布局（侧边栏 + 主内容区）

```
┌────────────────────────────────────────────────────────────────────────┐
│  ┌──────────┐                                                         │
│  │ 🏠 Logo  │   Security Analyze                     [👤 管理员]      │
│  └──────────┘                                                         │
├──────────┬─────────────────────────────────────────────────────────────┤
│          │                                                             │
│ 📊 概览   │   登录日志                                                  │
│          │                                                             │
│ 👥 用户   │  ┌─────────────────────────────────────────────────────┐   │
│          │  │ 🔍 搜索用户...     [操作类型 ▼]  [日期范围]  [导出 CSV] │   │
│ 📝 登录日志│  └─────────────────────────────────────────────────────┘   │
│          │                                                             │
│ ⚙️ 设置   │  ┌─────────────────────────────────────────────────────┐   │
│          │  │ 时间      用户        操作         IP        详情      │   │
│          │  ├─────────────────────────────────────────────────────┤   │
│          │  │ 08:00    lixiaoyi   ✅登录成功   192.168.1  -        │   │
│          │  │ 07:55    lixiaoyi   ❌登录失败   192.168.1  密码错误  │   │
│          │  │ 07:50    lixiaoyi   ❌登录失败   192.168.1  密码错误  │   │
│          │  │ 07:45    lixiaoyi   ❌登录失败   192.168.1  密码错误  │   │
│          │  │ 10:00    analyst01  ✅登录成功   192.168.2  -        │   │
│          │  │ 09:30    lixiaoyi   🔓登出       192.168.1  -        │   │
│          │  └─────────────────────────────────────────────────────┘   │
│          │                                                             │
│          │  显示 1-20 / 共 1,024 条                      < 1 2 3 ... 52 >│
│          │                                                             │
└──────────┴─────────────────────────────────────────────────────────────┘
```

**顶部筛选栏**：

| 筛选项 | 类型 | 说明 |
|--------|------|------|
| 用户搜索 | text | 匹配 userId / username |
| 操作类型 | select | 全部 / 登录成功 / 登录失败 / 登出 / 密码重置 / 邮箱验证 |
| 日期范围 | date-range | 开始日期 ~ 结束日期，默认近7天 |
| 导出 | button | 导出 CSV（可选） |

**日志列表字段**：

| 字段 | 说明 | 宽度 |
|------|------|------|
| 时间 | 精确到分钟，格式 `MM-DD HH:mm` | 120px |
| 用户 | 用户名（可点击跳转用户详情） | 140px |
| 操作 | action 类型，带图标和颜色 | 140px |
| IP | IP 地址（支持 IPv6） | 140px |
| 详情 | details 字段 | flex |
| 设备 | 浏览器 + 系统（hover 显示完整 UA） | 200px |

**操作类型样式**：

| 操作 | 图标 | 颜色 | 说明 |
|------|------|------|------|
| login_success | ✅ | success | 绿色 |
| login_failed | ❌ | danger | 红色，背景微红 |
| logout | 🔓 | text-muted | 灰色 |
| password_reset | 🔑 | warning | 橙色 |
| email_verified | ✉️ | info | 蓝色 |
| force_password_reset | ⚠️ | warning | 橙色，admin 强制 |

---

## 三、组件规范

### 3.1 日志动作 Badge

```vue
<!-- LogActionBadge.vue -->
<script setup lang="ts">
defineProps<{
  action: string
}>()

const actionConfig: Record<string, { icon: string; label: string; color: string }> = {
  login_success: { icon: '✅', label: '登录成功', color: 'var(--success)' },
  login_failed: { icon: '❌', label: '登录失败', color: 'var(--danger)' },
  logout: { icon: '🔓', label: '登出', color: 'var(--text-muted)' },
  password_reset: { icon: '🔑', label: '密码重置', color: '#ff9500' },
  email_verified: { icon: '✉️', label: '邮箱验证', color: 'var(--primary)' },
  force_password_reset: { icon: '⚠️', label: '强制改密', color: '#ff9500' },
}
</script>

<template>
  <span
    class="log-action"
    :style="{ color: actionConfig[action]?.color }"
  >
    {{ actionConfig[action]?.icon }}
    {{ actionConfig[action]?.label || action }}
  </span>
</template>

<style scoped>
.log-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
}
</style>
```

### 3.2 日期范围选择器

```vue
<!-- DateRangePicker.vue -->
<script setup lang="ts">
import { ref } from 'vue'

const startDate = ref('')
const endDate = ref('')

// 快捷选项
const shortcuts = [
  { label: '今天', value: () => {
    const now = new Date()
    return { start: now, end: now }
  }},
  { label: '近7天', value: () => {
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - 7)
    return { start, end }
  }},
  { label: '近30天', value: () => {
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - 30)
    return { start, end }
  }},
]

function applyShortcut(shortcut: typeof shortcuts[0]) {
  const { start, end } = shortcut.value()
  startDate.value = formatDate(start)
  endDate.value = formatDate(end)
  emit('change', { startDate: startDate.value, endDate: endDate.value })
}

function formatDate(date: Date): string {
  return date.toISOString().split('T')[0]
}
</script>

<template>
  <div class="date-range-picker">
    <input
      v-model="startDate"
      type="date"
      class="date-input"
      :max="endDate"
    />
    <span class="date-separator">至</span>
    <input
      v-model="endDate"
      type="date"
      class="date-input"
      :min="startDate"
    />
  </div>
</template>
```

### 3.3 日志表格

```vue
<!-- LoginLogTable.vue -->
<script setup lang="ts">
interface LoginLog {
  id: number
  userId: string
  username: string
  action: string
  ip: string
  userAgent: string
  details?: string
  timestamp: string
}

defineProps<{
  logs: LoginLog[]
  loading?: boolean
}>()

function parseUserAgent(ua: string) {
  // 简化显示：浏览器 + 系统
  const browserMatch = ua.match(/(Chrome|Firefox|Safari|Edge)\/[\d.]+/)
  const osMatch = ua.match(/\(([^)]+)\)/)
  return {
    browser: browserMatch?.[1] || 'Unknown',
    os: osMatch?.[1] || 'Unknown',
  }
}
</script>

<template>
  <div class="log-table-wrapper">
    <table class="log-table">
      <thead>
        <tr>
          <th>时间</th>
          <th>用户</th>
          <th>操作</th>
          <th>IP</th>
          <th>详情</th>
          <th>设备</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="log in logs"
          :key="log.id"
          :class="{ 'row-failed': log.action === 'login_failed' }"
        >
          <td class="time-cell">{{ formatTime(log.timestamp) }}</td>
          <td>
            <a :href="`/admin/users/${log.userId}`" class="user-link">
              {{ log.username }}
            </a>
          </td>
          <td><LogActionBadge :action="log.action" /></td>
          <td class="ip-cell">{{ log.ip }}</td>
          <td class="details-cell">{{ log.details || '-' }}</td>
          <td class="device-cell" :title="log.userAgent">
            {{ parseUserAgent(log.userAgent).browser }} · {{ parseUserAgent(log.userAgent).os }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
```

---

## 四、Store 设计

### 4.1 loginLogs store

```typescript
// frontend/src/stores/modules/loginLogs.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/utils/request'

export interface LoginLog {
  id: number
  userId: string
  username: string
  action: 'login_success' | 'login_failed' | 'logout' | 'password_reset' | 'email_verified' | 'force_password_reset'
  ip: string
  userAgent: string
  details?: string
  timestamp: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export const useLoginLogsStore = defineStore('loginLogs', () => {
  const logs = ref<LoginLog[]>([])
  const total = ref(0)
  const loading = ref(false)

  async function fetchLogs(params: {
    page?: number
    size?: number
    userId?: string
    action?: string
    startDate?: string
    endDate?: string
  }) {
    loading.value = true
    try {
      const data = await http.get<PageResult<LoginLog>>('/api/v1/admin/login-logs', params)
      logs.value = data.list
      total.value = data.total
      return data
    } finally {
      loading.value = false
    }
  }

  // 导出 CSV
  async function exportCSV(params: {
    userId?: string
    action?: string
    startDate?: string
    endDate?: string
  }) {
    // 构建下载链接
    const searchParams = new URLSearchParams()
    for (const [key, value] of Object.entries(params)) {
      if (value) searchParams.append(key, value)
    }
    const url = `/api/v1/admin/login-logs/export?${searchParams.toString()}`

    // 使用 fetch 下载
    const token = localStorage.getItem('sai_token') || sessionStorage.getItem('sai_token')
    const response = await fetch(url, {
      headers: { 'Authorization': `Bearer ${token}` }
    })

    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = `login-logs-${new Date().toISOString().split('T')[0]}.csv`
    link.click()
    URL.revokeObjectURL(downloadUrl)
  }

  return {
    logs,
    total,
    loading,
    fetchLogs,
    exportCSV,
  }
})
```

---

## 五、路由配置

```typescript
{
  path: '/admin',
  component: AdminLayout,
  meta: { requiresAuth: true, requiresAdmin: true },
  children: [
    // ... 用户管理路由
    {
      path: 'login-logs',
      name: 'AdminLoginLogs',
      component: () => import('@/views/admin/logs/LoginLogsView.vue'),
      meta: { title: '登录日志' },
    },
  ],
}
```

---

## 六、API 契约

### 6.1 获取登录日志列表（已在 auth-api-contract.md 中定义）

```
GET /api/v1/admin/login-logs
```

**Request（Query）**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页大小，默认 20 |
| userId | string | 否 | 用户 ID 筛选 |
| action | string | 否 | 动作类型 |
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
        "id": 1,
        "userId": "user_xxx",
        "username": "lixiaoyi",
        "action": "login_success",
        "ip": "192.168.1.1",
        "userAgent": "Chrome/125.0.0.0",
        "details": "登录成功",
        "timestamp": "2026-05-12T08:00:00"
      }
    ],
    "total": 1024,
    "page": 1,
    "size": 20
  }
}
```

### 6.2 导出登录日志 CSV（新增）

```
GET /api/v1/admin/login-logs/export
```

**Request（Query）**：同上

**Response**：CSV 文件下载

---

## 七、后端实现要点

### 7.1 登录日志记录时机

在 `AuthAppServiceImpl` 中记录：

```java
// 登录成功时
loginLogService.record(userId, username, "login_success", ip, userAgent, "登录成功");

// 登录失败时
loginLogService.record(null, attemptedUsername, "login_failed", ip, userAgent,
    String.format("密码错误（第%d次）", attempts));

// 登出时
loginLogService.record(userId, username, "logout", ip, userAgent, "正常登出"));

// 密码重置时
loginLogService.record(userId, username, "password_reset", ip, userAgent, "邮箱重置"));

// 邮箱验证时
loginLogService.record(userId, username, "email_verified", ip, userAgent, "邮箱验证成功"));
```

### 7.2 日志查询优化

- 按 `created_at` 倒序排列
- 支持分页（每页 20 条，默认）
- 日期范围默认查询近 7 天
- IP 支持 IPv6 存储（VARCHAR(45)）
- 定期清理：保留 180 天日志

---

## 八、功能清单

| 功能点 | 状态 | 说明 |
|--------|------|------|
| LoginLogsView 页面 | ❌ 待补充 | 日志列表 + 筛选 |
| LoginLogTable 组件 | ❌ 待补充 | 表格组件 |
| LogActionBadge 组件 | ❌ 待补充 | 操作类型标签 |
| DateRangePicker 组件 | ❌ 待补充 | 日期范围选择 |
| loginLogs Store | ❌ 待补充 | 状态管理 |
| 路由配置 | ❌ 待补充 | /admin/login-logs |
| 后端 LoginLogService | ❌ 待补充 | 日志记录服务 |
| 后端 LoginLogController | ❌ 待补充 | GET /api/v1/admin/login-logs |
| 数据库 tb_login_log 表 | ⚠️ 仅设计 | 待创建 |

---

## 九、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | 2026-05-12 | 初始版本，设计登录日志功能 |
