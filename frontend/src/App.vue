<script setup lang="ts">
import { RouterView, useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { computed } from 'vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const showNav = computed(() => {
  return !['/login', '/register', '/admin/login'].includes(route.path)
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-wrapper">
    <el-header v-if="showNav" class="app-header">
      <div class="header-content">
        <div class="brand" @click="router.push('/')">证券分析系统</div>
        <div class="nav-right">
          <template v-if="authStore.isLoggedIn">
            <span class="user-name">{{ authStore.userInfo?.realName || authStore.userInfo?.username }}</span>
            <el-button v-if="authStore.isAdmin" type="primary" size="small" @click="router.push('/admin/users')">
              用户管理
            </el-button>
            <el-button size="small" @click="handleLogout">退出</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-wrapper {
  min-height: 100vh;
}
.app-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
}
.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}
.brand {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  cursor: pointer;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name {
  color: #606266;
  font-size: 14px;
}
.app-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}
</style>
