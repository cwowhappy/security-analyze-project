<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElAutocomplete,
  ElButton,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElTable,
  ElTableColumn,
  ElPagination,
  ElMessage,
  ElCard,
  ElTag,
  ElEmpty,
} from 'element-plus'
import { Search, Star, StarFilled } from '@element-plus/icons-vue'
import { getCompanyList } from '@/api/company'
import type { Company, CompanyListResponse } from '@/types/company'

const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const tableData = ref<Company[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(20)
const hasSearched = ref(false)

// 重点关注公司（本地暂存，待后端设计完成后替换为接口）
const favoriteCompanies = ref<Company[]>([])
const favoriteLoading = ref(false)

const FAVORITE_STORAGE_KEY = 'favorite_companies'

function loadFavoritesFromStorage(): string[] {
  try {
    const raw = localStorage.getItem(FAVORITE_STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveFavoritesToStorage(codes: string[]) {
  localStorage.setItem(FAVORITE_STORAGE_KEY, JSON.stringify(codes))
}

function isFavorite(stockCode: string): boolean {
  return loadFavoritesFromStorage().includes(stockCode)
}

function toggleFavorite(company: Company) {
  const codes = loadFavoritesFromStorage()
  const idx = codes.indexOf(company.stockCode)
  if (idx > -1) {
    codes.splice(idx, 1)
    ElMessage.success(`已取消关注 ${company.stockName}`)
  } else {
    codes.push(company.stockCode)
    ElMessage.success(`已关注 ${company.stockName}`)
  }
  saveFavoritesToStorage(codes)
  refreshFavorites()
}

async function refreshFavorites() {
  const codes = loadFavoritesFromStorage()
  if (codes.length === 0) {
    favoriteCompanies.value = []
    return
  }
  // 临时方案：从搜索结果或已加载数据中匹配；如无则批量查询（这里简化处理，直接按代码搜索）
  // 待后端设计完成后，应替换为专用的批量查询接口如 GET /companies/favorites
  favoriteLoading.value = true
  try {
    // 尝试用已缓存的搜索数据匹配
    const cached = tableData.value.filter((c) => codes.includes(c.stockCode))
    const cachedCodes = cached.map((c) => c.stockCode)
    const missing = codes.filter((c) => !cachedCodes.includes(c))

    if (missing.length > 0) {
      // 简单兜底：逐个查询（实际应使用批量接口）
      for (const code of missing) {
        try {
          const res: CompanyListResponse = await getCompanyList({ keyword: code, page: 0, size: 1 })
          if (res.items.length > 0) cached.push(res.items[0])
        } catch {
          // ignore
        }
      }
    }
    favoriteCompanies.value = codes
      .map((code) => cached.find((c) => c.stockCode === code))
      .filter(Boolean) as Company[]
  } finally {
    favoriteLoading.value = false
  }
}

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

async function handleSearch() {
  const trimmed = keyword.value.trim()
  if (!trimmed) {
    hasSearched.value = false
    tableData.value = []
    return
  }
  loading.value = true
  try {
    const res: CompanyListResponse = await getCompanyList({
      keyword: trimmed,
      page: page.value,
      size: size.value,
    })
    tableData.value = res.items
    total.value = res.total
    hasSearched.value = true
  } catch (err) {
    ElMessage.error('加载数据失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

function handleSelect(item: SuggestItem) {
  router.push(`/companies/${item.stockCode}`)
}

function handleRowClick(row: Company) {
  router.push(`/companies/${row.stockCode}`)
}

function handleKeyEnter() {
  page.value = 0
  handleSearch()
}

function handleClear() {
  hasSearched.value = false
  tableData.value = []
}

onMounted(() => {
  refreshFavorites()
})
</script>

<template>
  <div class="company-search">
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem>公司信息</ElBreadcrumbItem>
    </ElBreadcrumb>

    <h2 class="page-title">公司信息</h2>

    <!-- 上方：公司搜索（居中） -->
    <div class="search-section">
      <div class="search-box">
        <ElAutocomplete
          v-model="keyword"
          :fetch-suggestions="fetchSuggestions"
          placeholder="输入股票代码或公司名称"
          clearable
          style="width: 480px"
          :highlight-first-item="false"
          @select="handleSelect"
          @keyup.enter="handleKeyEnter"
          @clear="handleClear"
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
        <ElButton type="primary" @click="handleKeyEnter">
          搜索
        </ElButton>
      </div>
      <div class="search-hint">例如：600519（贵州茅台）、000001（平安银行）、002594（比亚迪）</div>
    </div>

    <!-- 搜索结果 -->
    <div v-if="hasSearched" v-loading="loading" class="result-area">
      <ElTable
        :data="tableData"
        stripe
        style="width: 100%"
        highlight-current-row
        @row-click="handleRowClick"
      >
        <ElTableColumn prop="stockCode" label="股票代码" width="120" />
        <ElTableColumn prop="stockName" label="公司名称" min-width="180" />
        <ElTableColumn prop="industry" label="所属行业" width="150" />
        <ElTableColumn prop="region" label="地区" width="120" />
        <ElTableColumn prop="listingDate" label="上市日期" width="120" />
        <ElTableColumn prop="market" label="市场" width="80" />
        <ElTableColumn label="操作" width="100" align="center">
          <template #default="{ row }">
            <button
              class="fav-btn"
              :class="{ active: isFavorite(row.stockCode) }"
              @click.stop="toggleFavorite(row)"
            >
              <el-icon><component :is="isFavorite(row.stockCode) ? StarFilled : Star" /></el-icon>
              <span>{{ isFavorite(row.stockCode) ? '已关注' : '关注' }}</span>
            </button>
          </template>
        </ElTableColumn>
      </ElTable>

      <div class="pagination">
        <ElPagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handleSearch"
          @size-change="page = 0; handleSearch()"
        />
      </div>
    </div>

    <!-- 下方：重点关注公司 -->
    <section class="favorite-section" v-loading="favoriteLoading">
      <div class="section-title">重点关注公司</div>

      <div v-if="favoriteCompanies.length > 0" class="company-cards">
        <ElCard
          v-for="item in favoriteCompanies"
          :key="item.stockCode"
          class="company-card"
          shadow="hover"
          @click="handleRowClick(item)"
        >
          <div class="card-header">
            <span class="company-name">{{ item.stockName }}</span>
            <ElTag size="small" type="info">{{ item.market }}</ElTag>
          </div>
          <div class="card-code">{{ item.stockCode }}</div>
          <div v-if="item.industry" class="card-info">{{ item.industry }}</div>
          <div v-if="item.region" class="card-info">{{ item.region }}</div>
          <div class="card-actions">
            <ElButton
              link
              type="warning"
              size="small"
              :icon="StarFilled"
              @click.stop="toggleFavorite(item)"
            >
              取消关注
            </ElButton>
          </div>
        </ElCard>
      </div>

      <div v-else class="empty-favorite">
        <ElEmpty description="暂无重点关注公司">
          <template #description>
            <div class="empty-desc">
              <p>暂无重点关注公司</p>
              <p class="empty-tip">在搜索结果中点击“关注”即可添加至此</p>
            </div>
          </template>
        </ElEmpty>
      </div>
    </section>
  </div>
</template>

<style scoped>
.company-search {
  padding: var(--page-padding);
}

.fav-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-base);
}
.fav-btn:hover {
  border-color: var(--accent-warm);
  color: var(--accent-warm);
}
.fav-btn.active {
  background: var(--accent-warm-dim);
  border-color: var(--accent-warm);
  color: var(--accent-warm);
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 16px 0;
}

/* 搜索区域：居中 */
.search-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 32px 0 40px;
  padding: 32px 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  transition: border-color var(--transition-base);
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

/* 搜索结果 */
.result-area {
  margin-bottom: 32px;
}
.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 重点关注公司 */
.favorite-section {
  margin-bottom: 32px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}
.company-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.company-card {
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}
.company-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent-primary);
  box-shadow: var(--shadow-glow);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.company-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.card-code {
  font-size: 13px;
  color: var(--accent-primary);
  font-family: var(--font-mono);
  margin-bottom: 4px;
}
.card-info {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 2px;
}
.card-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}
.empty-favorite {
  padding: 24px 0;
}
.empty-desc {
  text-align: center;
  color: var(--text-secondary);
}
.empty-desc p {
  margin: 0;
}
.empty-tip {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-top: 4px;
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

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
