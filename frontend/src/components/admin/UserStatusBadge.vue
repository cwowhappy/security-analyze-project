<script setup lang="ts">
defineProps<{
  emailVerified: boolean
  locked: boolean
}>()
</script>

<template>
  <span
    class="status-badge"
    :class="{
      verified: emailVerified && !locked,
      unverified: !emailVerified && !locked,
      locked: locked,
    }"
  >
    <span class="status-dot" />
    {{ locked ? '已锁定' : emailVerified ? '已验证' : '未验证' }}
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
