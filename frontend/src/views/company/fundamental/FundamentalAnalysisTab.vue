<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElSkeleton, ElEmpty, ElCollapse, ElCollapseItem, ElMessage } from 'element-plus'
import { getFundamentalOverview } from '@/api/research'
import type { FundamentalOverview, AnnualMetric } from '@/types/research'
import ProfitabilityChart from '@/views/research/components/ProfitabilityChart.vue'
import CostExpenseChart from '@/views/research/components/CostExpenseChart.vue'
import BalanceSheetChart from '@/views/research/components/BalanceSheetChart.vue'
import CashFlowChart from '@/views/research/components/CashFlowChart.vue'

const props = defineProps<{
  stockCode: string
}>()

const loading = ref(false)
const overview = ref<FundamentalOverview | null>(null)

const latestMetric = computed<AnnualMetric | null>(() => {
  if (!overview.value?.metrics?.length) return null
  return overview.value.metrics[overview.value.metrics.length - 1]
})

const metricCards = computed(() => {
  const m = latestMetric.value
  if (!m) return []
  return [
    { label: '营业总收入', value: formatMoney(m.totalRevenue), color: '#00d4ff' },
    { label: '归母净利润', value: formatMoney(m.parentNetProfit), color: '#67c23a' },
    { label: '总资产', value: formatMoney(m.totalAssets), color: '#ff9500' },
    { label: '净资产', value: formatMoney(m.totalEquity), color: '#f56c6c' },
    { label: '经营现金流', value: formatMoney(m.operatingCashFlow), color: '#9ca3af' },
    { label: '毛利率', value: formatPercent(m.grossMargin), color: '#409eff' },
    { label: '净利率', value: formatPercent(m.netMargin), color: '#67c23a' },
    { label: 'ROE', value: formatPercent(m.roe), color: '#e6a23c' },
  ]
})

const tableColumns = [
  { prop: 'reportDate', label: '报告期' },
  { prop: 'totalRevenue', label: '营收', formatter: formatMoney },
  { prop: 'parentNetProfit', label: '净利润', formatter: formatMoney },
  { prop: 'grossMargin', label: '毛利率', formatter: formatPercent },
  { prop: 'roe', label: 'ROE', formatter: formatPercent },
  { prop: 'totalAssets', label: '总资产', formatter: formatMoney },
  { prop: 'debtRatio', label: '负债率', formatter: formatPercent },
  { prop: 'operatingCashFlow', label: '经营现金流', formatter: formatMoney },
]

async function fetchData() {
  loading.value = true
  try {
    overview.value = await getFundamentalOverview(props.stockCode)
  } catch (err) {
    ElMessage.error('加载基本面数据失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

function formatMoney(val?: number): string {
  if (val == null) return '-'
  const abs = Math.abs(val)
  if (abs >= 1e8) return (val / 1e8).toFixed(2) + ' 亿'
  if (abs >= 1e4) return (val / 1e4).toFixed(2) + ' 万'
  return val.toLocaleString()
}

function formatPercent(val?: number): string {
  if (val == null) return '-'
  return val.toFixed(2) + '%'
}

onMounted(fetchData)
watch(() => props.stockCode, fetchData)
</script>

<template>
  <div class="fundamental-tab">
    <!-- 加载状态 -->
    <template v-if="loading">
      <div class="metric-cards">
        <ElSkeleton v-for="i in 8" :key="i" :rows="1" animated style="height: 80px" />
      </div>
      <ElSkeleton :rows="5" animated style="margin-top: 24px" />
    </template>

    <!-- 有数据 -->
    <template v-else-if="overview && overview.metrics.length > 0">
      <!-- 核心指标卡片区 -->
      <div class="metric-cards">
        <div
          v-for="card in metricCards"
          :key="card.label"
          class="metric-card"
          :style="{ borderTop: `3px solid ${card.color}` }"
        >
          <div class="card-label">{{ card.label }}</div>
          <div class="card-value" :style="{ color: card.color }">{{ card.value }}</div>
        </div>
      </div>

      <!-- 趋势图表区 -->
      <div class="charts-area">
        <ProfitabilityChart :metrics="overview.metrics" />
        <CostExpenseChart :metrics="overview.metrics" />
        <BalanceSheetChart :metrics="overview.metrics" />
        <CashFlowChart :metrics="overview.metrics" />
      </div>

      <!-- 数据一览表 -->
      <ElCollapse style="margin-top: 24px">
        <ElCollapseItem title="近5年数据一览">
          <table class="data-table">
            <thead>
              <tr>
                <th v-for="col in tableColumns" :key="col.prop">{{ col.label }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in overview.metrics" :key="row.reportDate">
                <td v-for="col in tableColumns" :key="col.prop">
                  {{ col.formatter ? col.formatter((row as any)[col.prop]) : (row as any)[col.prop] }}
                </td>
              </tr>
            </tbody>
          </table>
        </ElCollapseItem>
      </ElCollapse>
    </template>

    <!-- 空态 -->
    <ElEmpty v-else description="暂无年报数据，请检查采集任务是否已完成" />
  </div>
</template>

<style scoped>
.fundamental-tab { padding: 8px 0; }
.metric-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
@media (max-width: 768px) {
  .metric-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
.metric-card {
  background: var(--card-bg, rgba(255,255,255,0.03));
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}
.card-label {
  font-size: 13px;
  color: var(--text-secondary, #9ca3af);
  margin-bottom: 8px;
}
.card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: var(--font-mono, monospace);
}
.charts-area {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.data-table th,
.data-table td {
  padding: 10px 12px;
  text-align: right;
  border-bottom: 1px solid var(--border-color, rgba(255,255,255,0.06));
}
.data-table th {
  color: var(--text-secondary, #9ca3af);
  font-weight: 500;
  text-align: right;
}
.data-table td {
  color: var(--text-primary, #e5e7eb);
}
.data-table tbody tr:hover {
  background: rgba(255,255,255,0.02);
}
</style>
