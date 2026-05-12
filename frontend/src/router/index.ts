import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'LoginForm',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { public: true, title: '登录' },
      },
    ],
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'RegisterForm',
        component: () => import('@/views/auth/RegisterView.vue'),
        meta: { public: true, title: '注册' },
      },
    ],
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'ForgotPasswordForm',
        component: () => import('@/views/auth/ForgotPasswordView.vue'),
        meta: { public: true, title: '忘记密码' },
      },
    ],
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/auth/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'ResetPasswordForm',
        component: () => import('@/views/auth/ResetPasswordView.vue'),
        meta: { public: true, title: '重置密码' },
      },
    ],
  },
  {
    path: '/verify-email',
    name: 'VerifyEmail',
    component: () => import('@/views/auth/AuthLayout.vue'),
    children: [
      {
        path: '',
        name: 'VerifyEmailForm',
        component: () => import('@/views/auth/VerifyEmailView.vue'),
        meta: { public: true, title: '验证邮箱' },
      },
    ],
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页概览' },
  },
  {
    path: '/stocks',
    name: 'StockList',
    component: () => import('@/views/stock/StockListView.vue'),
    meta: { title: '股票列表' },
  },
  {
    path: '/stocks/:stockCode',
    name: 'StockDetail',
    component: () => import('@/views/stock/StockDetailView.vue'),
    props: true,
    meta: { title: '股票详情', parent: { name: '股票列表', path: '/stocks' } },
  },
  {
    path: '/companies',
    name: 'CompanyList',
    component: () => import('@/views/company/CompanyListView.vue'),
    meta: { title: '公司列表' },
  },
  {
    path: '/companies/:uscCode',
    name: 'CompanyDetail',
    component: () => import('@/views/company/CompanyDetailView.vue'),
    props: true,
    meta: { title: '公司详情', parent: { name: '公司列表', path: '/companies' } },
  },
  {
    path: '/collection/tasks',
    name: 'CollectionTaskList',
    component: () => import('@/views/collection/CollectionTaskListView.vue'),
    meta: { title: '采集任务' },
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/users/UserListView.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'users/:userId',
        name: 'AdminUserDetail',
        component: () => import('@/views/admin/users/UserDetailView.vue'),
        meta: { title: '用户详情' },
      },
      {
        path: 'login-logs',
        name: 'AdminLoginLogs',
        component: () => import('@/views/admin/logs/LoginLogsView.vue'),
        meta: { title: '登录日志' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const isPublic = to.matched.some((record) => record.meta.public)
  const requiresAdmin = to.matched.some((record) => record.meta.requiresAdmin)

  if (!authStore.isLoggedIn && !isPublic) {
    next('/login')
    return
  }

  if (authStore.isLoggedIn && (to.path === '/login' || to.path === '/register')) {
    next('/')
    return
  }

  if (requiresAdmin && authStore.user?.role !== 'admin') {
    next('/')
    return
  }

  next()
})

export default router
