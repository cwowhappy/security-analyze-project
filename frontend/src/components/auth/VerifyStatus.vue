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
      <div class="icon-invalid">⚠️</div>
      <h2>缺少验证信息</h2>
      <p>验证链接不完整，请检查邮件内容</p>
    </div>
  </div>
</template>

<style scoped>
.verify-status {
  text-align: center;
  padding: 24px 0;
}

.verify-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: var(--text-secondary);
  font-size: 14px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.icon-success,
.icon-error,
.icon-invalid {
  font-size: 56px;
  margin-bottom: 16px;
}

.verify-success h2,
.verify-error h2,
.verify-invalid h2 {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.verify-success p,
.verify-error p,
.verify-invalid p {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 24px;
}
</style>
