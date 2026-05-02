import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
    {
      path: '/companies',
      name: 'company-list',
      component: () => import('@/views/company/CompanyListView.vue'),
    },
    {
      path: '/companies/:stockCode',
      name: 'company-detail',
      component: () => import('@/views/company/CompanyDetailView.vue'),
    },
    {
      path: '/dashboard/collector',
      name: 'collector-dashboard',
      component: () => import('@/views/collector/CollectorDashboardView.vue'),
    },
    {
      path: '/industries',
      name: 'industry-list',
      component: () => import('@/views/industry/IndustryListView.vue'),
    },
    {
      path: '/industries/:industryName',
      name: 'industry-detail',
      component: () => import('@/views/industry/IndustryDetailView.vue'),
    },
  ],
})

export default router
