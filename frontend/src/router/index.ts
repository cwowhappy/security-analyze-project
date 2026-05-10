import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' },
  },
  {
    path: '/stocks',
    name: 'StockList',
    component: () => import('@/views/stock/StockListView.vue'),
    meta: { title: '股票信息' },
  },
  {
    path: '/stocks/:stockCode',
    name: 'StockDetail',
    component: () => import('@/views/stock/StockDetailView.vue'),
    props: true,
    meta: { title: '股票详情', parent: { name: '股票信息', path: '/stocks' } },
  },
  {
    path: '/companies',
    name: 'CompanyList',
    component: () => import('@/views/company/CompanyListView.vue'),
    meta: { title: '公司信息' },
  },
  {
    path: '/companies/:uscCode',
    name: 'CompanyDetail',
    component: () => import('@/views/company/CompanyDetailView.vue'),
    props: true,
    meta: { title: '公司详情', parent: { name: '公司信息', path: '/companies' } },
  },
  {
    path: '/collection/tasks',
    name: 'CollectionTaskList',
    component: () => import('@/views/collection/CollectionTaskListView.vue'),
    meta: { title: '采集任务' },
  },
  {
    path: '/collection/schedules',
    name: 'CollectionScheduleList',
    component: () => import('@/views/collection/CollectionScheduleListView.vue'),
    meta: { title: '定时规则' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
