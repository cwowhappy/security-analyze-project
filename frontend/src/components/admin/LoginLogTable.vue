<script setup lang="ts">
import LogActionBadge from './LogActionBadge.vue'
import type { LoginLog } from '@/stores/modules/loginLogs'

defineProps<{
  logs: LoginLog[]
  loading?: boolean
}>()

function formatTime(timestamp: string): string {
  const date = new Date(timestamp)
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  const hour = date.getHours().toString().padStart(2, '0')
  const minute = date.getMinutes().toString().padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function parseUserAgent(ua: string) {
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
            <span class="user-link">{{ log.username || '-' }}</span>
          </td>
          <td><LogActionBadge :action="log.action" /></td>
          <td class="ip-cell">{{ log.ip || '-' }}</td>
          <td class="details-cell">{{ log.details || '-' }}</td>
          <td class="device-cell" :title="log.userAgent">
            {{ parseUserAgent(log.userAgent).browser }} · {{ parseUserAgent(log.userAgent).os }}
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="loading" class="table-loading">加载中...</div>
    <div v-if="!loading && logs.length === 0" class="table-empty">暂无数据</div>
  </div>
</template>

<style scoped>
.log-table-wrapper {
  overflow-x: auto;
}

.log-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.log-table th {
  text-align: left;
  padding: 10px 12px;
  font-weight: 600;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}

.log-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
  color: var(--text-primary);
  white-space: nowrap;
}

.log-table tbody tr:hover {
  background: var(--surface-hover);
}

.log-table tbody tr.row-failed {
  background: rgba(255, 59, 48, 0.04);
}

.time-cell {
  width: 110px;
  font-variant-numeric: tabular-nums;
}

.user-link {
  color: var(--primary);
  cursor: pointer;
}

.user-link:hover {
  text-decoration: underline;
}

.ip-cell {
  width: 130px;
  font-variant-numeric: tabular-nums;
}

.details-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-cell {
  width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-loading,
.table-empty {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}
</style>
