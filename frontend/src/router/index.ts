import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/AdminLoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/companies',
      name: 'company-list',
      component: () => import('@/views/company/CompanyListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/companies/:stockCode',
      name: 'company-detail',
      component: () => import('@/views/company/CompanyDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/dashboard/collector',
      name: 'collector-dashboard',
      component: () => import('@/views/collector/CollectorDashboardView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/industries',
      name: 'industry-list',
      component: () => import('@/views/industry/IndustryListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/industries/:industryCode',
      name: 'industry-detail',
      component: () => import('@/views/industry/IndustryDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('@/views/admin/UserManagementView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
  ],
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  authStore.loadUserInfo()

  const isLoggedIn = authStore.isLoggedIn
  const isAdmin = authStore.isAdmin

  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
    return
  }

  if (to.meta.requiresAdmin && !isAdmin) {
    next('/')
    return
  }

  if (to.meta.guestOnly && isLoggedIn) {
    next('/')
    return
  }

  next()
})

export default router
