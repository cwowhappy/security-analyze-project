<script setup lang="ts">
import { onMounted, ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFinancialStore } from '@/stores/modules/financial'
import { useStockStore } from '@/stores/modules/stock'
import FinancialTabs from './components/FinancialTabs.vue'
import StatementTabs from './components/StatementTabs.vue'
import IndicatorGrid from './components/IndicatorGrid.vue'
import IncomeStatement from './components/IncomeStatement.vue'
import BalanceSheet from './components/BalanceSheet.vue'
import CashflowStatement from './components/CashflowStatement.vue'
import IndicatorTrendChart from './components/IndicatorTrendChart.vue'
import DupontAnalysis from './components/DupontAnalysis.vue'
import PeerComparisonChart from './components/PeerComparisonChart.vue'

const props = defineProps<{
  stockCode: string
}>()

const financialStore = useFinancialStore()
const stockStore = useStockStore()
const router = useRouter()

const stockName = computed(() => stockStore.currentStock?.name || props.stockCode)

const activeStatement = ref('income')

const peerMetric = ref('roe')
const peerMetrics = [
  { value: 'roe', label: 'ROE' },
  { value: 'roa', label: 'ROA' },
  { value: 'grossMargin', label: '毛利率' },
  { value: 'netMargin', label: '净利率' },
  { value: 'revenueGrowth', label: '营收增速' },
  { value: 'npParentGrowth', label: '归母净利增速' },
  { value: 'debtRatio', label: '资产负债率' },
  { value: 'currentRatio', label: '流动比率' },
  { value: 'pe', label: 'PE-TTM' },
  { value: 'pb', label: 'PB' },
]

const trendMetrics = ['roe', 'roa', 'grossMargin', 'netMargin', 'revenueGrowth', 'npParentGrowth']

// 年份区间选择器（仅用于财务报表页签）
const startYear = ref<number | ''>('')
const endYear = ref<number | ''>('')

const availableYears = computed(() => {
  const years = new Set<number>()
  financialStore.incomes.forEach((d) => {
    const y = parseInt(d.reportDate.substring(0, 4), 10)
    if (!isNaN(y)) years.add(y)
  })
  return Array.from(years).sort((a, b) => b - a)
})

function filterByYearRange<T extends { reportDate: string }>(data: T[]): T[] {
  if (!startYear.value && !endYear.value) return data
  return data.filter((d) => {
    const y = parseInt(d.reportDate.substring(0, 4), 10)
    if (isNaN(y)) return false
    if (startYear.value && y < startYear.value) return false
    if (endYear.value && y > endYear.value) return false
    return true
  })
}

const statementIncomes = computed(() => filterByYearRange(financialStore.incomes))
const statementBalances = computed(() => filterByYearRange(financialStore.balances))
const statementCashflows = computed(() => filterByYearRange(financialStore.cashflows))

function loadTrend() {
  if (financialStore.trendData.length === 0) {
    financialStore.fetchTrend(props.stockCode, trendMetrics, 8)
  }
}

function loadDupont() {
  if (!financialStore.dupontData && financialStore.latestIndicator) {
    const indicator = financialStore.latestIndicator
    financialStore.fetchDupont(props.stockCode, indicator.reportDate, indicator.reportType)
  }
}

function loadPeer() {
  if (financialStore.latestIndicator) {
    financialStore.fetchPeerComparison(props.stockCode, peerMetric.value, financialStore.latestIndicator.reportType)
  }
}

function onTabChange() {
  if (financialStore.activeTab === 'analysis') {
    loadTrend()
    loadDupont()
    loadPeer()
  }
}

function onPeerMetricChange() {
  loadPeer()
}

onMounted(() => {
  if (stockStore.currentStock?.stockCode !== props.stockCode) {
    stockStore.fetchStockDetail(props.stockCode)
  }
  financialStore.fetchAll(props.stockCode).then(() => {
    loadTrend()
    loadDupont()
    loadPeer()
  })
})

watch(() => props.stockCode, (newCode) => {
  financialStore.trendData = []
  financialStore.dupontData = null
  financialStore.peerComparison = null
  startYear.value = ''
  endYear.value = ''
  stockStore.fetchStockDetail(newCode)
  financialStore.fetchAll(newCode).then(() => {
    loadTrend()
    loadDupont()
    loadPeer()
  })
})

function goBack() {
  router.push(`/stocks/${props.stockCode}`)
}
</script>

<template>
  <div class="fin-page">
    <div class="fin-header">
      <button class="bk" @click="goBack">← 返回股票详情</button>
      <div class="fin-title-row">
        <h1 class="fin-title">
          <span class="stock-name">{{ stockName }}</span>
          <span class="stock-code">[{{ props.stockCode }}]</span>
          <span class="fin-label">财务分析</span>
        </h1>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="financialStore.loading && !financialStore.incomes.length" class="loading-state">
      <div class="spinner" />
      <p>正在加载财务数据...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="financialStore.error && !financialStore.incomes.length" class="error-state">
      <p>{{ financialStore.error }}</p>
      <button class="retry-btn" @click="financialStore.fetchAll(props.stockCode)">重新加载</button>
    </div>

    <!-- 主内容 -->
    <template v-else>
      <FinancialTabs v-model="financialStore.activeTab" @update:model-value="onTabChange" />

      <!-- 基本面分析 -->
      <div v-if="financialStore.activeTab === 'analysis'" class="tab-panel">
        <h2 class="panel-title">核心财务指标</h2>
        <IndicatorGrid :indicator="financialStore.latestIndicator" />

        <h2 class="panel-title">指标趋势</h2>
        <IndicatorTrendChart :data="financialStore.trendData" />

        <div class="two-col-grid">
          <div>
            <h2 class="panel-title" style="margin-top: 24px">杜邦分析</h2>
            <DupontAnalysis :data="financialStore.dupontData" />
          </div>
          <div>
            <div class="peer-controls">
              <h2 class="panel-title" style="margin: 0; border: none;">同业对比</h2>
              <select class="metric-select" v-model="peerMetric" @change="onPeerMetricChange">
                <option v-for="m in peerMetrics" :key="m.value" :value="m.value">{{ m.label }}</option>
              </select>
            </div>
            <PeerComparisonChart :data="financialStore.peerComparison" />
          </div>
        </div>
      </div>

      <!-- 财务报表 -->
      <div v-else-if="financialStore.activeTab === 'statements'" class="tab-panel">
        <div class="statement-controls">
          <StatementTabs v-model="activeStatement" />
          <div class="year-filters">
            <select class="year-select" v-model="startYear">
              <option value="">起始年份</option>
              <option v-for="y in availableYears" :key="y" :value="y">{{ y }}年</option>
            </select>
            <span class="year-sep">至</span>
            <select class="year-select" v-model="endYear">
              <option value="">结束年份</option>
              <option v-for="y in availableYears" :key="y" :value="y">{{ y }}年</option>
            </select>
            <button v-if="startYear || endYear" class="reset-btn" @click="startYear = ''; endYear = ''">重置</button>
          </div>
        </div>

        <div v-if="activeStatement === 'income'">
          <h2 class="panel-title">利润表</h2>
          <IncomeStatement :data="statementIncomes" />
        </div>
        <div v-else-if="activeStatement === 'balance'">
          <h2 class="panel-title">资产负债表</h2>
          <BalanceSheet :data="statementBalances" />
        </div>
        <div v-else-if="activeStatement === 'cashflow'">
          <h2 class="panel-title">现金流量表</h2>
          <CashflowStatement :data="statementCashflows" />
        </div>
      </div>

      <!-- AI 解读 -->
      <div v-else-if="financialStore.activeTab === 'ai-report'" class="tab-panel">
        <div class="placeholder-card">
          <div class="placeholder-icon">🤖</div>
          <h3 class="placeholder-title">AI 财报解读</h3>
          <p class="placeholder-desc">基于大语言模型的智能财报分析功能，将为您提供：</p>
          <ul class="placeholder-list">
            <li>盈利能力、成长性、现金流、财务健康度四维评分</li>
            <li>结构化风险信号识别与预警</li>
            <li>Markdown 格式的深度解读报告</li>
          </ul>
          <span class="placeholder-badge">敬请期待</span>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.fin-page {
  padding-bottom: 40px;
}

.fin-header {
  margin-bottom: 20px;
}

.fin-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  flex-wrap: wrap;
  gap: 10px;
}

.fin-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
}

.stock-name {
  font-family: var(--mono);
  color: var(--primary);
}

.stock-code {
  color: var(--text-primary);
}

.fin-label {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-secondary);
  letter-spacing: 1px;
  margin-left: 4px;
}

.bk {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-family: var(--font);
  padding: 6px 0;
  transition: color 0.15s;
}

.bk:hover {
  color: var(--primary);
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.tab-panel {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.two-col-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-top: 8px;
}

@media (max-width: 1024px) {
  .two-col-grid {
    grid-template-columns: 1fr;
  }
}

.peer-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.metric-select {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text-primary);
  font-family: var(--font);
  font-size: 13px;
  cursor: pointer;
  outline: none;
}

.metric-select:focus {
  border-color: var(--primary);
}

/* 财务报表控制栏 */
.statement-controls {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.year-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.year-select {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text-primary);
  font-family: var(--font);
  font-size: 13px;
  cursor: pointer;
  outline: none;
}

.year-select:focus {
  border-color: var(--primary);
}

.year-sep {
  font-size: 13px;
  color: var(--text-muted);
  font-family: var(--font);
}

.reset-btn {
  padding: 6px 12px;
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-family: var(--font);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.reset-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--text-muted);
  gap: 16px;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.retry-btn {
  padding: 8px 20px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.retry-btn:hover {
  background: var(--primary-hover);
}

/* AI 解读占位 */
.placeholder-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 60px 40px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  gap: 12px;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.placeholder-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.placeholder-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.placeholder-list {
  list-style: none;
  padding: 0;
  margin: 0 0 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.placeholder-list li {
  font-size: 13px;
  color: var(--text-muted);
  position: relative;
  padding-left: 16px;
}

.placeholder-list li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: var(--primary);
}

.placeholder-badge {
  display: inline-block;
  padding: 4px 12px;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
  border-radius: 12px;
  border: 1px solid rgba(99, 91, 255, 0.2);
}
</style>
