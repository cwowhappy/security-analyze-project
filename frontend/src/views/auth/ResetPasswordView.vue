<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const token = ref('')
const userId = ref('')
const email = ref('')
const tokenValid = ref(false)
const checkingToken = ref(true)

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

const errors = reactive({
  newPassword: '',
  confirmPassword: '',
})

const message = ref('')
const messageType = ref<'success' | 'error'>('error')
const loading = ref(false)

const pwdStrength = ref(0)
const pwdLabel = ref('')
const pwdColor = ref('')

onMounted(async () => {
  const resetToken = route.query.token as string
  if (!resetToken) {
    message.value = '无效的重置链接，请重新申请'
    messageType.value = 'error'
    checkingToken.value = false
    return
  }

  token.value = resetToken
  try {
    const result = await authStore.verifyResetToken(resetToken)
    userId.value = result.userId
    email.value = result.email
    tokenValid.value = true
  } catch (err: any) {
    message.value = err.message || '重置链接已失效，请重新申请'
    messageType.value = 'error'
    tokenValid.value = false
  } finally {
    checkingToken.value = false
  }
})

function validatePassword(): boolean {
  if (!form.newPassword) {
    errors.newPassword = '请输入新密码'
    return false
  }
  if (form.newPassword.length < 8) {
    errors.newPassword = '密码至少8位'
    return false
  }
  if (!/[a-zA-Z]/.test(form.newPassword) || !/\d/.test(form.newPassword)) {
    errors.newPassword = '需同时包含字母和数字'
    return false
  }
  errors.newPassword = ''
  return true
}

function validateConfirmPassword(): boolean {
  if (!form.confirmPassword) {
    errors.confirmPassword = '请再次输入密码'
    return false
  }
  if (form.newPassword !== form.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
    return false
  }
  errors.confirmPassword = ''
  return true
}

function updatePwdStrength() {
  const p = form.newPassword
  if (!p) {
    pwdStrength.value = 0
    pwdLabel.value = ''
    pwdColor.value = ''
    return
  }
  let score = 0
  if (p.length >= 8) score++
  if (/[a-zA-Z]/.test(p) && /\d/.test(p)) score++
  if (/[!@#$%^&*(),.?":{}|<>]/.test(p)) score++

  pwdStrength.value = score
  if (score >= 3) {
    pwdLabel.value = '密码强度：强'
    pwdColor.value = 'var(--success)'
  } else if (score >= 2) {
    pwdLabel.value = '密码强度：中'
    pwdColor.value = '#FF9500'
  } else {
    pwdLabel.value = '密码强度：弱'
    pwdColor.value = 'var(--danger)'
  }
}

watch(() => form.newPassword, updatePwdStrength)

function validateAll(): boolean {
  return validatePassword() && validateConfirmPassword()
}

async function handleReset() {
  message.value = ''
  if (!validateAll()) return

  loading.value = true
  try {
    await authStore.resetPassword(token.value, form.newPassword, form.confirmPassword)
    message.value = '密码重置成功，即将跳转到登录页'
    messageType.value = 'success'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  } catch (err: any) {
    message.value = err.message || '密码重置失败'
    messageType.value = 'error'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-card">
    <div class="auth-title">重置密码</div>

    <div v-if="checkingToken" class="checking-token">
      <div class="spinner" />
      <span>正在验证链接...</span>
    </div>

    <template v-else-if="tokenValid">
      <div class="auth-sub">请为您的账户 {{ email }} 设置新密码</div>

      <div v-if="message" class="auth-msg" :class="messageType">{{ message }}</div>

      <div class="form-group">
        <label class="form-label">新密码</label>
        <input
          v-model="form.newPassword"
          type="password"
          class="form-input"
          :class="{ error: errors.newPassword }"
          placeholder="至少8位，含字母+数字"
        />
        <div class="pwd-strength">
          <div class="pwd-bar" :class="{ weak: pwdStrength >= 1, medium: pwdStrength >= 2, strong: pwdStrength >= 3 }" />
          <div class="pwd-bar" :class="{ medium: pwdStrength >= 2, strong: pwdStrength >= 3 }" />
          <div class="pwd-bar" :class="{ strong: pwdStrength >= 3 }" />
        </div>
        <div v-if="pwdLabel" class="pwd-label" :style="{ color: pwdColor }">{{ pwdLabel }}</div>
        <div v-if="errors.newPassword" class="form-error">{{ errors.newPassword }}</div>
      </div>

      <div class="form-group">
        <label class="form-label">确认密码</label>
        <input
          v-model="form.confirmPassword"
          type="password"
          class="form-input"
          :class="{ error: errors.confirmPassword }"
          placeholder="请再次输入密码"
          @keyup.enter="handleReset"
        />
        <div v-if="errors.confirmPassword" class="form-error">{{ errors.confirmPassword }}</div>
      </div>

      <button class="form-btn form-btn-primary" :disabled="loading" @click="handleReset">
        {{ loading ? '重置中...' : '重置密码' }}
      </button>

      <div class="form-divider">或</div>
      <button class="form-btn form-btn-secondary" @click="router.push('/login')">
        返回登录
      </button>
    </template>

    <template v-else>
      <div class="auth-msg error">{{ message }}</div>
      <button class="form-btn form-btn-secondary" @click="router.push('/forgot-password')">
        重新申请重置链接
      </button>
    </template>
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

.checking-token {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 14px;
  margin-bottom: 24px;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
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

.auth-msg.error {
  background: rgba(255, 59, 48, 0.1);
  color: var(--danger);
  border: 1px solid rgba(255, 59, 48, 0.25);
}

.pwd-strength {
  display: flex;
  gap: 4px;
  margin-top: 6px;
}

.pwd-bar {
  flex: 1;
  height: 3px;
  border-radius: 2px;
  background: var(--border);
  transition: background 0.2s;
}

.pwd-bar.weak {
  background: var(--danger);
}

.pwd-bar.medium {
  background: #ff9500;
}

.pwd-bar.strong {
  background: var(--success);
}

.pwd-label {
  font-size: 11px;
  margin-top: 3px;
  font-weight: 600;
}
</style>
