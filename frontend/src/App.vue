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
        <div class="brand" @click="router.push('/')">
          <span class="brand-icon">◈</span>
          证券分析与投资系统
        </div>
        <div class="nav-right">
          <template v-if="authStore.isLoggedIn">
            <!-- 证券分析 -->
            <el-button link size="small" class="nav-link" @click="router.push('/indexes')">指数信息</el-button>
            <el-button link size="small" class="nav-link" @click="router.push('/industries')">行业信息</el-button>
            <el-button link size="small" class="nav-link" @click="router.push('/companies')">公司信息</el-button>
            <el-button link size="small" class="nav-link" @click="router.push('/research/fundamental')">投研分析</el-button>
            <el-divider direction="vertical" class="nav-divider" />
            <!-- 个人持仓 -->
            <el-button link size="small" class="nav-link" @click="router.push('/portfolios')">持仓管理</el-button>
            <el-divider direction="vertical" class="nav-divider" />
            <!-- 辅助功能 -->
            <el-button v-if="authStore.isAdmin" link size="small" class="nav-link" @click="router.push('/dashboard/collector')">采集监控</el-button>
            <!-- 用户 -->
            <div class="nav-user">
              <span class="user-name">{{ authStore.userInfo?.realName || authStore.userInfo?.username }}</span>
              <el-button size="small" class="logout-btn" @click="handleLogout">退出</el-button>
            </div>
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
  background: var(--bg-primary);
}
.app-header {
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
}
.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}
.brand {
  font-size: 18px;
  font-weight: 700;
  color: var(--accent-primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  letter-spacing: 0.5px;
}
.brand-icon {
  font-size: 20px;
  color: var(--accent-primary);
  text-shadow: 0 0 8px rgba(0, 212, 255, 0.4);
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nav-link {
  color: var(--text-secondary);
  font-size: 14px;
  transition: color 0.2s;
}
.nav-link:hover {
  color: var(--accent-primary);
}
.nav-divider {
  border-color: var(--border-color-strong);
}
.nav-user {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
.user-name {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
}
.admin-btn {
  background: var(--accent-primary-dim);
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}
.admin-btn:hover {
  background: var(--accent-primary);
  color: var(--bg-primary);
}
.logout-btn {
  background: var(--bg-card-solid);
  border-color: var(--border-color);
  color: var(--text-secondary);
}
.logout-btn:hover {
  border-color: var(--up-color);
  color: var(--up-color);
}
.app-main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 24px;
}
</style>
