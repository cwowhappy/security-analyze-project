<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElAutocomplete,
  ElButton,
  ElTable,
  ElTableColumn,
  ElPagination,
  ElMessage,
} from 'element-plus'
import { Search } from '@element-plus/icons-vue'
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
</script>

<template>
  <div class="company-search" :class="{ 'has-result': hasSearched }">
    <div class="search-container">
      <h2 v-if="!hasSearched" class="search-title">公司信息搜索</h2>
      <div class="search-box">
        <ElAutocomplete
          v-model="keyword"
          :fetch-suggestions="fetchSuggestions"
          placeholder="输入股票代码或公司名称"
          :prefix-icon="Search"
          clearable
          class="search-input"
          :highlight-first-item="false"
          @select="handleSelect"
          @keyup.enter="handleKeyEnter"
          @clear="handleClear"
        >
          <template #default="{ item }">
            <div class="suggest-item">
              <span class="suggest-code">{{ item.stockCode }}</span>
              <span class="suggest-name">{{ item.stockName }}</span>
              <span v-if="item.market" class="suggest-market">[{{ item.market }}]</span>
            </div>
          </template>
        </ElAutocomplete>
        <ElButton type="primary" :icon="Search" class="search-btn" @click="handleKeyEnter">
          搜索
        </ElButton>
      </div>
    </div>

    <div v-if="hasSearched" class="result-section">
      <ElTable
        :data="tableData"
        v-loading="loading"
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
  </div>
</template>

<style scoped>
.company-search {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 40px;
  transition: all 0.3s ease;
}

.company-search:not(.has-result) {
  justify-content: center;
  align-items: center;
}

.company-search.has-result {
  justify-content: flex-start;
  align-items: stretch;
  padding-top: 24px;
}

.search-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 680px;
  transition: all 0.3s ease;
}

.has-result .search-container {
  align-items: flex-start;
  max-width: 100%;
}

.search-title {
  font-size: 28px;
  font-weight: 500;
  color: #333;
  margin-bottom: 24px;
}

.search-box {
  display: flex;
  gap: 12px;
  width: 100%;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 4px 12px;
  height: 48px;
}

.search-input :deep(.el-input__inner) {
  font-size: 16px;
}

.search-btn {
  height: 48px;
  padding: 0 32px;
  font-size: 16px;
  border-radius: 8px;
}

.suggest-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
}

.suggest-code {
  font-weight: 600;
  color: #409eff;
  min-width: 80px;
}

.suggest-name {
  flex: 1;
  color: #333;
}

.suggest-market {
  color: #999;
  font-size: 12px;
}

.result-section {
  margin-top: 24px;
  width: 100%;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
