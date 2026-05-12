<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { path: '/admin/users', label: '👥 用户管理', name: 'AdminUsers' },
  { path: '/admin/login-logs', label: '📝 登录日志', name: 'AdminLoginLogs' },
]

function isActive(path: string) {
  return route.path.startsWith(path)
}

function goBack() {
  router.push('/')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <div class="logo" @click="goBack">SAI</div>
        <div class="subtitle">管理后台</div>
      </div>
      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          {{ item.label }}
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <button class="back-btn" @click="goBack">← 返回前台</button>
      </div>
    </aside>
    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

.admin-sidebar {
  width: 220px;
  background: var(--surface);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--border);
}

.logo {
  font-size: 22px;
  font-weight: 800;
  color: var(--primary);
  cursor: pointer;
}

.subtitle {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  padding: 10px 12px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 14px;
  text-decoration: none;
  transition: all 0.15s;
}

.nav-item:hover {
  background: var(--surface-hover);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--primary);
  color: #fff;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--border);
}

.back-btn {
  width: 100%;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.back-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.admin-main {
  flex: 1;
  margin-left: 220px;
  padding: 24px;
  min-height: 100vh;
}

@media (max-width: 768px) {
  .admin-sidebar {
    display: none;
  }
  .admin-main {
    margin-left: 0;
  }
}
</style>
