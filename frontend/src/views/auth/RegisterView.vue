<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  role: 'portfolio_manager',
  agree: false,
})

const errors = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agree: '',
  verifyCode: '',
})

const message = ref('')
const messageType = ref<'success' | 'error'>('error')
const loading = ref(false)

// 唯一性检查状态
const checkingUsername = ref(false)
const checkingEmail = ref(false)

// 注册成功后的邮箱验证状态
const registeredUserId = ref<string | null>(null)
const registeredEmail = ref('')
const verifyCode = ref('')
const verifyLoading = ref(false)
const resendCooldown = ref(0)

const pwdStrength = ref(0)
const pwdLabel = ref('')
const pwdColor = ref('')

function validateUsername(): boolean {
  if (!form.username.trim()) {
    errors.username = '请输入用户名'
    return false
  }
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(form.username.trim())) {
    errors.username = '3-20个字符，仅字母数字下划线'
    return false
  }
  errors.username = ''
  return true
}

// 用户名唯一性检查（blur 时触发）
async function handleUsernameBlur() {
  if (!form.username.trim() || !/^[a-zA-Z0-9_]{3,20}$/.test(form.username.trim())) {
    return
  }
  checkingUsername.value = true
  try {
    const available = await authStore.checkUsername(form.username.trim())
    if (!available) {
      errors.username = '该用户名已被注册'
    }
  } catch {
    // 忽略错误，不影响用户继续输入
  } finally {
    checkingUsername.value = false
  }
}

function validateEmail(): boolean {
  if (!form.email.trim()) {
    errors.email = '请输入邮箱'
    return false
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    errors.email = '请输入有效的邮箱地址'
    return false
  }
  errors.email = ''
  return true
}

// 邮箱唯一性检查（blur 时触发）
async function handleEmailBlur() {
  if (!form.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    return
  }
  checkingEmail.value = true
  try {
    const available = await authStore.checkEmail(form.email.trim())
    if (!available) {
      errors.email = '该邮箱已被注册'
    }
  } catch {
    // 忽略错误，不影响用户继续输入
  } finally {
    checkingEmail.value = false
  }
}

function validatePassword(): boolean {
  if (!form.password) {
    errors.password = '请输入密码'
    return false
  }
  if (form.password.length < 8) {
    errors.password = '密码至少8位'
    return false
  }
  if (!/[a-zA-Z]/.test(form.password) || !/\d/.test(form.password)) {
    errors.password = '需同时包含字母和数字'
    return false
  }
  errors.password = ''
  return true
}

function validateConfirmPassword(): boolean {
  if (!form.confirmPassword) {
    errors.confirmPassword = '请再次输入密码'
    return false
  }
  if (form.password !== form.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
    return false
  }
  errors.confirmPassword = ''
  return true
}

function validateAgree(): boolean {
  if (!form.agree) {
    errors.agree = '请同意用户协议'
    return false
  }
  errors.agree = ''
  return true
}

function updatePwdStrength() {
  const p = form.password
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

watch(() => form.password, updatePwdStrength)

function validateAll(): boolean {
  const results = [
    validateUsername(),
    validateEmail(),
    validatePassword(),
    validateConfirmPassword(),
    validateAgree(),
  ]
  return results.every(Boolean)
}

async function handleRegister() {
  message.value = ''
  if (!validateAll()) return

  loading.value = true
  try {
    const user = await authStore.register({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      role: form.role,
    })
    registeredUserId.value = user.id
    registeredEmail.value = user.email
    message.value = '注册成功，请验证您的邮箱'
    messageType.value = 'success'
  } catch (err: any) {
    message.value = err.message || '注册失败'
    messageType.value = 'error'
  } finally {
    loading.value = false
  }
}

async function handleVerifyEmail() {
  errors.verifyCode = ''
  if (!verifyCode.value.trim()) {
    errors.verifyCode = '请输入验证码'
    return
  }
  if (!registeredUserId.value) return

  verifyLoading.value = true
  try {
    await authStore.verifyEmail(registeredUserId.value, verifyCode.value.trim())
    message.value = '邮箱验证成功，即将跳转到登录页'
    messageType.value = 'success'
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (err: any) {
    errors.verifyCode = err.message || '验证失败'
  } finally {
    verifyLoading.value = false
  }
}

async function handleResend() {
  if (!registeredUserId.value || resendCooldown.value > 0) return
  try {
    await authStore.resendVerification(registeredUserId.value)
    message.value = '验证码已重新发送'
    messageType.value = 'success'
    resendCooldown.value = 60
    const timer = setInterval(() => {
      resendCooldown.value--
      if (resendCooldown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (err: any) {
    message.value = err.message || '发送失败'
    messageType.value = 'error'
  }
}
</script>

<template>
  <div class="auth-card">
    <div v-if="!registeredUserId">
      <!-- 注册表单 -->
      <div class="auth-title">注册账户</div>
      <div class="auth-sub">填写以下信息创建您的新账户</div>

      <div v-if="message" class="auth-msg" :class="messageType">{{ message }}</div>

      <div class="form-group">
        <label class="form-label">用户名</label>
        <div class="input-with-status">
          <input
            v-model="form.username"
            type="text"
            class="form-input"
            :class="{ error: errors.username }"
            placeholder="3-20个字符，仅字母数字下划线"
            @blur="handleUsernameBlur"
          />
          <span v-if="checkingUsername" class="input-status loading">检查中...</span>
        </div>
        <div v-if="errors.username" class="form-error">{{ errors.username }}</div>
      </div>

      <div class="form-group">
        <label class="form-label">邮箱</label>
        <div class="input-with-status">
          <input
            v-model="form.email"
            type="email"
            class="form-input"
            :class="{ error: errors.email }"
            placeholder="请输入邮箱地址"
            @blur="handleEmailBlur"
          />
          <span v-if="checkingEmail" class="input-status loading">检查中...</span>
        </div>
        <div v-if="errors.email" class="form-error">{{ errors.email }}</div>
      </div>

      <div class="form-group">
        <label class="form-label">密码</label>
        <input
          v-model="form.password"
          type="password"
          class="form-input"
          :class="{ error: errors.password }"
          placeholder="至少8位，含字母+数字"
        />
        <div class="pwd-strength">
          <div class="pwd-bar" :class="{ weak: pwdStrength >= 1, medium: pwdStrength >= 2, strong: pwdStrength >= 3 }" />
          <div class="pwd-bar" :class="{ medium: pwdStrength >= 2, strong: pwdStrength >= 3 }" />
          <div class="pwd-bar" :class="{ strong: pwdStrength >= 3 }" />
        </div>
        <div v-if="pwdLabel" class="pwd-label" :style="{ color: pwdColor }">{{ pwdLabel }}</div>
        <div v-if="errors.password" class="form-error">{{ errors.password }}</div>
      </div>

      <div class="form-group">
        <label class="form-label">确认密码</label>
        <input
          v-model="form.confirmPassword"
          type="password"
          class="form-input"
          :class="{ error: errors.confirmPassword }"
          placeholder="请再次输入密码"
        />
        <div v-if="errors.confirmPassword" class="form-error">{{ errors.confirmPassword }}</div>
      </div>

      <div class="form-group">
        <label class="form-label">用户角色</label>
        <select v-model="form.role" class="form-input" style="cursor: pointer">
          <option value="portfolio_manager">投资组合经理</option>
          <option value="analyst">分析师</option>
          <option value="viewer">观察者</option>
        </select>
      </div>

      <div class="form-row">
        <label class="form-check">
          <input v-model="form.agree" type="checkbox" /> 我已阅读并同意<strong>用户协议</strong>
        </label>
      </div>
      <div v-if="errors.agree" class="form-error" style="margin-bottom: 12px">{{ errors.agree }}</div>

      <button class="form-btn form-btn-primary" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注册账户' }}
      </button>

      <div class="form-divider">或</div>
      <button class="form-btn form-btn-secondary" @click="router.push('/login')">
        返回登录
      </button>
    </div>

    <div v-else>
      <!-- 邮箱验证 -->
      <div class="auth-title">验证邮箱</div>
      <div class="auth-sub">验证码已发送至 {{ registeredEmail }}，请输入 6 位验证码</div>

      <div v-if="message" class="auth-msg" :class="messageType">{{ message }}</div>

      <div class="form-group">
        <label class="form-label">验证码</label>
        <input
          v-model="verifyCode"
          type="text"
          class="form-input"
          :class="{ error: errors.verifyCode }"
          placeholder="请输入 6 位验证码"
          maxlength="6"
        />
        <div v-if="errors.verifyCode" class="form-error">{{ errors.verifyCode }}</div>
      </div>

      <button class="form-btn form-btn-primary" :disabled="verifyLoading" @click="handleVerifyEmail">
        {{ verifyLoading ? '验证中...' : '验证邮箱' }}
      </button>

      <div class="form-row" style="justify-content: center">
        <button
          class="form-link"
          :disabled="resendCooldown > 0"
          @click="handleResend"
        >
          {{ resendCooldown > 0 ? `${resendCooldown} 秒后重新发送` : '重新发送验证码' }}
        </button>
      </div>

      <div class="form-divider">或</div>
      <button class="form-btn form-btn-secondary" @click="router.push('/login')">
        返回登录
      </button>
    </div>
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

.form-link:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  text-decoration: none;
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

.input-with-status {
  position: relative;
}

.input-status {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: var(--text-muted);
}

.input-status.loading::before {
  content: '';
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-right: 4px;
  vertical-align: middle;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
