<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElTabs, ElTabPane, ElBreadcrumb, ElBreadcrumbItem, ElMessage, ElCard, ElLink, ElEmpty, ElTag } from 'element-plus'
import { OfficeBuilding, MapLocation, Calendar, Money, TrendCharts, Grid } from '@element-plus/icons-vue'
import { getCompanyDetail } from '@/api/company'
import type { CompanyDetail } from '@/types/company'
import { getMarketColor } from '@/utils/colors'
import FinanceReportTab from './finance/FinanceReportTab.vue'
import FundamentalAnalysisTab from './fundamental/FundamentalAnalysisTab.vue'

const route = useRoute()
const router = useRouter()

const stockCode = route.params.stockCode as string
const loading = ref(false)
const company = ref<CompanyDetail | null>(null)
const activeTab = ref('basic')

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getCompanyDetail(stockCode)
    company.value = res
  } catch (err) {
    ElMessage.error('加载公司详情失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDetail()
})

function getMarketBadgeClass(market?: string): string {
  switch (market?.toUpperCase()) {
    case 'SH': return 'market-sh'
    case 'SZ': return 'market-sz'
    case 'BJ': return 'market-bj'
    case 'HK': return 'market-hk'
    default: return 'market-default'
  }
}

function getListingStatusClass(status?: string): string {
  switch (status?.toLowerCase()) {
    case 'listed': return 'status-listed'
    case 'suspended': return 'status-suspended'
    case 'delisted': return 'status-delisted'
    default: return 'status-default'
  }
}

function getListingStatusLabel(status?: string): string {
  switch (status?.toLowerCase()) {
    case 'listed': return '上市中'
    case 'suspended': return '停牌'
    case 'delisted': return '退市'
    default: return status || '-'
  }
}
</script>

<template>
  <div class="company-detail" v-loading="loading">
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem :to="{ path: '/companies' }">公司信息</ElBreadcrumbItem>
      <ElBreadcrumbItem>{{ company?.stockName || stockCode }}</ElBreadcrumbItem>
    </ElBreadcrumb>

    <!-- 公司名片头部 -->
    <div v-if="company" class="company-header-card">
      <div class="header-main">
        <div class="header-left">
          <h1 class="company-name">{{ company.stockName }}</h1>
          <div class="company-meta">
            <span class="stock-code num-font">{{ company.stockCode }}</span>
            <span v-if="company.market" class="market-badge" :class="getMarketBadgeClass(company.market)">
              {{ company.market }}
            </span>
          </div>
        </div>
        <div class="header-right">
          <div v-if="company.industries && company.industries.length > 0" class="header-industries">
            <span
              v-for="ind in company.industries.filter(i => i.primary)"
              :key="ind.standardCode"
              class="industry-badge"
            >
              {{ ind.level2Name || ind.level1Name || '-' }}
            </span>
          </div>
          <span v-else-if="company.industry" class="industry-badge">{{ company.industry }}</span>
        </div>
      </div>
      <div class="header-divider" />
      <div class="header-stats">
        <div class="stat-item">
          <span class="stat-label">地区</span>
          <span class="stat-value">{{ company.region || '-' }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">成立日期</span>
          <span class="stat-value">{{ company.establishDate || '-' }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">上市日期</span>
          <span class="stat-value">{{ company.listingDate || '-' }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">注册资本</span>
          <span class="stat-value num-font">{{ company.registeredCapital ? company.registeredCapital + ' 万' : '-' }}</span>
        </div>
      </div>
    </div>

    <!-- Tab 内容区 -->
    <div class="tab-container">
      <ElTabs v-model="activeTab" class="detail-tabs">
        <ElTabPane label="基本信息" name="basic">
          <div v-if="company" class="info-content">
            <!-- 公司概况 -->
            <div class="info-section">
              <h3 class="section-heading">
                <OfficeBuilding class="section-icon" />
                公司概况
              </h3>
              <div class="info-cards">
                <div class="info-card">
                  <span class="card-label">股票代码</span>
                  <span class="card-value num-font">{{ company.stockCode }}</span>
                </div>
                <div class="info-card">
                  <span class="card-label">公司名称</span>
                  <span class="card-value">{{ company.stockName }}</span>
                </div>
                <div class="info-card">
                  <span class="card-label">所属行业</span>
                  <div class="card-value">
                    <template v-if="company.industries && company.industries.length > 0">
                      <div v-for="ind in company.industries" :key="ind.standardCode + ind.level2Code" class="industry-row">
                        <span class="industry-standard">{{ ind.standardName }}</span>
                        <ElLink
                          type="primary"
                          @click="router.push({ path: `/industries/${encodeURIComponent(ind.level2Code || '')}`, query: { standard: ind.standardCode } })"
                        >
                          {{ ind.level2Name || ind.level1Name || '-' }}
                        </ElLink>
                        <span v-if="!ind.primary" class="industry-secondary">次</span>
                      </div>
                    </template>
                    <ElLink
                      v-else-if="company.industry"
                      type="primary"
                      @click="router.push({ path: `/industries/${encodeURIComponent(company.industry)}`, query: { standard: 'EM' } })"
                    >
                      {{ company.industry }}
                    </ElLink>
                    <span v-else>-</span>
                  </div>
                </div>
                <div class="info-card">
                  <span class="card-label">地区</span>
                  <span class="card-value">{{ company.region || '-' }}</span>
                </div>
                <div class="info-card">
                  <span class="card-label">成立日期</span>
                  <span class="card-value">{{ company.establishDate || '-' }}</span>
                </div>
                <div class="info-card">
                  <span class="card-label">注册资本</span>
                  <span class="card-value num-font">{{ company.registeredCapital ? company.registeredCapital + ' 万元' : '-' }}</span>
                </div>
                <div class="info-card">
                  <span class="card-label">上市日期</span>
                  <span class="card-value">{{ company.listingDate || '-' }}</span>
                </div>
                <div class="info-card">
                  <span class="card-label">市场板块</span>
                  <span class="card-value">
                    <span class="market-badge" :class="getMarketBadgeClass(company.market)">
                      {{ company.market || '-' }}
                    </span>
                  </span>
                </div>
              </div>
            </div>
          </div>
        </ElTabPane>

        <ElTabPane label="关联证券" name="securities">
          <div v-if="company?.securities && company.securities.length > 0" class="securities-grid">
            <ElCard
              v-for="sec in company.securities"
              :key="sec.stockCode"
              class="security-card neon-border-cyan"
              shadow="hover"
            >
              <template #header>
                <div class="security-header">
                  <span class="security-name">{{ sec.stockName }}</span>
                  <span class="security-code num-font">{{ sec.stockCode }}</span>
                  <span class="market-badge" :class="getMarketBadgeClass(sec.market)">
                    {{ sec.market }}
                  </span>
                </div>
              </template>
              <div class="security-info">
                <div class="security-row">
                  <span class="security-label">证券类型</span>
                  <span class="security-value">{{ sec.securityType || '-' }}</span>
                </div>
                <div class="security-row">
                  <span class="security-label">上市日期</span>
                  <span class="security-value">{{ sec.listingDate || '-' }}</span>
                </div>
                <div class="security-row">
                  <span class="security-label">上市状态</span>
                  <span class="security-value">
                    <span class="status-badge" :class="getListingStatusClass(sec.listingStatus)">
                      {{ getListingStatusLabel(sec.listingStatus) }}
                    </span>
                  </span>
                </div>
              </div>
            </ElCard>
          </div>
          <ElEmpty v-else description="暂无关联证券数据" />
        </ElTabPane>

        <ElTabPane label="财务报告" name="finance">
          <FinanceReportTab :stock-code="stockCode" />
        </ElTabPane>

        <ElTabPane label="基本面分析" name="fundamental">
          <FundamentalAnalysisTab :stock-code="stockCode" />
        </ElTabPane>

        <ElTabPane label="历史变更" name="history">
          <ElEmpty description="历史变更记录开发中" />
        </ElTabPane>
      </ElTabs>
    </div>
  </div>
</template>

<style scoped>
.company-detail {
  padding: var(--page-padding);
}

/* ===== 公司名片头部 ===== */
.company-header-card {
  background: var(--bg-card);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin: 20px 0;
  position: relative;
  overflow: hidden;
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}

.company-header-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--accent-primary), var(--accent-pink));
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
}

.company-header-card:hover {
  border-color: var(--border-color-neon);
  box-shadow: var(--shadow-glow);
}

.header-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.company-name {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: 0.5px;
  text-shadow: none;
}

.company-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stock-code {
  font-size: 18px;
  font-weight: 600;
  color: var(--accent-primary);
  font-family: var(--font-mono);
}

.market-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  font-family: var(--font-mono);
}

.market-sh {
  background: var(--accent-primary-dim);
  color: var(--accent-primary);
  border: 1px solid rgba(43, 106, 255, 0.3);
}
.market-sz {
  background: var(--accent-pink-dim);
  color: var(--accent-pink);
  border: 1px solid rgba(139, 92, 246, 0.3);
}
.market-bj {
  background: var(--accent-warm-dim);
  color: var(--accent-warm);
  border: 1px solid rgba(245, 158, 11, 0.3);
}
.market-hk {
  background: var(--warning-color-dim);
  color: var(--warning-color);
  border: 1px solid rgba(255, 202, 58, 0.3);
}
.market-default {
  background: var(--bg-hover);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-industries {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.industry-badge {
  background: var(--bg-hover);
  border: 1px solid var(--border-color-light);
  color: var(--text-secondary);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
}

.header-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border-color-light), transparent);
  margin: 16px 0;
}

.header-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.stat-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

/* ===== Tab 容器 ===== */
.tab-container {
  background: var(--bg-card);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

/* ===== 基本信息 Tab ===== */
.info-content {
  padding: 4px 0;
}

.info-section {
  margin-bottom: 24px;
}

.section-heading {
  font-size: var(--section-title-size);
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  width: 20px;
  height: 20px;
  color: var(--accent-primary);
}

.info-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.info-card {
  background: var(--bg-card-solid);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: border-color var(--transition-base);
}

.info-card:hover {
  border-color: var(--border-color-light);
}

.card-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.card-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.industry-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.industry-standard {
  background: var(--accent-primary-dim);
  color: var(--accent-primary);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.industry-secondary {
  font-size: 11px;
  color: var(--text-muted);
  background: var(--bg-hover);
  padding: 1px 6px;
  border-radius: 4px;
}

/* ===== 关联证券 Tab ===== */
.securities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.security-card {
  cursor: default;
  transition: all var(--transition-base);
}

.security-card:hover {
  transform: translateY(-2px);
}

.security-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.security-name {
  font-weight: 600;
  font-size: 16px;
  color: var(--text-primary);
}

.security-code {
  color: var(--text-secondary);
  font-size: 14px;
}

.security-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.security-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.security-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.security-value {
  font-weight: 500;
  font-size: 14px;
  color: var(--text-primary);
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-listed {
  background: var(--success-color-dim);
  color: var(--success-color);
}
.status-suspended {
  background: var(--warning-color-dim);
  color: var(--warning-color);
}
.status-delisted {
  background: var(--danger-color-dim);
  color: var(--danger-color);
}
.status-default {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .company-header-card {
    padding: 16px;
  }
  .company-name {
    font-size: 22px;
  }
  .header-stats {
    gap: 16px;
  }
  .info-cards {
    grid-template-columns: 1fr;
  }
}
</style>
