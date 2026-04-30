<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElTable, ElTableColumn, ElPagination, ElInput, ElMessage } from 'element-plus'
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

let debounceTimer: ReturnType<typeof setTimeout> | null = null

async function fetchData() {
  loading.value = true
  try {
    const res: CompanyListResponse = await getCompanyList({
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value,
    })
    tableData.value = res.items
    total.value = res.total
  } catch (err) {
    ElMessage.error('加载数据失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

watch(keyword, () => {
  page.value = 0
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    fetchData()
  }, 300)
})

watch(page, () => {
  fetchData()
})

watch(size, () => {
  page.value = 0
  fetchData()
})

function handleRowClick(row: Company) {
  router.push(`/companies/${row.stockCode}`)
}

// initial load
fetchData()
</script>

<template>
  <div class="company-list">
    <h2>公司信息</h2>

    <div class="search-bar">
      <ElInput
        v-model="keyword"
        placeholder="输入股票代码或公司名称"
        :prefix-icon="Search"
        clearable
        style="width: 320px"
      />
    </div>

    <ElTable
      :data="tableData"
      v-loading="loading"
      stripe
      style="width: 100%; margin-top: 16px"
      @row-click="handleRowClick"
      highlight-current-row
    >
      <ElTableColumn prop="stockCode" label="股票代码" width="120" />
      <ElTableColumn prop="stockName" label="公司名称" min-width="180" />
      <ElTableColumn prop="industry" label="所属行业" width="150" />
      <ElTableColumn prop="region" label="地区" width="120" />
      <ElTableColumn prop="listingDate" label="上市日期" width="120" />
      <ElTableColumn prop="market" label="市场" width="80">
        <template #default="{ row }">
          <span>{{ row.market }}</span>
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
      />
    </div>
  </div>
</template>

<style scoped>
.company-list {
  padding: 24px;
}

.search-bar {
  margin-top: 16px;
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
