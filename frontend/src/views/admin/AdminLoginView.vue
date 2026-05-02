<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from '@/api/auth'
import { adminLogin, registerAdmin } from '@/api/adminAuth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const showRegister = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  password: '',
  realName: '',
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }

  loading.value = true
  try {
    const authRes = await adminLogin({ username: form.username, password: form.password })
    authStore.setToken(authRes.token)

    const user = await getCurrentUser()
    authStore.setUserInfo(user)

    ElMessage.success('管理员登录成功')
    router.push('/admin/users')
  } catch (error: any) {
    const msg = error.response?.data?.message || '登录失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

async function handleRegisterAdmin() {
  if (!registerForm.username || !registerForm.password || !registerForm.realName) {
    ElMessage.warning('请填写所有必填项')
    return
  }

  loading.value = true
  try {
    await registerAdmin({
      username: registerForm.username,
      password: registerForm.password,
      realName: registerForm.realName,
    })
    ElMessage.success('管理员账号创建成功')
    showRegister.value = false
    registerForm.username = ''
    registerForm.password = ''
    registerForm.realName = ''
  } catch (error: any) {
    const msg = error.response?.data?.message || '创建失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <el-card class="login-card" shadow="always">
      <h2 class="title">管理后台登录</h2>
      <el-form v-if="!showRegister" :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入管理员用户名" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
        <div class="links">
          <el-link type="primary" @click="showRegister = true">创建新管理员</el-link>
        </div>
      </el-form>

      <el-form v-else :model="registerForm" label-position="top" @submit.prevent="handleRegisterAdmin">
        <h3 style="text-align: center; margin-bottom: 16px;">创建管理员账号</h3>
        <el-form-item label="用户名">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleRegisterAdmin">
            创建
          </el-button>
        </el-form-item>
        <div class="links">
          <el-link type="primary" @click="showRegister = false">返回登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}
.login-card {
  width: 400px;
}
.title {
  text-align: center;
  margin-bottom: 24px;
  color: #303133;
}
.links {
  text-align: center;
  margin-top: 8px;
}
</style>
