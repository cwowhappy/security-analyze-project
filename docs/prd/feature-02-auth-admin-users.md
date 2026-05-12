# Feature-02.3：用户管理后台

> 文档版本：v1.0
> 创建日期：2026-05-12
> 优先级：P2
> 预计工时：2 天
> 角色要求：admin

---

## 一、功能概述

用户管理后台是管理员（admin 角色）专用的用户管理界面，提供用户列表查看、用户信息编辑、账户解锁、会话管理等功能。

### 页面清单

| 页面 | 路由 | 说明 |
|------|------|------|
| 用户管理首页 | `/admin/users` | 用户列表 + 搜索筛选 |
| 用户详情页 | `/admin/users/:userId` | 用户详细信息 + 会话管理 |

---

## 二、页面设计

### 2.1 用户管理首页（`/admin/users`）

**布局**：管理后台布局（侧边栏 + 主内容区）

```
┌────────────────────────────────────────────────────────────────────────┐
│  ┌──────────┐                                                         │
│  │ 🏠 Logo  │   Security Analyze                     [👤 管理员]      │
│  └──────────┘                                                         │
├──────────┬─────────────────────────────────────────────────────────────┤
│          │                                                             │
│ 📊 概览   │  用户管理                                                    │
│          │                                                             │
│ 👥 用户   │  ┌─────────────────────────────────────────────────────┐   │
│          │  │ 🔍 搜索用户名/邮箱...     [角色 ▼] [状态 ▼] [+ 新增用户]│   │
│ 📝 内容   │  └─────────────────────────────────────────────────────┘   │
│          │                                                             │
│ ⚙️ 设置   │  ┌─────────────────────────────────────────────────────┐   │
│          │  │ 用户名    邮箱           角色        状态    操作      │   │
│          │  ├─────────────────────────────────────────────────────┤   │
│          │  │ L小艺    lixiaoyi@...  投资经理    ✓ 已验证  [查看]   │   │
│          │  │ A分析员  analyst01@...  分析师      ✓ 已验证  [查看]   │   │
│          │  │ V访客    viewer01@...   观察者      ⚠ 未验证  [查看]   │   │
│          │  │ X小黑    hacker@...     观察者      🔒 已锁定  [查看]   │   │
│          │  └─────────────────────────────────────────────────────┘   │
│          │                                                             │
│          │  显示 1-20 / 共 156 条                        < 1 2 3 ... 8 >│
│          │                                                             │
└──────────┴─────────────────────────────────────────────────────────────┘
```

**顶部筛选栏**：

| 筛选项 | 类型 | 选项 |
|--------|------|------|
| 关键词搜索 | text | 匹配 username / email / displayName |
| 角色 | select | 全部 / 投资组合经理 / 分析师 / 观察者 / 管理员 |
| 状态 | select | 全部 / 已验证 / 未验证 / 已锁定 |

**用户列表字段**：

| 字段 | 说明 |
|------|------|
| 头像 | 用户名首字母，圆形 |
| 用户名 | 点击可复制 |
| 邮箱 | 显示前20字符 |
| 角色 | badge 标签 |
| 状态 | 已验证 ✓ / 未验证 ⚠ / 已锁定 🔒 |
| 最后登录 | 相对时间 |
| 操作 | 查看详情按钮 |

**表格行样式**：
- 锁定用户：整行背景 `rgba(255,59,48,0.05)`
- 未验证用户：整行背景 `rgba(255,200,0,0.05)`

### 2.2 用户详情页（`/admin/users/:userId`）

**布局**：管理后台布局

```
┌────────────────────────────────────────────────────────────────────────┐
│  ┌──────────┐                                                         │
│  │ 🏠 Logo  │   Security Analyze                     [👤 管理员]      │
│  └──────────┘                                                         │
├──────────┬─────────────────────────────────────────────────────────────┤
│          │  ← 返回用户列表                                             │
│ 📊 概览   │                                                             │
│          │  👤 用户详情                                                │
│ 👥 用户   │  ┌─────────────────────────────────────────────────────┐   │
│          │  │ 基本信息                      [编辑] [强制修改密码]     │   │
│ 📝 内容   │  │ ───────────────────────────────────────────────    │   │
│          │  │ 用户ID：  user_xxx123456                             │   │
│ ⚙️ 设置   │  │ 用户名：  lixiaoyi                                  │   │
│          │  │ 邮箱：    lixiaoyi@example.com                      │   │
│          │  │ 角色：    投资组合经理  [变更角色 ▼]                   │   │
│          │  │ 注册时间：2026-05-01 10:30                           │   │
│          │  │ 最后登录：2026-05-12 08:00 (3小时前)                  │   │
│          │  └─────────────────────────────────────────────────────┘   │
│          │                                                             │
│          │  账户状态                                                   │
│          │  ┌─────────────────────────────────────────────────────┐   │
│          │  │ 邮箱验证：  ✓ 已验证              [发送验证邮件]       │   │
│          │  │ 账户状态：  🔓 正常               [锁定账户]           │   │
│          │  │ 登录失败：  0 次                  [重置失败次数]      │   │
│          │  │ 锁定截止：  -                     [手动解锁]          │   │
│          │  └─────────────────────────────────────────────────────┘   │
│          │                                                             │
│          │  活跃会话 (3)                                               │
│          │  ┌─────────────────────────────────────────────────────┐   │
│          │  │ 🌐 Chrome · Windows · 192.168.1.1   [当前]  [终止]  │   │
│          │  │ 🌐 Safari · macOS · 192.168.1.2                    [终止]│   │
│          │  │ 📱 iPhone · iOS · 10.0.0.1                        [终止]│   │
│          │  └─────────────────────────────────────────────────────┘   │
│          │                                                             │
│          │  操作日志                                                   │
│          │  ┌─────────────────────────────────────────────────────┐   │
│          │  │ 2026-05-12 08:00  登录成功        Chrome/Win         │   │
│          │  │ 2026-05-12 07:55  登录失败(3次)   Chrome/Win         │   │
│          │  │ 2026-05-10 14:00  邮箱验证成功    -                  │   │
│          │  └─────────────────────────────────────────────────────┘   │
└──────────┴─────────────────────────────────────────────────────────────┘
```

---

## 三、组件规范

### 3.1 用户状态 Badge

```vue
<!-- UserStatusBadge.vue -->
<script setup lang="ts">
defineProps<{
  emailVerified: boolean
  isLocked: boolean
}>()
</script>

<template>
  <span
    class="status-badge"
    :class="{
      'verified': emailVerified && !isLocked,
      'unverified': !emailVerified,
      'locked': isLocked
    }"
  >
    <span class="status-dot" />
    {{ isLocked ? '已锁定' : (emailVerified ? '已验证' : '未验证') }}
  </span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-badge.verified {
  background: rgba(0, 217, 36, 0.1);
  color: var(--success);
}
.status-badge.verified .status-dot {
  background: var(--success);
}

.status-badge.unverified {
  background: rgba(255, 200, 0, 0.1);
  color: #ff9500;
}
.status-badge.unverified .status-dot {
  background: #ff9500;
}

.status-badge.locked {
  background: rgba(255, 59, 48, 0.1);
  color: var(--danger);
}
.status-badge.locked .status-dot {
  background: var(--danger);
}
</style>
```

### 3.2 用户角色 Badge

```vue
<!-- UserRoleBadge.vue -->
<script setup lang="ts">
defineProps<{
  role: string
}>()

const roleMap: Record<string, { label: string; color: string }> = {
  admin: { label: '管理员', color: '#6366f1' },
  portfolio_manager: { label: '投资经理', color: '#10b981' },
  analyst: { label: '分析师', color: '#3b82f6' },
  viewer: { label: '观察者', color: '#94a3b8' },
}
</script>

<template>
  <span
    class="role-badge"
    :style="{ background: `${roleMap[role]?.color}15`, color: roleMap[role]?.color }"
  >
    {{ roleMap[role]?.label || role }}
  </span>
</template>

<style scoped>
.role-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
</style>
```

### 3.3 用户表格

```vue
<!-- UserTable.vue -->
<script setup lang="ts">
interface User {
  id: string
  username: string
  email: string
  role: string
  displayName: string
  emailVerified: boolean
  isLocked: boolean
  lastLoginAt?: string
}

defineProps<{
  users: User[]
  loading?: boolean
}>()

defineEmits<{
  (e: 'view', userId: string): void
}>()
</script>

<template>
  <div class="user-table-wrapper">
    <table class="user-table">
      <thead>
        <tr>
          <th>用户</th>
          <th>邮箱</th>
          <th>角色</th>
          <th>状态</th>
          <th>最后登录</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="user in users"
          :key="user.id"
          :class="{ 'row-locked': user.isLocked, 'row-unverified': !user.emailVerified && !user.isLocked }"
        >
          <td>
            <div class="user-cell">
              <div class="avatar">{{ user.username[0].toUpperCase() }}</div>
              <span class="username">{{ user.username }}</span>
            </div>
          </td>
          <td class="email-cell">{{ user.email }}</td>
          <td><UserRoleBadge :role="user.role" /></td>
          <td>
            <UserStatusBadge :email-verified="user.emailVerified" :is-locked="user.isLocked" />
          </td>
          <td class="time-cell">{{ user.lastLoginAt || '-' }}</td>
          <td>
            <button class="btn-view" @click="$emit('view', user.id)">查看</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
```

---

## 四、Store 设计

### 4.1 admin store（待创建）

```typescript
// frontend/src/stores/modules/admin.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/utils/request'

export interface User {
  id: string
  username: string
  email: string
  displayName: string
  role: string
  avatarInitial: string
  emailVerified: boolean
  isLocked: boolean
  lockedUntil: string | null
  failedLoginAttempts: number
  lastLoginAt: string
  createdAt: string
}

export interface LoginSession {
  sessionId: string
  ip: string
  userAgent: string
  createdAt: string
  expiresAt: string
  isCurrent: boolean
}

export interface UserDetail extends User {
  updatedAt: string
  loginSessions: LoginSession[]
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export const useAdminStore = defineStore('admin', () => {
  const users = ref<User[]>([])
  const currentUser = ref<UserDetail | null>(null)
  const total = ref(0)
  const loading = ref(false)

  // 获取用户列表
  async function fetchUsers(params: {
    page?: number
    size?: number
    role?: string
    keyword?: string
    emailVerified?: boolean
    locked?: boolean
  }) {
    loading.value = true
    try {
      const data = await http.get<PageResult<User>>('/api/v1/admin/users', params)
      users.value = data.list
      total.value = data.total
      return data
    } finally {
      loading.value = false
    }
  }

  // 获取用户详情
  async function fetchUserDetail(userId: string) {
    loading.value = true
    try {
      const data = await http.get<UserDetail>(`/api/v1/admin/users/${userId}`)
      currentUser.value = data
      return data
    } finally {
      loading.value = false
    }
  }

  // 更新用户信息
  async function updateUser(userId: string, data: { displayName?: string; role?: string }) {
    await http.put<void>(`/api/v1/admin/users/${userId}`, data)
    // 刷新当前用户
    if (currentUser.value?.id === userId) {
      await fetchUserDetail(userId)
    }
  }

  // 解锁账户
  async function unlockUser(userId: string) {
    await http.post<void>(`/api/v1/admin/users/${userId}/unlock`, {})
    // 刷新
    if (currentUser.value?.id === userId) {
      await fetchUserDetail(userId)
    }
  }

  // 终止指定会话
  async function terminateSession(userId: string, sessionId: string) {
    await http.delete<void>(`/api/v1/admin/users/${userId}/sessions/${sessionId}`)
    // 刷新
    if (currentUser.value?.id === userId) {
      await fetchUserDetail(userId)
    }
  }

  // 终止所有其他会话
  async function terminateAllSessions(userId: string) {
    await http.delete<void>(`/api/v1/admin/users/${userId}/sessions`)
    // 刷新
    if (currentUser.value?.id === userId) {
      await fetchUserDetail(userId)
    }
  }

  // 强制修改密码
  async function forcePasswordReset(userId: string, reason?: string) {
    await http.post<void>(`/api/v1/admin/users/${userId}/force-password-reset`, { reason })
  }

  return {
    users,
    currentUser,
    total,
    loading,
    fetchUsers,
    fetchUserDetail,
    updateUser,
    unlockUser,
    terminateSession,
    terminateAllSessions,
    forcePasswordReset,
  }
})
```

---

## 五、路由配置

```typescript
// frontend/src/router/index.ts

// Admin 布局组件
const AdminLayout = () => import('@/views/admin/AdminLayout.vue')

// 路由配置
{
  path: '/admin',
  component: AdminLayout,
  meta: { requiresAuth: true, requiresAdmin: true },
  children: [
    {
      path: 'users',
      name: 'AdminUsers',
      component: () => import('@/views/admin/users/UserListView.vue'),
      meta: { title: '用户管理' },
    },
    {
      path: 'users/:userId',
      name: 'AdminUserDetail',
      component: () => import('@/views/admin/users/UserDetailView.vue'),
      meta: { title: '用户详情' },
    },
  ],
}
```

---

## 六、API 契约（已在 auth-api-contract.md 中定义）

| API | 方法 | 路径 | 状态 |
|-----|------|------|------|
| 获取用户列表 | GET | /api/v1/admin/users | ⚠️ 后端待实现 |
| 获取用户详情 | GET | /api/v1/admin/users/{userId} | ⚠️ 后端待实现 |
| 更新用户信息 | PUT | /api/v1/admin/users/{userId} | ⚠️ 后端待实现 |
| 解锁账户 | POST | /api/v1/admin/users/{userId}/unlock | ⚠️ 后端待实现 |
| 终止会话 | DELETE | /api/v1/admin/users/{userId}/sessions/{sessionId} | ⚠️ 后端待实现 |
| 终止所有会话 | DELETE | /api/v1/admin/users/{userId}/sessions | ⚠️ 后端待实现 |
| 强制改密 | POST | /api/v1/admin/users/{userId}/force-password-reset | ⚠️ 后端待实现 |

---

## 七、功能清单

| 功能点 | 状态 | 说明 |
|--------|------|------|
| AdminLayout 布局 | ❌ 待补充 | 侧边栏 + 主内容区 |
| UserListView 用户列表页 | ❌ 待补充 | 搜索/筛选/分页 |
| UserDetailView 用户详情页 | ❌ 待补充 | 详情/编辑/会话管理 |
| UserTable 组件 | ❌ 待补充 | 表格组件 |
| UserStatusBadge 组件 | ❌ 待补充 | 状态标签 |
| UserRoleBadge 组件 | ❌ 待补充 | 角色标签 |
| admin Store | ❌ 待补充 | 状态管理 |
| 路由守卫 admin 权限检查 | ❌ 待补充 | 仅 admin 可访问 |
| 后端 AdminController | ❌ 待补充 | CRUD API |
| 后端 AdminService | ❌ 待补充 | 业务逻辑 |

---

## 八、版本记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | 2026-05-12 | 初始版本，设计用户管理后台 |
