<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, TrendCharts } from '@element-plus/icons-vue'
import { login, getCurrentUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  remember: false,
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '长度在 3 到 32 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '长度在 6 到 64 个字符', trigger: 'blur' },
  ],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const authRes = await login({ username: form.username, password: form.password })
    authStore.setToken(authRes.token)

    const user = await getCurrentUser()
    authStore.setUserInfo(user)

    if (form.remember) {
      localStorage.setItem('login_remember_user', form.username)
    } else {
      localStorage.removeItem('login_remember_user')
    }

    ElMessage.success('登录成功')
    router.push('/')
  } catch (error: any) {
    let msg = '登录失败'
    if (error.response) {
      msg = error.response.data?.message || `登录失败（${error.response.status}）`
    } else if (error.request) {
      msg = '网络异常，请检查网络连接'
    }
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

function handleKeyEnter(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    handleLogin()
  }
}

onMounted(() => {
  const remembered = localStorage.getItem('login_remember_user')
  if (remembered) {
    form.username = remembered
    form.remember = true
  }
})
</script>

<template>
  <div class="login-page">
    <!-- 左侧：品牌展示区 -->
    <div class="login-left">
      <img src="/images/login-bg.png" class="bg-image" alt="background" />
      <div class="left-overlay">
        <div class="left-brand">
          <el-icon class="brand-icon" :size="48"><TrendCharts /></el-icon>
          <h1 class="left-brand-title">证券分析与投资系统</h1>
          <p class="left-brand-desc">专业 · 智能 · 高效的 A 股投研分析平台</p>
        </div>
        <div class="left-footer">
          <p>数据驱动决策，科技赋能投资</p>
        </div>
      </div>
    </div>

    <!-- 右侧：登录表单区 -->
    <div class="login-right">
      <div class="login-content">
        <div class="brand-section">
          <h1 class="brand-title">证券分析与投资系统</h1>
          <p class="brand-subtitle">SECURITY ANALYZE</p>
        </div>

        <div class="login-form-section">
          <h2 class="form-title">用户登录</h2>
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            @submit.prevent="handleLogin"
            @keyup.enter="handleKeyEnter"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                clearable
                size="large"
                :prefix-icon="User"
                aria-label="用户名"
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                show-password
                clearable
                size="large"
                :prefix-icon="Lock"
                aria-label="密码"
              />
            </el-form-item>
            <el-form-item class="remember-row">
              <el-checkbox v-model="form.remember" class="remember-checkbox">
                记住用户名
              </el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                style="width: 100%"
                :loading="loading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
            <div class="form-links">
              <el-link type="primary" :underline="false" @click="router.push('/register')">
                还没有账号？去注册
              </el-link>
            </div>
          </el-form>
        </div>

        <div class="login-footer">
          <p> 证券分析与投资系统 · 内部使用</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  z-index: 1;
  min-width: 900px;
}

/* ============================
   左侧：品牌展示区
   ============================ */
.login-left {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #0a1628;
}

.bg-image {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: auto;
  display: block;
  opacity: 0.55;
}

.left-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 60px 56px 40px;
  background: linear-gradient(
    135deg,
    rgba(10, 22, 40, 0.85) 0%,
    rgba(10, 22, 40, 0.4) 60%,
    rgba(10, 22, 40, 0.7) 100%
  );
}

.left-brand {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.brand-icon {
  color: var(--accent-primary);
  filter: drop-shadow(0 0 8px rgba(0, 212, 255, 0.4));
}

.left-brand-title {
  font-size: 36px;
  font-weight: 800;
  color: #ffffff;
  margin: 0;
  letter-spacing: 6px;
  line-height: 1.2;
}

.left-brand-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.55);
  margin: 0;
  letter-spacing: 2px;
}

.left-footer {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 1px;
}

/* ============================
   右侧：登录表单区
   ============================ */
.login-right {
  width: 480px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #0a1628 0%, #0d1d33 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 56px;
  position: relative;
  z-index: 2;
}

.login-right::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(0, 212, 255, 0.2) 30%,
    rgba(0, 212, 255, 0.2) 70%,
    transparent 100%
  );
}

.login-content {
  width: 100%;
  max-width: 368px;
  display: flex;
  flex-direction: column;
  gap: 40px;
}

/* 品牌区域 */
.brand-section {
  text-align: center;
}

.brand-title {
  font-size: 32px;
  font-weight: 800;
  color: #ffffff;
  margin: 0 0 10px 0;
  letter-spacing: 4px;
  line-height: 1.2;
}

.brand-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  margin: 0;
  letter-spacing: 6px;
}

/* 表单区域 */
.login-form-section {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 32px;
  backdrop-filter: blur(12px);
}

.form-title {
  font-size: 20px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 24px 0;
  text-align: center;
}

.remember-row {
  margin-bottom: 8px;
  margin-top: -4px;
}

.remember-row :deep(.el-form-item__content) {
  line-height: 1;
}

.remember-checkbox {
  --el-checkbox-text-color: rgba(255, 255, 255, 0.5);
  --el-checkbox-font-size: 13px;
}

.remember-checkbox :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: rgba(255, 255, 255, 0.7);
}

.remember-checkbox :deep(.el-checkbox__inner) {
  background-color: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

.form-links {
  text-align: center;
  margin-top: 4px;
}

/* 底部版权 */
.login-footer {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.25);
  letter-spacing: 0.5px;
}

/* ============================
   Element Plus 深度覆盖
   ============================ */
:deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.6) !important;
  font-size: 13px;
  padding-bottom: 6px;
}

:deep(.el-form-item.is-required .el-form-item__label::before) {
  color: var(--accent-primary) !important;
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: none !important;
  border-radius: 8px;
}

:deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 212, 255, 0.4) !important;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #00d4ff !important;
  box-shadow: 0 0 0 2px rgba(0, 212, 255, 0.15) !important;
}

:deep(.el-input__inner) {
  color: #ffffff !important;
  font-size: 14px;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3) !important;
}

:deep(.el-input__prefix-inner .el-icon) {
  color: rgba(255, 255, 255, 0.4) !important;
}

:deep(.el-input__suffix-inner .el-icon) {
  color: rgba(255, 255, 255, 0.4) !important;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #00a8e8 0%, #0077b6 100%);
  border: none;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  height: 46px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #00c3ff 0%, #0096c7 100%);
  box-shadow: 0 4px 24px rgba(0, 168, 232, 0.35);
  transform: translateY(-1px);
}

:deep(.el-link--primary) {
  color: #00d4ff !important;
  font-size: 13px;
}

:deep(.el-link--primary:hover) {
  color: #66e5ff !important;
}

:deep(.el-form-item__error) {
  color: #ff6b6b;
  font-size: 12px;
  padding-top: 4px;
}
</style>
