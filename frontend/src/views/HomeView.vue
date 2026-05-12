<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { stockApi } from '@/api/modules/stock'
import { companyApi } from '@/api/modules/company'

const router = useRouter()

const stats = ref({
  stockTotal: 0,
  companyTotal: 0,
  nextCollection: '02:00',
  successRate: '99.4%',
})

const featCards = [
  { icon: '📋', iconClass: 'violet', title: '股票列表', desc: '全市场 A 股基础信息，支持多维筛选与快速查询', tag: '已上线', tagClass: 'tag-active', path: '/stocks' },
  { icon: '🏢', iconClass: 'teal', title: '公司列表', desc: '上市公司工商信息、管理层与主营业务', tag: '已上线', tagClass: 'tag-active', path: '/companies' },
  { icon: '⏱', iconClass: 'coral', title: '采集任务', desc: '管理数据采集任务，监控执行状态与成功率', tag: '已上线', tagClass: 'tag-active', path: '/collection/tasks' },
  { icon: '📊', iconClass: 'amber', title: '数据分析', desc: '多维数据分析、财报拆解与趋势预测', tag: '即将上线', tagClass: 'tag-soon', path: '' },
  { icon: '💼', iconClass: 'sky', title: '投资管理', desc: '组合持仓管理、收益追踪与再平衡建议', tag: '即将上线', tagClass: 'tag-soon', path: '' },
  { icon: '⚖️', iconClass: 'mint', title: '风险管理', desc: '风险敞口监控、黑天鹅预警与压力测试', tag: '即将上线', tagClass: 'tag-soon', path: '' },
]

const statCards = computed(() => [
  { icon: '📈', iconClass: 'blue', value: stats.value.stockTotal.toLocaleString(), label: 'A股上市公司总数' },
  { icon: '✓', iconClass: 'green', value: stats.value.companyTotal.toLocaleString(), label: '数据覆盖股票' },
  { icon: '⏱', iconClass: 'orange', value: stats.value.nextCollection, label: '下次定时采集' },
  { icon: '✓', iconClass: 'red', value: stats.value.successRate, label: '最近采集成功率' },
])

onMounted(async () => {
  try {
    const [stockRes, companyRes] = await Promise.all([
      stockApi.page({ page: 1, size: 1 }),
      companyApi.list({ page: 1, size: 1 }),
    ])
    stats.value.stockTotal = stockRes.total ?? 0
    stats.value.companyTotal = companyRes.total ?? 0
  } catch (e) {
    /* ignore */
  }
})

function go(path: string) {
  if (path) router.push(path)
}
</script>

<template>
  <div class="home">
    <!-- Welcome -->
    <div class="welcome">
      <div class="welcome-t">你好，lixiaoyi</div>
      <div class="welcome-s">今日 A 股市场正常运行 · 祝投资顺利</div>
    </div>

    <!-- Stats -->
    <div class="stat-row">
      <div v-for="(s, i) in statCards" :key="i" class="stat-card">
        <div class="stat-icon" :class="s.iconClass">{{ s.icon }}</div>
        <div class="stat-n">{{ s.value }}</div>
        <div class="stat-l">{{ s.label }}</div>
      </div>
    </div>

    <!-- Features -->
    <div class="feat-grid">
      <div
        v-for="card in featCards"
        :key="card.title"
        class="feat-card"
        :class="{ disabled: !card.path }"
        @click="go(card.path)"
      >
        <div class="feat-icon" :class="card.iconClass">{{ card.icon }}</div>
        <div class="feat-t">{{ card.title }}</div>
        <div class="feat-d">{{ card.desc }}</div>
        <span class="feat-tag" :class="card.tagClass">{{ card.tag }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  padding: 0 0 32px;
  animation: fi 0.2s ease;
}

@keyframes fi {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Welcome */
.welcome {
  background: var(--primary);
  border-radius: var(--radius-lg);
  padding: 32px;
  margin-bottom: 24px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.welcome::after {
  content: '';
  position: absolute;
  right: -30px;
  top: -30px;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

.welcome::before {
  content: '';
  position: absolute;
  right: 60px;
  bottom: -60px;
  width: 160px;
  height: 160px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 50%;
}

.welcome-t {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 6px;
  position: relative;
  z-index: 1;
}

.welcome-s {
  font-size: 13px;
  opacity: 0.8;
  position: relative;
  z-index: 1;
}

/* Stats */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  transition: all 0.2s;
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-bottom: 12px;
}

.stat-icon.blue {
  background: rgba(99, 91, 255, 0.12);
  color: var(--primary);
}

.stat-icon.green {
  background: rgba(0, 217, 36, 0.12);
  color: var(--success);
}

.stat-icon.orange {
  background: rgba(255, 149, 0, 0.12);
  color: var(--warning);
}

.stat-icon.red {
  background: rgba(255, 59, 48, 0.12);
  color: var(--danger);
}

.stat-n {
  font-size: 26px;
  font-weight: 700;
  font-family: var(--mono);
  color: var(--text-primary);
}

.stat-l {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

/* Features */
.feat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.feat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: var(--shadow-sm);
}

.feat-card:hover:not(.disabled) {
  border-color: var(--primary);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.feat-card.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.feat-icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.feat-icon.violet {
  background: rgba(99, 91, 255, 0.12);
  color: var(--primary);
}

.feat-icon.teal {
  background: rgba(0, 180, 180, 0.12);
  color: #00B4B4;
}

.feat-icon.coral {
  background: rgba(255, 100, 80, 0.12);
  color: #FF6450;
}

.feat-icon.amber {
  background: rgba(255, 149, 0, 0.12);
  color: #FF9500;
}

.feat-icon.sky {
  background: rgba(30, 140, 255, 0.12);
  color: #1E8CFF;
}

.feat-icon.mint {
  background: rgba(0, 200, 130, 0.12);
  color: #00C882;
}

.feat-t {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--text-primary);
}

.feat-d {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.feat-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  margin-top: 12px;
}

.tag-active {
  background: rgba(0, 217, 36, 0.12);
  color: var(--success);
}

.tag-soon {
  background: var(--surface-hover);
  color: var(--text-muted);
  border: 1px solid var(--border);
}

/* Responsive */
@media (max-width: 900px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .feat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stat-row {
    grid-template-columns: 1fr 1fr;
  }
  .feat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
