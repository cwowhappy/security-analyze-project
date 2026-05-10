<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElEmpty, ElSelect, ElOption } from 'element-plus'
import { getFundamentalOverview } from '@/api/research'
import type { FundamentalOverview, AnnualMetric } from '@/types/research'
import { getChartColor } from '@/utils/colors'
import MetricDashboard from '@/views/research/components/MetricDashboard.vue'
import ProfitabilityChart from '@/views/research/components/ProfitabilityChart.vue'
import CostExpenseChart from '@/views/research/components/CostExpenseChart.vue'
import BalanceSheetChart from '@/views/research/components/BalanceSheetChart.vue'
import CashFlowChart from '@/views/research/components/CashFlowChart.vue'
import DupontAnalysisChart from '@/views/research/components/DupontAnalysisChart.vue'

const props = defineProps<{
  stockCode: string
}>()

const loading = ref(false)
const overview = ref<FundamentalOverview | null>(null)
const selectedYear = ref<number | null>(null)

const availableYears = computed(() => {
  if (!overview.value?.metrics?.length) return []
  return overview.value.metrics.map(m => m.reportYear)
})

const selectedMetric = computed<AnnualMetric | null>(() => {
  if (!overview.value?.metrics?.length) return null
  if (selectedYear.value != null) {
    return overview.value.metrics.find(m => m.reportYear === selectedYear.value) || null
  }
  return overview.value.metrics[overview.value.metrics.length - 1]
})

function getYoyInfo(val?: number): { value?: number; direction?: 'up' | 'down' | 'flat' } {
  if (val == null) return {}
  if (val > 0.01) return { value: val, direction: 'up' }
  if (val < -0.01) return { value: val, direction: 'down' }
  return { value: val, direction: 'flat' }
}

const metricCards = computed(() => {
  const m = selectedMetric.value
  if (!m) return []
  const revenueYoy = getYoyInfo(m.revenueYoy)
  const profitYoy = getYoyInfo(m.profitYoy)
  return [
    { label: '营业总收入', value: formatMoney(m.totalRevenue), color: getChartColor(0), yoyValue: revenueYoy.value, yoyDirection: revenueYoy.direction },
    { label: '归母净利润', value: formatMoney(m.parentNetProfit), color: getChartColor(1), yoyValue: profitYoy.value, yoyDirection: profitYoy.direction },
    { label: '毛利率', value: formatPercent(m.grossMargin), color: getChartColor(2) },
    { label: '净利率', value: formatPercent(m.netMargin), color: getChartColor(3) },
    { label: 'ROE', value: formatPercent(m.roe), color: getChartColor(4) },
    { label: '总资产', value: formatMoney(m.totalAssets), color: getChartColor(5) },
    { label: '资产负债率', value: formatPercent(m.debtRatio), color: getChartColor(6) },
    { label: '经营现金流/净利润', value: formatPercent(m.cashflowProfitRatio), color: getChartColor(7) },
  ]
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getFundamentalOverview(props.stockCode)
    overview.value = res
    if (res.metrics?.length) {
      selectedYear.value = res.metrics[res.metrics.length - 1].reportYear
    } else {
      selectedYear.value = null
    }
  } catch (err) {
    ElMessage.error('加载基本面数据失败')
    overview.value = null
    selectedYear.value = null
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
  <div v-loading="loading">
    <template v-if="overview">
      <!-- 年份筛选 -->
      <div class="year-filter-bar">
        <span class="year-filter-label">数据年份：</span>
        <ElSelect v-model="selectedYear" placeholder="选择年份" size="small" style="width: 120px">
          <ElOption
            v-for="year in availableYears"
            :key="year"
            :label="year + ' 年'"
            :value="year"
          />
        </ElSelect>
      </div>

      <MetricDashboard :cards="metricCards" />

      <div class="charts-container">
        <ProfitabilityChart :metrics="overview.metrics" />
        <CostExpenseChart :metrics="overview.metrics" />
        <BalanceSheetChart :metrics="overview.metrics" />
        <CashFlowChart :metrics="overview.metrics" />
        <DupontAnalysisChart :metrics="overview.metrics" />
      </div>
    </template>

    <ElEmpty v-else description="暂无年报数据，请检查采集任务是否已完成" />
  </div>
</template>

<style scoped>
.year-filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 8px;
}
.year-filter-label {
  font-size: 14px;
  color: var(--text-secondary, #9ca3af);
}
.charts-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
</style>
