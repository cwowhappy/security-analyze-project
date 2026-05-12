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
  <span class="log-action" :style="{ color: actionConfig[action]?.color }">
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
