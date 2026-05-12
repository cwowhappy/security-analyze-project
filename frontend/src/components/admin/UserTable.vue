<script setup lang="ts">
import UserStatusBadge from './UserStatusBadge.vue'
import UserRoleBadge from './UserRoleBadge.vue'
import type { User } from '@/stores/modules/admin'

defineProps<{
  users: User[]
  loading?: boolean
}>()

defineEmits<{
  (e: 'view', userId: string): void
}>()

function formatTime(timestamp: string | undefined): string {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
}
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
          :class="{
            'row-locked': user.locked,
            'row-unverified': !user.emailVerified && !user.locked,
          }"
        >
          <td>
            <div class="user-cell">
              <div class="avatar">{{ user.avatarInitial || user.username[0].toUpperCase() }}</div>
              <span class="username">{{ user.username }}</span>
            </div>
          </td>
          <td class="email-cell">{{ user.email }}</td>
          <td><UserRoleBadge :role="user.role" /></td>
          <td>
            <UserStatusBadge :email-verified="user.emailVerified" :locked="user.locked" />
          </td>
          <td class="time-cell">{{ formatTime(user.lastLoginAt) }}</td>
          <td>
            <button class="btn-view" @click="$emit('view', user.id)">查看</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="loading" class="table-loading">加载中...</div>
    <div v-if="!loading && users.length === 0" class="table-empty">暂无数据</div>
  </div>
</template>

<style scoped>
.user-table-wrapper {
  overflow-x: auto;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.user-table th {
  text-align: left;
  padding: 10px 12px;
  font-weight: 600;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}

.user-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
  color: var(--text-primary);
}

.user-table tbody tr:hover {
  background: var(--surface-hover);
}

.user-table tbody tr.row-locked {
  background: rgba(255, 59, 48, 0.04);
}

.user-table tbody tr.row-unverified {
  background: rgba(255, 200, 0, 0.04);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.username {
  font-weight: 500;
}

.email-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-cell {
  white-space: nowrap;
  color: var(--text-muted);
}

.btn-view {
  padding: 4px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--text-primary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-view:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.table-loading,
.table-empty {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}
</style>
