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
  ElTabs,
  ElTabPane,
  ElCard,
  ElTag,
} from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getIndexList, getIndexCategories } from '@/api/index'
import type { IndexListItem, IndexListResponse, IndexCategoryGroup } from '@/types/index'

const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const tableData = ref<IndexListItem[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(20)
const hasSearched = ref(false)

const categoryLoading = ref(false)
const categoryGroups = ref<IndexCategoryGroup[]>([])
const activeCategory = ref('')

interface SuggestItem {
  value: string
  indexCode: string
  indexName: string
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
      const res: IndexListResponse = await getIndexList(trimmed, 0, 10)
      const items = res.items.map((item) => ({
        value: `${item.indexCode} ${item.indexName}`,
        indexCode: item.indexCode,
        indexName: item.indexName,
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
    const res: IndexListResponse = await getIndexList(trimmed, page.value, size.value)
    tableData.value = res.items
    total.value = res.total
    hasSearched.value = true
  } catch (err) {
    ElMessage.error('搜索失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

function handlePageChange(newPage: number) {
  page.value = newPage - 1
  handleSearch()
}

function goToDetail(indexCode: string) {
  router.push(`/indexes/${encodeURIComponent(indexCode)}`)
}

function handleSelect(item: SuggestItem) {
  keyword.value = item.indexCode
  page.value = 0
  handleSearch()
}

async function loadCategories() {
  categoryLoading.value = true
  try {
    const res = await getIndexCategories()
    categoryGroups.value = res
    if (res.length > 0) {
      activeCategory.value = res[0].indexType
    }
  } catch (err) {
    console.error('加载分类指数失败', err)
  } finally {
    categoryLoading.value = false
  }
}

function getTagType(indexType: string): string {
  const map: Record<string, string> = {
    '宽基': 'primary',
    '行业': 'success',
    '主题': 'warning',
    '策略': 'info',
  }
  return map[indexType] || 'info'
}

onMounted(() => {
  loadCategories()
})
</script>

<template>
  <div class="index-list">
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem>指数信息</ElBreadcrumbItem>
    </ElBreadcrumb>

    <h2 class="page-title">指数信息</h2>

    <!-- 上方：指数搜索 -->
    <div class="search-section">
      <div class="search-box">
        <ElAutocomplete
          v-model="keyword"
          :fetch-suggestions="fetchSuggestions"
          placeholder="输入指数代码或名称搜索"
          clearable
          style="width: 480px"
          @select="handleSelect"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <Search style="width: 16px; height: 16px; color: var(--text-tertiary)" />
          </template>
        </ElAutocomplete>
        <ElButton type="primary" @click="handleSearch">搜索</ElButton>
      </div>
      <div class="search-hint">
        例如：000001（上证指数）、399001（深证成指）、000300（沪深300）
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-if="hasSearched" v-loading="loading" class="result-area">
      <ElTable :data="tableData" style="width: 100%" @row-click="(row: IndexListItem) => goToDetail(row.indexCode)">
        <ElTableColumn prop="indexCode" label="指数代码" width="120" />
        <ElTableColumn prop="indexName" label="指数名称" />
        <ElTableColumn prop="indexType" label="指数类型" width="120" />
        <ElTableColumn prop="market" label="市场" width="100" />
        <ElTableColumn prop="publishDate" label="发布日期" width="120" />
      </ElTable>

      <ElPagination
        v-if="total > 0"
        :current-page="page + 1"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 下方：核心指数 -->
    <div v-if="categoryGroups.length > 0" v-loading="categoryLoading" class="category-section">
      <div class="section-title">核心指数</div>
      <ElTabs v-model="activeCategory" type="border-card">
        <ElTabPane
          v-for="group in categoryGroups"
          :key="group.indexType"
          :label="group.indexTypeLabel"
          :name="group.indexType"
        >
          <div class="index-cards">
            <ElCard
              v-for="item in group.items"
              :key="item.indexCode"
              class="index-card"
              shadow="hover"
              @click="goToDetail(item.indexCode)"
            >
              <div class="card-header">
                <span class="index-name">{{ item.indexName }}</span>
                <ElTag size="small" :type="getTagType(item.indexType)">{{ item.market }}</ElTag>
              </div>
              <div class="card-code">{{ item.indexCode }}</div>
              <div v-if="item.publishDate" class="card-date">发布: {{ item.publishDate }}</div>
            </ElCard>
          </div>
        </ElTabPane>
      </ElTabs>
    </div>
  </div>
</template>

<style scoped>
.index-list {
  padding: 8px;
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 16px 0;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
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

/* 搜索结果 */
.result-area {
  margin-bottom: 32px;
}

/* 核心指数 */
.category-section {
  margin-bottom: 32px;
}
.index-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  padding: 8px 0;
}
.index-card {
  cursor: pointer;
  transition: all 0.3s ease;
}
.index-card:hover {
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
.index-name {
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
.card-date {
  font-size: 12px;
  color: var(--text-tertiary);
}

:deep(.el-table__row) {
  cursor: pointer;
}
:deep(.el-tabs__content) {
  padding: 12px;
}
</style>
