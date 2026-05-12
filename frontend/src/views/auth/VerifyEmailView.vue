<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import VerifyStatus from '@/components/auth/VerifyStatus.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const status = ref<'loading' | 'success' | 'error' | 'invalid'>('loading')
const email = ref('')
const userId = ref('')
const resendCooldown = ref(0)

onMounted(() => {
  const queryUserId = route.query.userId as string
  const queryCode = route.query.code as string

  if (!queryUserId || !queryCode) {
    status.value = 'invalid'
    return
  }

  userId.value = queryUserId
  doVerify(queryUserId, queryCode)
})

async function doVerify(uid: string, code: string) {
  status.value = 'loading'
  try {
    await authStore.verifyEmail(uid, code)
    status.value = 'success'
    // 3秒后自动跳转到登录页
    setTimeout(() => {
      router.push('/login')
    }, 3000)
  } catch (err: any) {
    status.value = 'error'
  }
}

async function handleResend() {
  if (!userId.value || resendCooldown.value > 0) return
  try {
    await authStore.resendVerification(userId.value)
    resendCooldown.value = 60
    const timer = setInterval(() => {
      resendCooldown.value--
      if (resendCooldown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (err: any) {
    // 忽略错误
  }
}

function goToLogin() {
  router.push('/login')
}

function goToRegister() {
  router.push('/register')
}
</script>

<template>
  <div class="auth-card">
    <VerifyStatus :status="status" :email="email" />

    <div class="verify-actions">
      <!-- 成功态：前往登录 -->
      <button v-if="status === 'success'" class="form-btn form-btn-primary" @click="goToLogin">
        前往登录
      </button>

      <!-- 失败态：重新发送 + 返回注册 -->
      <template v-else-if="status === 'error'">
        <button
          class="form-btn form-btn-primary"
          :disabled="resendCooldown > 0"
          @click="handleResend"
        >
          {{ resendCooldown > 0 ? `${resendCooldown} 秒后重新发送` : '重新发送验证码' }}
        </button>
        <button class="form-btn form-btn-secondary" @click="goToRegister">
          返回注册
        </button>
      </template>

      <!-- 无效链接态：返回注册 -->
      <button v-else-if="status === 'invalid'" class="form-btn form-btn-primary" @click="goToRegister">
        返回注册
      </button>
    </div>

    <div v-if="status === 'error' || status === 'invalid'" class="verify-tips">
      <p>没有收到邮件？</p>
      <ul>
        <li>检查垃圾邮件文件夹</li>
        <li>确认邮箱地址是否正确</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.auth-card {
  width: 100%;
  max-width: 340px;
}

.verify-actions {
  margin-top: 8px;
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

.verify-tips {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-muted);
}

.verify-tips p {
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--text-secondary);
}

.verify-tips ul {
  padding-left: 16px;
  margin: 0;
}

.verify-tips li {
  margin-bottom: 4px;
}
</style>
