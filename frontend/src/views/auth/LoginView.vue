<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  rememberMe: false,
})

const errors = reactive({
  username: '',
  password: '',
})

const message = ref('')
const messageType = ref<'success' | 'error'>('error')
const loading = ref(false)
const lockMinutes = ref(0)
const needsVerify = ref(false)
const verifyUserId = ref('')

function validate(): boolean {
  let ok = true
  if (!form.username.trim()) {
    errors.username = '请输入用户名或邮箱'
    ok = false
  } else if (form.username.trim().length < 3) {
    errors.username = '至少 3 个字符'
    ok = false
  } else {
    errors.username = ''
  }

  if (!form.password) {
    errors.password = '请输入密码'
    ok = false
  } else if (form.password.length < 6) {
    errors.password = '密码至少 6 位'
    ok = false
  } else {
    errors.password = ''
  }

  return ok
}

async function handleLogin() {
  message.value = ''
  lockMinutes.value = 0
  needsVerify.value = false
  verifyUserId.value = ''
  if (!validate()) return

  loading.value = true
  try {
    await authStore.login({
      username: form.username.trim(),
      password: form.password,
      rememberMe: form.rememberMe,
    })
    router.push('/')
  } catch (err: any) {
    const msg = err.message || '用户名或密码错误'
    message.value = msg
    messageType.value = 'error'

    // 解析锁定剩余时间
    const lockMatch = msg.match(/请\s*(\d+)\s*分钟后重试/)
    if (lockMatch) {
      lockMinutes.value = parseInt(lockMatch[1], 10)
    }

    // 解析需要验证邮箱的情况
    if (msg.includes('请先验证邮箱')) {
      needsVerify.value = true
    }
  } finally {
    loading.value = false
  }
}

function handleKeyup(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    handleLogin()
  }
}

function goToVerify() {
  router.push('/register')
}
</script>

<template>
  <div class="auth-card">
    <div class="auth-title">登录账户</div>
    <div class="auth-sub">请输入您的用户名或邮箱以登录系统</div>

    <div v-if="message" class="auth-msg" :class="messageType">
      {{ message }}
      <div v-if="lockMinutes > 0" class="lock-hint">
        账户锁定中，请 {{ lockMinutes }} 分钟后重试
      </div>
      <div v-if="needsVerify" class="verify-hint">
        <button class="form-link" @click="goToVerify">前往注册页验证邮箱 →</button>
      </div>
    </div>

    <div class="form-group">
      <label class="form-label">用户名或邮箱</label>
      <input
        v-model="form.username"
        type="text"
        class="form-input"
        :class="{ error: errors.username }"
        placeholder="请输入用户名或邮箱"
        @keyup="handleKeyup"
      />
      <div v-if="errors.username" class="form-error">{{ errors.username }}</div>
    </div>

    <div class="form-group">
      <label class="form-label">密码</label>
      <input
        v-model="form.password"
        type="password"
        class="form-input"
        :class="{ error: errors.password }"
        placeholder="请输入密码"
        @keyup="handleKeyup"
      />
      <div v-if="errors.password" class="form-error">{{ errors.password }}</div>
    </div>

    <div class="form-row">
      <label class="form-check">
        <input v-model="form.rememberMe" type="checkbox" /> 记住我
      </label>
      <RouterLink to="/forgot-password" class="form-link">忘记密码？</RouterLink>
    </div>

    <button class="form-btn form-btn-primary" :disabled="loading" @click="handleLogin">
      {{ loading ? '登录中...' : '登 录' }}
    </button>

    <div class="form-divider">或</div>
    <button class="form-btn form-btn-secondary" @click="router.push('/register')">
      注册新账户
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

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.form-check {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
}

.form-check input {
  accent-color: var(--primary);
  width: 15px;
  height: 15px;
}

.form-link {
  font-size: 13px;
  color: var(--primary);
  text-decoration: none;
  cursor: pointer;
  background: none;
  border: none;
  font-family: var(--font);
  padding: 0;
}

.form-link:hover {
  text-decoration: underline;
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

.lock-hint {
  margin-top: 6px;
  font-size: 12px;
  opacity: 0.9;
}

.verify-hint {
  margin-top: 8px;
}
</style>
