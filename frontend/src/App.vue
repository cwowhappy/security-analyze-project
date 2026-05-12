<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 主题切换
const isDark = ref(false)
function toggleTheme() {
  isDark.value = !isDark.value
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  try { localStorage.setItem('sai-theme', isDark.value ? 'dark' : 'light') } catch (e) { /* ignore */ }
}
onMounted(() => {
  try {
    const saved = localStorage.getItem('sai-theme')
    if (saved === 'dark') {
      isDark.value = true
      document.documentElement.setAttribute('data-theme', 'dark')
    }
  } catch (e) { /* ignore */ }
})

// 面包屑配置
interface BreadcrumbItem {
  name: string
  path?: string
}
const breadcrumbs = computed<BreadcrumbItem[]>(() => {
  const items: BreadcrumbItem[] = [{ name: '首页概览', path: '/' }]
  const meta = route.meta as Record<string, any>
  if (meta?.parent) {
    items.push({ name: meta.parent.name, path: meta.parent.path })
  }
  if (meta?.title && route.path !== '/') {
    items.push({ name: meta.title })
  }
  return items
})
function navigate(path: string | undefined) {
  if (path) router.push(path)
}

// 导航项
const navItems = [
  { label: '首页概览', path: '/', icon: 'home' },
]
const dataNavItems = [
  { label: '股票列表', path: '/stocks', icon: 'trending' },
  { label: '公司列表', path: '/companies', icon: 'building' },
  { label: '采集任务', path: '/collection/tasks', icon: 'clock' },
]
const placeholderNavItems = [
  { label: '数据分析', icon: 'bar-chart', badge: '待上线' },
  { label: '投资管理', icon: 'briefcase', badge: '待上线' },
  { label: '风险管理', icon: 'shield', badge: '待上线' },
  { label: '分析师团队', icon: 'users', badge: '待上线' },
]

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const isAuthPage = computed(() => {
  return ['/login', '/register', '/forgot-password'].includes(route.path)
})

// 用户菜单
const userMenuVisible = ref(false)
function toggleUserMenu() {
  userMenuVisible.value = !userMenuVisible.value
}
function handleLogout() {
  userMenuVisible.value = false
  authStore.logout()
  router.push('/login')
}

function onDocumentClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  const userInfoEl = document.querySelector('.user-info')
  const userMenuEl = document.querySelector('.user-menu-dropdown')
  if (userInfoEl && !userInfoEl.contains(target) && userMenuEl && !userMenuEl.contains(target)) {
    userMenuVisible.value = false
  }
}
onMounted(() => {
  document.addEventListener('click', onDocumentClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
})
</script>

<template>
  <div v-if="isAuthPage" class="auth-page-wrapper">
    <RouterView />
  </div>
  <div v-else class="app">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sb-brand">
        <div class="sb-logo">SAI</div>
        <div>
          <div class="sb-name">证券分析与投资</div>
          <div class="sb-en">Security Analyze & Invest</div>
        </div>
      </div>
      <nav class="sb-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <svg class="ni-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          {{ item.label }}
        </RouterLink>

        <div class="nav-label">数据管理</div>
        <RouterLink
          v-for="item in dataNavItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <svg v-if="item.icon === 'trending'" class="ni-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
          <svg v-else-if="item.icon === 'building'" class="ni-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 21h18M9 8h1M9 12h1M9 16h1M14 8h1M14 12h1M14 16h1M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16"/>
          </svg>
          <svg v-else-if="item.icon === 'clock'" class="ni-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
          </svg>
          {{ item.label }}
        </RouterLink>

        <div class="nav-divider"></div>
        <div class="nav-label">功能模块</div>
        <button
          v-for="item in placeholderNavItems"
          :key="item.label"
          class="nav-item ph"
          disabled
        >
          <svg v-if="item.icon === 'bar-chart'" class="ni-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>
          </svg>
          <svg v-else-if="item.icon === 'briefcase'" class="ni-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
          </svg>
          <svg v-else-if="item.icon === 'shield'" class="ni-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
          <svg v-else-if="item.icon === 'users'" class="ni-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          {{ item.label }}
          <span class="nav-badge">{{ item.badge }}</span>
        </button>
      </nav>
    </aside>

    <!-- Main Wrapper -->
    <div class="main-wrap">
      <!-- Top Bar -->
      <header class="topbar">
        <div class="breadcrumb">
          <template v-for="(item, index) in breadcrumbs" :key="index">
            <span
              v-if="index < breadcrumbs.length - 1"
              class="bc-item"
              @click="navigate(item.path)"
            >{{ item.name }}</span>
            <span v-else class="bc-item active">{{ item.name }}</span>
            <span v-if="index < breadcrumbs.length - 1" class="bc-sep">›</span>
          </template>
        </div>
        <div class="topbar-right">
          <button class="theme-toggle" @click="toggleTheme" title="切换主题">
            {{ isDark ? '🌙' : '☀' }}
          </button>
          <div class="user-info" @click="toggleUserMenu">
            <div class="avatar">{{ authStore.avatarInitial }}</div>
            <div>
              <div class="username">{{ authStore.displayName }}</div>
              <div class="user-role">{{ authStore.roleLabel }}</div>
            </div>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </div>
          <div v-show="userMenuVisible" class="user-menu-dropdown">
            <button v-if="authStore.user?.role === 'admin'" class="user-menu-item" @click="router.push('/admin/users'); userMenuVisible = false">
              ⚙️ 管理后台
            </button>
            <button class="user-menu-item" @click="handleLogout">
              🚪 退出登录
            </button>
          </div>
        </div>
      </header>

      <!-- Main Content -->
      <main class="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ===== Sidebar ===== */
.sidebar {
  width: var(--sidebar-w);
  background: var(--surface);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.25s, background 0.25s;
}

.sb-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  height: var(--topbar-h);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.sb-logo {
  width: 34px;
  height: 34px;
  background: var(--primary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 12px;
  flex-shrink: 0;
  letter-spacing: 0.5px;
}

.sb-name {
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  color: var(--text-primary);
}

.sb-en {
  font-size: 10px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
}

.sb-nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 10px;
}

.sb-nav::-webkit-scrollbar {
  width: 3px;
}

.sb-nav::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  border: none;
  background: none;
  width: 100%;
  text-align: left;
  font-family: var(--font);
  transition: all 0.15s;
  white-space: nowrap;
  position: relative;
  text-decoration: none;
}

.nav-item:hover {
  background: var(--nav-hover);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--nav-active);
  color: var(--primary);
  font-weight: 600;
}

.nav-item .ni-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  opacity: 0.7;
}

.nav-item.active .ni-icon {
  opacity: 1;
}

.nav-divider {
  height: 1px;
  background: var(--border);
  margin: 10px 12px;
}

.nav-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  padding: 14px 12px 6px;
}

.nav-item.ph {
  opacity: 0.35;
  cursor: not-allowed;
}

.nav-item.ph:hover {
  background: none;
  color: var(--text-secondary);
}

.nav-badge {
  font-size: 10px;
  background: var(--primary);
  color: #fff;
  padding: 1px 6px;
  border-radius: 8px;
  font-weight: 600;
  margin-left: auto;
}

.nav-item.ph .nav-badge {
  display: none;
}

/* ===== Top Bar ===== */
.main-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.topbar {
  height: var(--topbar-h);
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  transition: background 0.25s, border-color 0.25s;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted);
}

.bc-sep {
  color: var(--border);
  font-size: 12px;
}

.bc-item {
  cursor: pointer;
  transition: color 0.15s;
}

.bc-item:hover {
  color: var(--primary);
}

.bc-item.active {
  color: var(--text-primary);
  font-weight: 600;
  cursor: default;
}

.bc-item.active:hover {
  color: var(--text-primary);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.theme-toggle {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: all 0.15s;
  font-size: 16px;
  flex-shrink: 0;
}

.theme-toggle:hover {
  background: var(--surface-hover);
  color: var(--text-primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.15s;
  background: var(--surface);
}

.user-info:hover {
  background: var(--surface-hover);
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.username {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-role {
  font-size: 11px;
  color: var(--text-muted);
}

.user-menu-dropdown {
  position: absolute;
  top: 56px;
  right: 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  z-index: 100;
  overflow: hidden;
  min-width: 140px;
}

.user-menu-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 10px 18px;
  border: none;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  font-size: 13px;
  cursor: pointer;
  color: var(--text-primary);
  font-family: var(--font);
  transition: background 0.1s;
}

.user-menu-item:last-child {
  border-bottom: none;
}

.user-menu-item:hover {
  background: var(--surface-hover);
}

/* ===== Main Content ===== */
.main {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  transition: padding 0.2s;
}

.main::-webkit-scrollbar {
  width: 5px;
}

.main::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 3px;
}

/* ===== Responsive ===== */
@media (max-width: 640px) {
  .sidebar {
    width: 0;
    overflow: hidden;
    border-right: none;
  }
}
</style>
