<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElCollapse, ElCollapseItem, ElLink, ElEmpty, ElBreadcrumb, ElBreadcrumbItem, ElSelect, ElOption } from 'element-plus'
import { getFundamentalOverview, getIndustryPeers, getIndustryRank } from '@/api/research'
import type { FundamentalOverview, AnnualMetric, PeerMetric, IndustryRankItem } from '@/types/research'
import FundamentalScreener from './components/FundamentalScreener.vue'
import MetricDashboard from './components/MetricDashboard.vue'
import IndustryPeersTable from './components/IndustryPeersTable.vue'
import IndustryRankTable from './components/IndustryRankTable.vue'
import ProfitabilityChart from './components/ProfitabilityChart.vue'
import CostExpenseChart from './components/CostExpenseChart.vue'
import BalanceSheetChart from './components/BalanceSheetChart.vue'
import CashFlowChart from './components/CashFlowChart.vue'
import DupontAnalysisChart from './components/DupontAnalysisChart.vue'

const router = useRouter()

const selectedStock = ref('')
const loading = ref(false)
const overview = ref<FundamentalOverview | null>(null)
const peers = ref<PeerMetric[]>([])
const rankData = ref<IndustryRankItem[]>([])
const rankSortBy = ref('roe')
const rankOrder = ref('desc')
const activeCollapse = ref<string[]>([])
const selectedYear = ref<number | null>(null)

// 对比池（最多5家）
const comparePool = ref<{ stockCode: string; stockName: string }[]>([])

function addToCompare(stockCode: string) {
  if (comparePool.value.length >= 5) {
    ElMessage.warning('对比池最多支持5家公司')
    return
  }
  if (comparePool.value.some(c => c.stockCode === stockCode)) {
    ElMessage.info('该公司已在对比池中')
    return
  }
  // 尝试从 peers 或 company list 中获取名称
  const name = peers.value.find(p => p.stockCode === stockCode)?.stockName || stockCode
  comparePool.value.push({ stockCode, stockName: name })
}

function removeFromCompare(index: number) {
  comparePool.value.splice(index, 1)
}

function clearCompare() {
  comparePool.value = []
}

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
    { label: '营业总收入', value: formatMoney(m.totalRevenue), color: '#00d4ff', yoyValue: revenueYoy.value, yoyDirection: revenueYoy.direction },
    { label: '归母净利润', value: formatMoney(m.parentNetProfit), color: '#67c23a', yoyValue: profitYoy.value, yoyDirection: profitYoy.direction },
    { label: '毛利率', value: formatPercent(m.grossMargin), color: '#409eff' },
    { label: '净利率', value: formatPercent(m.netMargin), color: '#67c23a' },
    { label: 'ROE', value: formatPercent(m.roe), color: '#e6a23c' },
    { label: '总资产', value: formatMoney(m.totalAssets), color: '#ff9500' },
    { label: '资产负债率', value: formatPercent(m.debtRatio), color: '#f56c6c' },
    { label: '经营现金流/净利润', value: formatPercent(m.cashflowProfitRatio), color: '#9ca3af' },
  ]
})

async function onRankSort(field: string) {
  const nextOrder = rankSortBy.value === field && rankOrder.value === 'desc' ? 'asc' : 'desc'
  rankSortBy.value = field
  rankOrder.value = nextOrder
  try {
    const res = await getIndustryRank(selectedStock.value, field, nextOrder)
    rankData.value = res.items
  } catch (err) {
    ElMessage.error('排序加载失败')
  }
}

async function onSelectCompany(stockCode: string) {
  selectedStock.value = stockCode
  loading.value = true
  try {
    const [overviewRes, peersRes, rankRes] = await Promise.all([
      getFundamentalOverview(stockCode),
      getIndustryPeers(stockCode),
      getIndustryRank(stockCode),
    ])
    overview.value = overviewRes
    peers.value = peersRes.peers
    rankData.value = rankRes.items
    // 默认选中最新年份
    if (overviewRes.metrics?.length) {
      selectedYear.value = overviewRes.metrics[overviewRes.metrics.length - 1].reportYear
    } else {
      selectedYear.value = null
    }
  } catch (err) {
    ElMessage.error('加载数据失败')
    overview.value = null
    peers.value = []
    rankData.value = []
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
</script>

<template>
  <div class="research-page">
    <ElBreadcrumb separator="/" class="page-breadcrumb">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem>投研分析</ElBreadcrumbItem>
    </ElBreadcrumb>

    <div class="research-layout">
      <!-- 左侧边栏 -->
      <aside class="research-sidebar">
        <FundamentalScreener @select="onSelectCompany" @add-compare="addToCompare" />
      </aside>

      <!-- 右侧主区域 -->
      <main class="research-main" v-loading="loading">
        <!-- 引导态 -->
      <ElEmpty v-if="!selectedStock" description="请输入股票代码或选择左侧筛选条件开始分析" />

      <template v-else-if="overview">
        <!-- 公司信息栏 -->
        <div class="company-header">
          <div class="company-title">
            <span class="title-name">{{ overview.stockName }}</span>
            <span class="title-code">({{ overview.stockCode }})</span>
            <span class="title-tags">
              <span class="tag">{{ overview.industry || '-' }}</span>
              <span class="tag">{{ overview.market || '-' }}</span>
            </span>
          </div>
          <ElLink type="primary" @click="router.push(`/companies/${overview.stockCode}`)">
            查看公司详情 →
          </ElLink>
        </div>

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

        <!-- 对比池 -->
        <div v-if="comparePool.length > 0" class="compare-pool-bar">
          <span class="compare-label">对比池：</span>
          <div class="compare-tags">
            <span
              v-for="(item, idx) in comparePool"
              :key="item.stockCode"
              class="compare-tag"
            >
              {{ item.stockName }}
              <span class="compare-remove" @click="removeFromCompare(idx)">×</span>
            </span>
          </div>
          <button class="compare-clear" @click="clearCompare">清空</button>
        </div>

        <!-- 图表区 -->
        <div class="charts-container">
          <ProfitabilityChart :metrics="overview.metrics" />
          <CostExpenseChart :metrics="overview.metrics" />
          <BalanceSheetChart :metrics="overview.metrics" />
          <CashFlowChart :metrics="overview.metrics" />
          <DupontAnalysisChart :metrics="overview.metrics" />
        </div>

        <!-- 同行业公司速览 + 行业排名 -->
        <ElCollapse v-model="activeCollapse" style="margin-top: 24px">
          <ElCollapseItem title="同行业公司速览" name="peers">
            <IndustryPeersTable :peers="peers" @select="onSelectCompany" />
          </ElCollapseItem>
          <ElCollapseItem title="行业排名" name="rank">
            <IndustryRankTable
              :data="rankData"
              :current-stock-code="selectedStock"
              :sort-by="rankSortBy"
              :order="rankOrder"
              @select="onSelectCompany"
              @sort="onRankSort"
            />
          </ElCollapseItem>
        </ElCollapse>
      </template>

      <ElEmpty v-else description="暂无数据" />
      </main>
    </div>
  </div>
</template>

<style scoped>
.research-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  overflow: hidden;
}
.page-breadcrumb {
  padding: 16px 16px 0;
  flex-shrink: 0;
}
.research-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  flex: 1;
  overflow: hidden;
}
.research-sidebar {
  overflow-y: auto;
  border-right: 1px solid var(--border-color, rgba(255,255,255,0.06));
  padding: 16px;
}
.research-main {
  overflow-y: auto;
  padding: 16px;
}
.company-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
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
.company-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary, #e5e7eb);
}
.title-code {
  font-size: 14px;
  color: var(--text-secondary, #9ca3af);
}
.title-tags {
  display: flex;
  gap: 6px;
  margin-left: 8px;
}
.tag {
  font-size: 12px;
  color: var(--text-secondary, #9ca3af);
  background: rgba(255,255,255,0.06);
  padding: 2px 8px;
  border-radius: 4px;
}
.charts-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.compare-pool-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.compare-label {
  font-size: 13px;
  color: var(--text-secondary, #9ca3af);
}
.compare-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.compare-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
  border: 1px solid rgba(64, 158, 255, 0.25);
}
.compare-remove {
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  opacity: 0.7;
}
.compare-remove:hover {
  opacity: 1;
}
.compare-clear {
  background: transparent;
  border: none;
  color: var(--text-secondary, #9ca3af);
  font-size: 12px;
  cursor: pointer;
  margin-left: auto;
}
.compare-clear:hover {
  color: #f56c6c;
}
@media (max-width: 768px) {
  .research-layout {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }
  .research-sidebar {
    border-right: none;
    border-bottom: 1px solid var(--border-color, rgba(255,255,255,0.06));
    max-height: 300px;
  }
}
</style>
