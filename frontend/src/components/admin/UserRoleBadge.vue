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
