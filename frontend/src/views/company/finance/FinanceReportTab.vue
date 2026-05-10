<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { ElTabs, ElTabPane, ElSelect, ElOption, ElMessage } from 'element-plus'
import { getFinanceReports, getFinanceReportDetail, getFinanceIndicators, getYearlyIndicators } from '@/api/finance'
import type { FinanceReportItem, FinanceReportDetail, IndicatorMetric } from '@/types/finance'
import IndicatorChart from './IndicatorChart.vue'
import ReportSummaryCards from './ReportSummaryCards.vue'
import BalanceSheetTable from './BalanceSheetTable.vue'
import ProfitSheetTable from './ProfitSheetTable.vue'
import CashFlowSheetTable from './CashFlowSheetTable.vue'

const props = defineProps<{
  stockCode: string
}>()

const loading = ref(false)
const reports = ref<FinanceReportItem[]>([])

// ========== 趋势分析区域 ==========
const trendStartDate = ref<string>('')
const trendEndDate = ref<string>('')
const trendReportType = ref<string>('all')
const indicators = ref<IndicatorMetric[]>([])

// 报告类型选项
const reportTypeOptions = [
  { value: 'all', label: '全部' },
  { value: '一季报', label: '一季度' },
  { value: '中报', label: '半年度' },
  { value: '三季报', label: '三季度' },
  { value: '年报', label: '年度' },
]

// 可用的报告期选项（倒序，最新的在前）
const reportDateOptions = computed(() => {
  return reports.value.map((r) => ({
    value: r.reportDate,
    label: `${r.reportDate} ${r.reportType}`,
  }))
})

async function fetchIndicators() {
  try {
    const res = await getFinanceIndicators(
      props.stockCode,
      ['totalRevenue', 'netProfit', 'grossMargin', 'netMargin', 'debtRatio'],
      trendStartDate.value || undefined,
      trendEndDate.value || undefined,
      trendReportType.value
    )
    indicators.value = res.metrics
  } catch (err) {
    console.error('加载指标趋势失败', err)
  }
}

// ========== 年度报告期对比区域 ==========
const comparisonYear = ref<number | null>(null)
const yearlyIndicators = ref<IndicatorMetric[]>([])

const yearOptions = computed(() => {
  const years = new Set(reports.value.map((r) => r.reportYear))
  return Array.from(years).sort((a, b) => b - a)
})

async function fetchYearlyIndicators() {
  if (!comparisonYear.value) {
    yearlyIndicators.value = []
    return
  }
  try {
    const res = await getYearlyIndicators(props.stockCode, comparisonYear.value)
    yearlyIndicators.value = res.metrics
  } catch (err) {
    console.error('加载年度指标对比失败', err)
  }
}

// ========== 报告详情区域 ==========
const selectedReportId = ref<number | null>(null)
const reportDetail = ref<FinanceReportDetail | null>(null)
const activeSheetTab = ref('balance')

async function fetchReportDetail(reportId: number) {
  try {
    const res = await getFinanceReportDetail(reportId)
    reportDetail.value = res
  } catch (err) {
    ElMessage.error('加载报告详情失败')
    console.error(err)
  }
}

// ========== 数据加载 ==========
async function fetchReports() {
  loading.value = true
  try {
    const res = await getFinanceReports(props.stockCode)
    reports.value = res.items

    // 默认选中最近一期报告
    if (reports.value.length > 0 && !selectedReportId.value) {
      selectedReportId.value = reports.value[0].id
    }

    // 默认选中最近有数据的年份
    if (yearOptions.value.length > 0 && !comparisonYear.value) {
      comparisonYear.value = yearOptions.value[0]
    }
  } catch (err) {
    ElMessage.error('加载财务报告列表失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

watch(selectedReportId, (id) => {
  if (id !== null) {
    fetchReportDetail(id)
  }
})

watch([trendStartDate, trendEndDate, trendReportType], () => {
  fetchIndicators()
})

watch(comparisonYear, () => {
  fetchYearlyIndicators()
})

onMounted(() => {
  fetchReports().then(() => {
    fetchIndicators()
    fetchYearlyIndicators()
  })
})
</script>

<template>
  <div class="finance-report-tab" v-loading="loading">
    <!-- 上排：趋势 + 对比并排 -->
    <div class="top-row">
      <!-- ========== 核心指标趋势分析区域 ========== -->
      <div class="section trend-section">
        <div class="section-header">
          <h4 class="section-title">核心指标趋势分析</h4>
          <div class="date-range-selector">
            <ElSelect
              v-model="trendReportType"
              placeholder="报告类型"
              style="width: 110px"
              size="small"
            >
              <ElOption
                v-for="opt in reportTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </ElSelect>
            <ElSelect
              v-model="trendStartDate"
              placeholder="起始期"
              clearable
              size="small"
              style="width: 150px"
            >
              <ElOption
                v-for="opt in reportDateOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </ElSelect>
            <span class="range-separator">→</span>
            <ElSelect
              v-model="trendEndDate"
              placeholder="结束期"
              clearable
              size="small"
              style="width: 150px"
            >
              <ElOption
                v-for="opt in reportDateOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
                :disabled="trendStartDate ? opt.value < trendStartDate : false"
              />
            </ElSelect>
          </div>
        </div>
        <IndicatorChart :metrics="indicators" />
      </div>

      <!-- ========== 年度报告期对比区域 ========== -->
      <div class="section comparison-section">
        <div class="section-header">
          <h4 class="section-title">年度报告期对比</h4>
          <div class="year-selector">
            <ElSelect
              v-model="comparisonYear"
              placeholder="选择年份"
              clearable
              size="small"
              style="width: 120px"
            >
              <ElOption
                v-for="year in yearOptions"
                :key="year"
                :label="`${year}年`"
                :value="year"
              />
            </ElSelect>
          </div>
        </div>
        <IndicatorChart
          v-if="comparisonYear"
          :metrics="yearlyIndicators"
          :x-axis-labels="['一季报', '中报', '三季报', '年报']"
        />
        <div v-else class="empty-tip">请选择年份查看对比</div>
      </div>
    </div>

    <!-- ========== 选定报告期详情区域 ========== -->
    <div class="section detail-section">
      <div class="section-header">
        <h4 class="section-title">选定报告期详情</h4>
        <div class="report-selector">
          <ElSelect v-model="selectedReportId" placeholder="选择报告期" size="small" style="width: 200px">
            <ElOption
              v-for="report in reports"
              :key="report.id"
              :label="`${report.reportDate} ${report.reportType}`"
              :value="report.id"
            />
          </ElSelect>
        </div>
      </div>

      <div v-if="reportDetail">
        <ReportSummaryCards :summary="reportDetail.summary" />

        <ElTabs v-model="activeSheetTab" class="sheet-tabs">
          <ElTabPane label="资产负债表" name="balance">
            <BalanceSheetTable :data="reportDetail.balanceSheet" />
          </ElTabPane>
          <ElTabPane label="利润表" name="profit">
            <ProfitSheetTable :data="reportDetail.profitSheet" />
          </ElTabPane>
          <ElTabPane label="现金流量表" name="cashflow">
            <CashFlowSheetTable :data="reportDetail.cashFlowSheet" />
          </ElTabPane>
        </ElTabs>
      </div>

      <div v-else-if="!loading" class="empty-tip">暂无财务报告数据</div>
    </div>
  </div>
</template>

<style scoped>
.finance-report-tab {
  padding: 4px 0;
}

/* 上排并排布局 */
.top-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 1100px) {
  .top-row {
    grid-template-columns: 1fr;
  }
}

.section {
  margin-bottom: 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
  background: var(--bg-card);
  backdrop-filter: blur(12px);
  position: relative;
  overflow: hidden;
  transition: border-color var(--transition-base);
}

.section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--accent-primary), transparent 70%);
  opacity: 0.6;
  transition: opacity var(--transition-base);
}

.section:hover::before {
  opacity: 1;
}

.section:hover {
  border-color: var(--border-color-light);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 10px;
}

.section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.3px;
}

.date-range-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.range-separator {
  color: var(--text-tertiary);
  font-size: 13px;
}
.year-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}
.report-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}
.empty-tip {
  color: var(--text-tertiary);
  padding: 40px 0;
  text-align: center;
  font-size: 13px;
}

/* 财务报表子 Tab */
.sheet-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}
</style>
