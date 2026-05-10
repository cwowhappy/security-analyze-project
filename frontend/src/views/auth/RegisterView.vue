<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
})

async function handleRegister() {
  if (!form.username || !form.password || !form.confirmPassword || !form.realName) {
    ElMessage.warning('请填写所有必填项')
    return
  }

  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      confirmPassword: form.confirmPassword,
      realName: form.realName,
    })
    ElMessage.success('注册成功，请等待管理员审批')
    router.push('/login')
  } catch (error: any) {
    const msg = error.response?.data?.message || '注册失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-container">
    <div class="register-bg" aria-hidden="true" />
    <div class="register-overlay" aria-hidden="true" />
    <el-card class="register-card" shadow="always">
      <h2 class="title">用户注册</h2>
      <el-form :model="form" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password clearable />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
        <div class="links">
          <el-link type="primary" @click="router.push('/login')">已有账号？去登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.register-container {
  position: fixed;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--bg-primary);
  overflow: hidden;
  z-index: 1;
}

.register-bg {
  position: absolute;
  inset: 0;
  background:
    url('/images/login-bg.png') center center / auto 100% no-repeat;
  opacity: 0.55;
}

.register-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(13, 15, 20, 0.88) 0%,
    rgba(13, 15, 20, 0.65) 50%,
    rgba(13, 15, 20, 0.88) 100%
  );
}

.register-card {
  position: relative;
  z-index: 2;
  width: 420px;
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  --el-card-bg-color: transparent;
  --el-card-border-color: transparent;
}
.title {
  text-align: center;
  margin-bottom: 24px;
  color: var(--text-primary);
}
.links {
  text-align: center;
  margin-top: 8px;
}
</style>
