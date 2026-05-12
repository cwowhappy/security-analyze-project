<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/utils/request'

const router = useRouter()

const form = reactive({
  email: '',
})

const error = ref('')
const message = ref('')
const loading = ref(false)

function validate(): boolean {
  if (!form.email.trim()) {
    error.value = '请输入邮箱地址'
    return false
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    error.value = '请输入有效的邮箱地址'
    return false
  }
  error.value = ''
  return true
}

async function handleSubmit() {
  message.value = ''
  error.value = ''
  if (!validate()) return

  loading.value = true
  try {
    await http.post<null>('/api/v1/auth/forgot-password', {
      email: form.email.trim(),
    })
    message.value = '重置链接已发送至您的邮箱，请查收'
  } catch (err: any) {
    error.value = err.message || '发送失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function handleKeyup(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    handleSubmit()
  }
}
</script>

<template>
  <div class="auth-card">
    <div class="auth-title">忘记密码</div>
    <div class="auth-sub">请输入注册时使用的邮箱，我们将发送密码重置链接</div>

    <div v-if="message" class="auth-msg success">{{ message }}</div>

    <div class="form-group">
      <label class="form-label">注册邮箱</label>
      <input
        v-model="form.email"
        type="email"
        class="form-input"
        :class="{ error: error }"
        placeholder="请输入注册邮箱"
        @keyup="handleKeyup"
      />
      <div v-if="error" class="form-error">{{ error }}</div>
    </div>

    <button class="form-btn form-btn-primary" :disabled="loading" @click="handleSubmit">
      {{ loading ? '发送中...' : '发送重置链接' }}
    </button>

    <div class="form-divider">或</div>
    <button class="form-btn form-btn-secondary" @click="router.push('/login')">
      返回登录
    </button>
  </div>
</template>

<style scoped>
.auth-card {
  width: 100%;
  max-width: 340px;
}

.auth-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.auth-sub {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 28px;
}

.form-group {
  margin-bottom: 18px;
}

.form-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
  letter-spacing: 0.3px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: var(--font);
  color: var(--text-primary);
  background: var(--surface);
  transition: border-color 0.15s, box-shadow 0.15s, background 0.25s;
  outline: none;
}

.form-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(99, 91, 255, 0.12);
}

.form-input.error {
  border-color: var(--danger);
}

.form-error {
  font-size: 12px;
  color: var(--danger);
  margin-top: 4px;
}

.form-btn {
  width: 100%;
  padding: 11px 0;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 700;
  font-family: var(--font);
  cursor: pointer;
  border: none;
  transition: all 0.15s;
  margin-bottom: 14px;
}

.form-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-btn-primary {
  background: var(--primary);
  color: #fff;
}

.form-btn-primary:hover:not(:disabled) {
  background: var(--primary-hover);
}

.form-btn-secondary {
  background: var(--surface);
  color: var(--text-primary);
  border: 1px solid var(--border);
}

.form-btn-secondary:hover:not(:disabled) {
  background: var(--surface-hover);
}

.form-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0;
  color: var(--text-muted);
  font-size: 12px;
}

.form-divider::before,
.form-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border);
}

.auth-msg {
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  margin-bottom: 16px;
}

.auth-msg.success {
  background: rgba(0, 217, 36, 0.1);
  color: var(--success);
  border: 1px solid rgba(0, 217, 36, 0.25);
}
</style>
