<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElCollapse, ElCollapseItem, ElLink, ElEmpty } from 'element-plus'
import { getFundamentalOverview, getIndustryPeers } from '@/api/research'
import type { FundamentalOverview, AnnualMetric, PeerMetric } from '@/types/research'
import FundamentalScreener from './components/FundamentalScreener.vue'
import ProfitabilityChart from './components/ProfitabilityChart.vue'
import CostExpenseChart from './components/CostExpenseChart.vue'
import BalanceSheetChart from './components/BalanceSheetChart.vue'
import CashFlowChart from './components/CashFlowChart.vue'

const router = useRouter()

const selectedStock = ref('')
const loading = ref(false)
const overview = ref<FundamentalOverview | null>(null)
const peers = ref<PeerMetric[]>([])
const activeCollapse = ref<string[]>([])

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
    { label: '毛利率', value: formatPercent(m.grossMargin), color: '#409eff' },
    { label: '净利率', value: formatPercent(m.netMargin), color: '#67c23a' },
    { label: 'ROE', value: formatPercent(m.roe), color: '#e6a23c' },
    { label: '总资产', value: formatMoney(m.totalAssets), color: '#ff9500' },
    { label: '资产负债率', value: formatPercent(m.debtRatio), color: '#f56c6c' },
    { label: '经营现金流/净利润', value: formatPercent(m.cashflowProfitRatio), color: '#9ca3af' },
  ]
})

async function onSelectCompany(stockCode: string) {
  selectedStock.value = stockCode
  loading.value = true
  try {
    const [overviewRes, peersRes] = await Promise.all([
      getFundamentalOverview(stockCode),
      getIndustryPeers(stockCode),
    ])
    overview.value = overviewRes
    peers.value = peersRes.peers
  } catch (err) {
    ElMessage.error('加载数据失败')
    overview.value = null
    peers.value = []
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
  <div class="research-layout">
    <!-- 左侧边栏 -->
    <aside class="research-sidebar">
      <FundamentalScreener @select="onSelectCompany" />
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

        <!-- 核心指标仪表盘 -->
        <div class="metric-dashboard">
          <div
            v-for="card in metricCards"
            :key="card.label"
            class="dashboard-card"
            :style="{ borderTop: `3px solid ${card.color}` }"
          >
            <div class="dashboard-label">{{ card.label }}</div>
            <div class="dashboard-value" :style="{ color: card.color }">{{ card.value }}</div>
          </div>
        </div>

        <!-- 图表区 -->
        <div class="charts-container">
          <ProfitabilityChart :metrics="overview.metrics" />
          <CostExpenseChart :metrics="overview.metrics" />
          <BalanceSheetChart :metrics="overview.metrics" />
          <CashFlowChart :metrics="overview.metrics" />

          <!-- 杜邦分析占位 -->
          <div class="dupont-placeholder">
            <div class="placeholder-title">杜邦分析拆解</div>
            <div class="placeholder-desc">阶段B即将上线：ROE = 净利率 × 资产周转率 × 权益乘数</div>
          </div>
        </div>

        <!-- 同行业公司速览 -->
        <ElCollapse v-model="activeCollapse" style="margin-top: 24px">
          <ElCollapseItem title="同行业公司速览" name="peers">
            <table v-if="peers.length > 0" class="peers-table">
              <thead>
                <tr>
                  <th>股票代码</th>
                  <th>公司名称</th>
                  <th>营收</th>
                  <th>净利润</th>
                  <th>ROE</th>
                  <th>负债率</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="peer in peers"
                  :key="peer.stockCode"
                  class="peer-row"
                  @click="onSelectCompany(peer.stockCode)"
                >
                  <td>{{ peer.stockCode }}</td>
                  <td>{{ peer.stockName }}</td>
                  <td>{{ formatMoney(peer.totalRevenue) }}</td>
                  <td>{{ formatMoney(peer.parentNetProfit) }}</td>
                  <td>{{ formatPercent(peer.roe) }}</td>
                  <td>{{ formatPercent(peer.debtRatio) }}</td>
                </tr>
              </tbody>
            </table>
            <ElEmpty v-else description="暂无同行业对比数据" />
          </ElCollapseItem>
        </ElCollapse>
      </template>

      <ElEmpty v-else description="暂无数据" />
    </main>
  </div>
</template>

<style scoped>
.research-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  height: calc(100vh - 64px);
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
.metric-dashboard {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.dashboard-card {
  background: var(--card-bg, rgba(255,255,255,0.03));
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}
.dashboard-label {
  font-size: 13px;
  color: var(--text-secondary, #9ca3af);
  margin-bottom: 8px;
}
.dashboard-value {
  font-size: 18px;
  font-weight: 600;
  font-family: var(--font-mono, monospace);
}
.charts-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.dupont-placeholder {
  height: 360px;
  background: var(--card-bg, rgba(255,255,255,0.03));
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary, #9ca3af);
  border: 1px dashed var(--border-color, rgba(255,255,255,0.1));
}
.placeholder-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
}
.placeholder-desc {
  font-size: 13px;
}
.peers-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.peers-table th,
.peers-table td {
  padding: 10px 12px;
  text-align: right;
  border-bottom: 1px solid var(--border-color, rgba(255,255,255,0.06));
}
.peers-table th {
  color: var(--text-secondary, #9ca3af);
  font-weight: 500;
}
.peers-table td {
  color: var(--text-primary, #e5e7eb);
}
.peer-row {
  cursor: pointer;
}
.peer-row:hover {
  background: rgba(255,255,255,0.03);
}
</style>
