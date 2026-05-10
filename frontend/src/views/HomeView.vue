<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const columns = [
  {
    name: '证券市场',
    desc: 'A 股市场股票与上市公司信息查询',
    items: [
      { label: '股票信息', path: '/stocks', desc: '浏览全量股票列表、查看股票详情', available: true },
      { label: '公司信息', path: '/companies', desc: '查询上市公司详情、关联股票等', available: true },
    ],
  },
  {
    name: '投研分析',
    desc: '行业研究与持仓分析（待开发）',
    items: [
      { label: '行业分析', path: '', desc: '按行业维度分析股票市场表现与趋势', available: false },
      { label: '证券持仓', path: '', desc: '管理并分析证券持仓组合', available: false },
    ],
  },
  {
    name: '辅助工具',
    desc: '数据采集任务管理与调度',
    items: [
      { label: '采集任务', path: '/collection/tasks', desc: '创建与管理即时数据采集任务', available: true },
      { label: '定时规则', path: '/collection/schedules', desc: '配置 Cron 规则实现自动化采集', available: true },
    ],
  },
]

function navigate(item: { path: string; available: boolean }) {
  if (item.available && item.path) {
    router.push(item.path)
  }
}
</script>

<template>
  <div class="home">
    <div class="home-header">
      <h1 class="home-title">欢迎使用证券分析系统</h1>
      <p class="home-subtitle">整合多数据源，提供全面的 A 股市场信息查询与分析能力</p>
    </div>

    <div class="column-grid">
      <div v-for="col in columns" :key="col.name" class="column-card">
        <div class="column-header">
          <h2 class="column-title">{{ col.name }}</h2>
          <p class="column-desc">{{ col.desc }}</p>
        </div>
        <div class="column-items">
          <div
            v-for="item in col.items"
            :key="item.label"
            class="item-row"
            :class="{ disabled: !item.available }"
            @click="navigate(item)"
          >
            <div class="item-left">
              <span class="item-label">{{ item.label }}</span>
              <span v-if="!item.available" class="item-badge">待开发</span>
            </div>
            <span class="item-arrow" :class="{ muted: !item.available }">›</span>
            <p class="item-desc" :class="{ muted: !item.available }">{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  padding: 8px 0 32px;
}

.home-header {
  margin-bottom: 28px;
}

.home-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.home-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

/* ---- 三列分栏 -------------------------------------------------------- */
.column-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.column-card {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 24px;
  transition: border-color var(--transition-fast);
}

.column-card:hover {
  border-color: var(--border-strong);
}

.column-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.column-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
  letter-spacing: 0.3px;
}

.column-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.column-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-row {
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto;
  align-items: center;
  gap: 2px 8px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.item-row:hover:not(.disabled) {
  background: rgba(255, 255, 255, 0.04);
  border-color: var(--border-default);
}

.item-row.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.item-badge {
  font-size: 11px;
  font-weight: 500;
  color: var(--text-muted);
  padding: 1px 7px;
  border-radius: 100px;
  background: var(--bg-input);
  border: 1px solid var(--border-default);
}

.item-arrow {
  font-size: 18px;
  color: var(--text-muted);
  transition: color var(--transition-fast);
}

.item-row:hover:not(.disabled) .item-arrow {
  color: var(--primary);
}

.item-arrow.muted {
  color: var(--text-muted);
  opacity: 0.4;
}

.item-desc {
  grid-column: 1 / -1;
  font-size: 12.5px;
  color: var(--text-secondary);
  line-height: 1.4;
  margin: 0;
}

.item-desc.muted {
  color: var(--text-muted);
}

@media (max-width: 992px) {
  .column-grid {
    grid-template-columns: 1fr;
  }

  .home-title {
    font-size: 20px;
  }
}
</style>
