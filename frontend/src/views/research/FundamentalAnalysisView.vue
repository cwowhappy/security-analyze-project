<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElMessage,
  ElLink,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElTabs,
  ElTabPane,
  ElAutocomplete,
  ElButton,
} from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getCompanyList } from '@/api/company'
import type { CompanyListResponse } from '@/types/company'
import FundamentalChartsTab from './tabs/FundamentalChartsTab.vue'
import ValuationAnalysisTab from './tabs/ValuationAnalysisTab.vue'
import PeerComparisonTab from './tabs/PeerComparisonTab.vue'

const router = useRouter()
const route = useRoute()

const selectedStock = ref('')
const stockName = ref('')
const industry = ref('')
const market = ref('')
const loading = ref(false)
const activeTab = ref('fundamental')
const searchKeyword = ref('')

// 对比池（最多5家）
const comparePool = ref<{ stockCode: string; stockName: string }[]>([])

interface SuggestItem {
  value: string
  stockCode: string
  stockName: string
  market?: string
}

let debounceTimer: ReturnType<typeof setTimeout> | null = null

function fetchSuggestions(queryString: string, callback: (data: SuggestItem[]) => void) {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    const trimmed = queryString.trim()
    if (!trimmed) {
      callback([])
      return
    }
    try {
      const res: CompanyListResponse = await getCompanyList({
        keyword: trimmed,
        page: 0,
        size: 10,
      })
      const items = res.items.map((item) => ({
        value: `${item.stockCode} ${item.stockName}`,
        stockCode: item.stockCode,
        stockName: item.stockName,
        market: item.market,
      }))
      callback(items)
    } catch {
      callback([])
    }
  }, 200)
}

async function onAnalyze(stockCode?: string) {
  const code = stockCode || searchKeyword.value.trim().split(' ')[0]
  if (!code) {
    ElMessage.warning('请输入股票代码或公司名称')
    return
  }
  await loadCompany(code)
}

async function loadCompany(stockCode: string) {
  loading.value = true
  try {
    const res: CompanyListResponse = await getCompanyList({
      keyword: stockCode,
      page: 0,
      size: 1,
    })
    const company = res.items.find((c) => c.stockCode === stockCode) || res.items[0]
    if (!company) {
      ElMessage.error('未找到该公司')
      return
    }
    selectedStock.value = company.stockCode
    stockName.value = company.stockName
    industry.value = company.industry || ''
    market.value = company.market || ''
    searchKeyword.value = `${company.stockCode} ${company.stockName}`

    // 更新 URL，便于分享
    const url = new URL(window.location.href)
    url.searchParams.set('stockCode', company.stockCode)
    window.history.replaceState({}, '', url.toString())
  } catch (err) {
    ElMessage.error('加载公司信息失败')
  } finally {
    loading.value = false
  }
}

function handleSelect(item: SuggestItem) {
  onAnalyze(item.stockCode)
}

function backToEntry() {
  selectedStock.value = ''
  stockName.value = ''
  industry.value = ''
  market.value = ''
  searchKeyword.value = ''
  comparePool.value = []
  activeTab.value = 'fundamental'

  const url = new URL(window.location.href)
  url.searchParams.delete('stockCode')
  window.history.replaceState({}, '', url.toString())
}

function addToCompare(stockCode: string, name?: string) {
  if (comparePool.value.length >= 5) {
    ElMessage.warning('对比池最多支持5家公司')
    return
  }
  if (comparePool.value.some((c) => c.stockCode === stockCode)) {
    ElMessage.info('该公司已在对比池中')
    return
  }
  comparePool.value.push({ stockCode, stockName: name || stockCode })
}

function removeFromCompare(index: number) {
  comparePool.value.splice(index, 1)
}

function clearCompare() {
  comparePool.value = []
}

function onPeerSelect(stockCode: string) {
  loadCompany(stockCode)
}

onMounted(() => {
  const queryCode = route.query.stockCode as string
  if (queryCode) {
    loadCompany(queryCode)
  }
})
</script>

<template>
  <div class="research-page">
    <!-- ========== 入口视图：未选中公司 ========== -->
    <template v-if="!selectedStock">
      <ElBreadcrumb separator="/" class="page-breadcrumb">
        <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
        <ElBreadcrumbItem>投研分析</ElBreadcrumbItem>
      </ElBreadcrumb>

      <h2 class="page-title">投研分析</h2>

      <div class="search-section">
        <div class="search-box">
          <ElAutocomplete
            v-model="searchKeyword"
            :fetch-suggestions="fetchSuggestions"
            placeholder="输入股票代码或公司名称"
            clearable
            style="width: 480px"
            :highlight-first-item="false"
            @select="handleSelect"
            @keyup.enter="onAnalyze()"
          >
            <template #prefix>
              <Search style="width: 16px; height: 16px; color: var(--text-tertiary)" />
            </template>
            <template #default="{ item }">
              <div class="suggest-item">
                <span class="suggest-code">{{ item.stockCode }}</span>
                <span class="suggest-name">{{ item.stockName }}</span>
                <span v-if="item.market" class="suggest-market">[{{ item.market }}]</span>
              </div>
            </template>
          </ElAutocomplete>
          <ElButton type="primary" @click="onAnalyze()">开始分析</ElButton>
        </div>
        <div class="search-hint">
          例如：600519（贵州茅台）、000001（平安银行）、002594（比亚迪）
        </div>
      </div>
    </template>

    <!-- ========== 分析视图：已选中公司 ========== -->
    <template v-else>
      <ElBreadcrumb separator="/" class="page-breadcrumb">
        <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
        <ElBreadcrumbItem>
          <ElLink type="primary" :underline="false" @click="backToEntry">
            投研分析
          </ElLink>
        </ElBreadcrumbItem>
        <ElBreadcrumbItem>{{ stockName }}</ElBreadcrumbItem>
      </ElBreadcrumb>

      <!-- 公司信息栏 -->
      <div class="company-header">
        <div class="company-title">
          <span class="title-name">{{ stockName }}</span>
          <span class="title-code">({{ selectedStock }})</span>
          <span class="title-tags">
            <span class="tag">{{ industry || '-' }}</span>
            <span class="tag">{{ market || '-' }}</span>
          </span>
        </div>
        <ElLink type="primary" @click="router.push(`/companies/${selectedStock}`)">
          查看公司详情 →
        </ElLink>
      </div>

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

      <ElTabs v-model="activeTab" type="border-card" class="research-tabs">
        <ElTabPane label="公司基本面数据图表" name="fundamental">
          <FundamentalChartsTab :stock-code="selectedStock" />
        </ElTabPane>

        <ElTabPane label="估值分析" name="valuation">
          <ValuationAnalysisTab :stock-code="selectedStock" />
        </ElTabPane>

        <ElTabPane label="同行对比分析" name="peer">
          <PeerComparisonTab :stock-code="selectedStock" @select="onPeerSelect" />
        </ElTabPane>
      </ElTabs>
    </template>
  </div>
</template>

<style scoped>
.research-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  overflow: hidden;
  padding: 0 16px 16px;
}
.page-breadcrumb {
  padding: 16px 0 0;
  flex-shrink: 0;
}

/* 入口视图 */
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 16px 0;
}

/* 搜索区域：参考公司信息页面 */
.search-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 32px 0 40px;
  padding: 32px 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 12px;
}
.search-hint {
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-tertiary);
}
.suggest-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
}
.suggest-code {
  font-weight: 600;
  color: var(--accent-primary);
  min-width: 80px;
}
.suggest-name {
  flex: 1;
  color: var(--text-primary);
}
.suggest-market {
  color: var(--text-tertiary);
  font-size: 12px;
}

/* 分析视图 */
.company-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0;
  flex-shrink: 0;
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
  background: rgba(255, 255, 255, 0.06);
  padding: 2px 8px;
  border-radius: 4px;
}
.compare-pool-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  flex-shrink: 0;
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
  background: var(--accent-primary-dim);
  color: var(--accent-primary);
  border: 1px solid var(--border-color-neon);
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
  color: var(--danger-color);
}
.research-tabs {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.research-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

@media (max-width: 768px) {
  .search-section {
    margin: 16px 0 24px;
    padding: 24px 16px;
  }
  .search-box {
    flex-direction: column;
    align-items: stretch;
    width: 100%;
  }
  .search-box .el-autocomplete {
    width: 100% !important;
  }
}
</style>
