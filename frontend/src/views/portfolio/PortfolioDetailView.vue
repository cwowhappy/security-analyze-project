<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getPortfolio, getPortfolioSummary } from '@/api/portfolio'
import type { Portfolio, PortfolioSummary } from '@/types/portfolio'
import PositionTab from './PositionTab.vue'
import TransactionTab from './TransactionTab.vue'
import AnalysisTab from './AnalysisTab.vue'

const route = useRoute()
const router = useRouter()
const portfolioId = Number(route.params.id)

const portfolio = ref<Portfolio | null>(null)
const summary = ref<PortfolioSummary | null>(null)
const loading = ref(false)
const activeTab = ref('position')
const positionTabRef = ref<InstanceType<typeof PositionTab>>()
const transactionTabRef = ref<InstanceType<typeof TransactionTab>>()
const analysisTabRef = ref<InstanceType<typeof AnalysisTab>>()

async function fetchPortfolio() {
  try {
    portfolio.value = await getPortfolio(portfolioId)
  } catch {
    ElMessage.error('加载组合信息失败')
  }
}

async function fetchSummary() {
  try {
    summary.value = await getPortfolioSummary(portfolioId)
  } catch {
    // ignore
  }
}

async function loadAll() {
  loading.value = true
  await Promise.all([fetchPortfolio(), fetchSummary()])
  loading.value = false
}

function handleTransactionChanged() {
  fetchSummary()
  if (positionTabRef.value) {
    positionTabRef.value.refresh()
  }
  if (analysisTabRef.value) {
    analysisTabRef.value.refresh()
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="portfolio-detail" v-loading="loading">
    <div class="detail-header">
      <el-button link :icon="ArrowLeft" @click="router.push('/portfolios')">返回</el-button>
      <h2 v-if="portfolio">{{ portfolio.name }}</h2>
      <el-tag v-if="portfolio" :type="portfolio.type === 'REAL' ? 'success' : 'info'">
        {{ portfolio.type === 'REAL' ? '实盘' : '模拟盘' }}
      </el-tag>
    </div>

    <div v-if="summary" class="summary-cards">
      <el-row :gutter="16">
        <el-col :span="4">
          <el-card>
            <div class="card-label">总市值</div>
            <div class="card-value">{{ summary.totalMarketValue?.toFixed(2) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card>
            <div class="card-label">总成本</div>
            <div class="card-value">{{ summary.totalCost?.toFixed(2) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card>
            <div class="card-label">浮动盈亏</div>
            <div class="card-value" :class="(summary.totalFloatingPnl ?? 0) >= 0 ? 'up' : 'down'">
              {{ summary.totalFloatingPnl?.toFixed(2) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card>
            <div class="card-label">已实现盈亏</div>
            <div class="card-value" :class="summary.totalRealizedPnl >= 0 ? 'up' : 'down'">
              {{ summary.totalRealizedPnl?.toFixed(2) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card>
            <div class="card-label">持仓数</div>
            <div class="card-value">{{ summary.holdingCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card>
            <div class="card-label">最近交易</div>
            <div class="card-value">{{ summary.latestTradeDate || '-' }}</div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="持仓" name="position">
        <PositionTab ref="positionTabRef" :portfolio-id="portfolioId" />
      </el-tab-pane>
      <el-tab-pane label="成交记录" name="transaction">
        <TransactionTab ref="transactionTabRef" :portfolio-id="portfolioId" @changed="handleTransactionChanged" />
      </el-tab-pane>
      <el-tab-pane label="分析" name="analysis">
        <AnalysisTab ref="analysisTabRef" :portfolio-id="portfolioId" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.portfolio-detail {
  padding: 8px;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.detail-header h2 {
  margin: 0;
  color: var(--text-primary);
}
.summary-cards {
  margin-bottom: 24px;
}
.card-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.card-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  font-family: var(--font-mono);
}
.up { color: var(--up-color); }
.down { color: var(--down-color); }
.detail-tabs {
  margin-top: 16px;
}
</style>
