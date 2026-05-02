<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { ElTabs, ElTabPane, ElSelect, ElOption, ElMessage } from 'element-plus'
import { getFinanceReports, getFinanceReportDetail, getFinanceIndicators } from '@/api/finance'
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
const indicators = ref<IndicatorMetric[]>([])

// 可用的报告期选项（倒序，最新的在前）
const reportDateOptions = computed(() => {
  return reports.value.map((r) => ({
    value: r.reportDate,
    label: `${r.reportDate} ${r.reportType}`,
  }))
})

// 校验：endDate 不能早于 startDate
const endDateDisabled = computed(() => {
  return (date: string) => {
    if (!trendStartDate.value) return false
    return date < trendStartDate.value
  }
})

async function fetchIndicators() {
  try {
    const res = await getFinanceIndicators(
      props.stockCode,
      ['totalRevenue', 'netProfit', 'grossMargin', 'netMargin', 'debtRatio'],
      trendStartDate.value || undefined,
      trendEndDate.value || undefined
    )
    indicators.value = res.metrics
  } catch (err) {
    console.error('加载指标趋势失败', err)
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

watch([trendStartDate, trendEndDate], () => {
  fetchIndicators()
})

onMounted(() => {
  fetchReports().then(() => {
    fetchIndicators()
  })
})
</script>

<template>
  <div class="finance-report-tab" v-loading="loading">
    <!-- ========== 核心指标趋势分析区域 ========== -->
    <div class="section trend-section">
      <div class="section-header">
        <h4 class="section-title">核心指标趋势分析</h4>
        <div class="date-range-selector">
          <ElSelect
            v-model="trendStartDate"
            placeholder="起始报告期"
            clearable
            style="width: 180px"
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
            placeholder="结束报告期"
            clearable
            style="width: 180px"
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

    <!-- ========== 选定报告期详情区域 ========== -->
    <div class="section detail-section">
      <div class="section-header">
        <h4 class="section-title">选定报告期详情</h4>
        <div class="report-selector">
          <ElSelect v-model="selectedReportId" placeholder="选择报告期" style="width: 200px">
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

        <ElTabs v-model="activeSheetTab" type="border-card">
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
  padding: 16px 0;
}

.section {
  margin-bottom: 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  background: #fff;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.date-range-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.range-separator {
  color: #999;
  font-size: 14px;
}

.report-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-tip {
  color: #999;
  padding: 40px 0;
  text-align: center;
}
</style>
