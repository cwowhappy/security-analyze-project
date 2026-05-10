<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

interface BreadcrumbItem {
  name: string
  path?: string
}

const breadcrumbs = computed<BreadcrumbItem[]>(() => {
  const items: BreadcrumbItem[] = [{ name: '首页', path: '/' }]

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
</script>

<template>
  <div class="app">
    <!-- 页眉 -->
    <header class="app-header">
      <div class="header-inner">
        <div class="header-brand" @click="router.push('/')">
          <span class="brand-icon">◈</span>
          <span class="brand-text">证券分析系统</span>
        </div>
        <div class="header-user">
          <span class="user-avatar">👤</span>
          <span class="user-name">登录用户（待完善）</span>
        </div>
      </div>
    </header>

    <!-- 面包屑 -->
    <div class="breadcrumb-bar">
      <div class="breadcrumb-inner">
        <span
          v-for="(item, index) in breadcrumbs"
          :key="index"
          class="breadcrumb-item"
        >
          <span
            v-if="item.path && index < breadcrumbs.length - 1"
            class="breadcrumb-link"
            @click="navigate(item.path)"
          >{{ item.name }}</span>
          <span v-else class="breadcrumb-text">{{ item.name }}</span>
          <span v-if="index < breadcrumbs.length - 1" class="breadcrumb-sep">/</span>
        </span>
      </div>
    </div>

    <!-- 页面内容 -->
    <main class="page-content">
      <div class="content-inner">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style>
/* ---- 页眉 ------------------------------------------------------------ */
.app-header {
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-default);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1320px;
  margin: 0 auto;
  padding: 0 32px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: opacity var(--transition-fast);
}

.header-brand:hover {
  opacity: 0.8;
}

.brand-icon {
  color: var(--primary);
  font-size: 18px;
}

.brand-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 14px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-default);
}

.user-avatar {
  font-size: 14px;
}

.user-name {
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---- 面包屑 ---------------------------------------------------------- */
.breadcrumb-bar {
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-default);
}

.breadcrumb-inner {
  max-width: 1320px;
  margin: 0 auto;
  padding: 8px 32px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
}

.breadcrumb-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.breadcrumb-link {
  color: var(--text-secondary);
  cursor: pointer;
  transition: color var(--transition-fast);
}

.breadcrumb-link:hover {
  color: var(--primary);
}

.breadcrumb-text {
  color: var(--text-primary);
  font-weight: 500;
}

.breadcrumb-sep {
  color: var(--text-muted);
  font-weight: 300;
}

/* ---- 页面内容区 ------------------------------------------------------ */
.page-content {
  background: var(--bg-base);
  min-height: calc(100vh - 52px - 34px);
  padding: 20px 32px 40px;
}

.content-inner {
  max-width: 1320px;
  margin: 0 auto;
}

/* ---- 卡片 ------------------------------------------------------------ */
.card {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 20px 24px;
  margin-bottom: 16px;
  transition: border-color var(--transition-fast);
}

.card:hover {
  border-color: var(--border-strong);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  letter-spacing: 0.3px;
}

/* ---- 按钮 ------------------------------------------------------------ */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 7px 16px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
  letter-spacing: 0.2px;
}

.btn-primary {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.btn-primary:hover {
  background: var(--primary-dim);
  border-color: var(--primary-dim);
}

.btn-success {
  background: var(--green);
  color: #fff;
  border-color: var(--green);
}

.btn-success:hover {
  background: #219a52;
  border-color: #219a52;
}

.btn-danger {
  background: var(--red);
  color: #fff;
  border-color: var(--red);
}

.btn-danger:hover {
  background: #c0392b;
  border-color: #c0392b;
}

.btn-default {
  background: transparent;
  color: var(--text-secondary);
  border-color: var(--border-default);
}

.btn-default:hover {
  color: var(--text-primary);
  border-color: var(--border-strong);
  background: rgba(255, 255, 255, 0.03);
}

.btn-sm {
  padding: 4px 12px;
  font-size: 12.5px;
}

.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ---- 表格 ------------------------------------------------------------ */
.table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
}

.table th,
.table td {
  padding: 10px 14px;
  text-align: left;
  border-bottom: 1px solid var(--border-subtle);
}

.table th {
  background: var(--bg-header-row);
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  position: sticky;
  top: 0;
}

.table th:first-child {
  border-radius: var(--radius-sm) 0 0 var(--radius-sm);
}

.table th:last-child {
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.table tbody tr {
  transition: background var(--transition-fast);
}

.table tbody tr:hover {
  background: var(--bg-row-hover);
}

.table tbody tr:nth-child(even) {
  background: var(--bg-row-alt);
}

.table tbody tr:nth-child(even):hover {
  background: var(--bg-row-hover);
}

.table a {
  color: var(--primary);
  text-decoration: none;
  cursor: pointer;
}

.table a:hover {
  text-decoration: underline;
}

/* ---- 徽标 ------------------------------------------------------------ */
.badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: 100px;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.badge::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.badge-success {
  background: var(--green-subtle);
  color: var(--green);
}

.badge-success::before { background: var(--green); }

.badge-warning {
  background: var(--amber-subtle);
  color: var(--amber);
}

.badge-warning::before { background: var(--amber); }

.badge-error {
  background: var(--red-subtle);
  color: var(--red);
}

.badge-error::before { background: var(--red); }

.badge-info {
  background: var(--cyan-subtle);
  color: var(--cyan);
}

.badge-info::before { background: var(--cyan); }

.badge-primary {
  background: var(--primary-subtle);
  color: var(--primary);
}

.badge-primary::before { background: var(--primary); }

/* ---- 筛选栏 ---------------------------------------------------------- */
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-bar input,
.filter-bar select {
  padding: 7px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  font-size: 13px;
  background: var(--bg-input);
  color: var(--text-primary);
  outline: none;
  transition: border-color var(--transition-fast);
  min-width: 140px;
}

.filter-bar input::placeholder {
  color: var(--text-muted);
}

.filter-bar input:focus,
.filter-bar select:focus {
  border-color: var(--border-focus);
}

/* ---- 分页 ------------------------------------------------------------ */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
}

.pagination button {
  padding: 6px 14px;
  border: 1px solid var(--border-default);
  background: var(--bg-card);
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 13px;
  transition: all var(--transition-fast);
}

.pagination button:hover:not(:disabled) {
  border-color: var(--border-strong);
  color: var(--text-primary);
  background: var(--bg-card-hover);
}

.pagination button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.pagination button.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.pagination span {
  color: var(--text-muted);
  font-size: 13px;
  padding: 0 8px;
}

/* ---- 信息网格（详情页） ---------------------------------------------- */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.info-label {
  font-size: 11.5px;
  color: var(--text-muted);
  font-weight: 500;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.info-value {
  font-size: 13.5px;
  color: var(--text-data);
  font-weight: 500;
}

.info-value a {
  color: var(--primary);
  text-decoration: none;
  cursor: pointer;
}

.info-value a:hover {
  text-decoration: underline;
}

/* ---- 空状态 ---------------------------------------------------------- */
.empty-state {
  text-align: center;
  padding: 48px;
  color: var(--text-muted);
  font-size: 14px;
}

/* ---- 代码/标签样式 --------------------------------------------------- */
code {
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 2px 6px;
  background: var(--bg-input);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  color: var(--text-data);
}

/* ---- 响应式 ---------------------------------------------------------- */
@media (max-width: 768px) {
  .page-content {
    padding: 16px;
  }

  .header-inner {
    padding: 0 16px;
  }

  .breadcrumb-inner {
    padding: 8px 16px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar input,
  .filter-bar select {
    min-width: 100px;
    flex: 1;
  }
}
</style>
