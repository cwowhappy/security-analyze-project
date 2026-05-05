<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElTable,
  ElTableColumn,
  ElPagination,
  ElMessage,
  ElSelect,
  ElOption,
  ElTag,
  ElEmpty,
} from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, DataZoomComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getIndustryCompanies, getIndustryTrend } from '@/api/industry'
import type { Company } from '@/types/company'
import type { TrendDataPoint } from '@/types/industry'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, DataZoomComponent])

const route = useRoute()
const router = useRouter()

const industryCode = computed(() => route.params.industryCode as string)
const standard = computed(() => (route.query.standard as string) || 'EM')

const loading = ref(false)
const trendLoading = ref(false)

const companies = ref<Company[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(20)

const trendData = ref<TrendDataPoint[]>([])
const period = ref('3m')
const fallback = ref(false)
const industryName = ref('')

const periodOptions = [
  { value: '1m', label: '近1月' },
  { value: '3m', label: '近3月' },
  { value: '6m', label: '近6月' },
  { value: '1y', label: '近1年' },
]

const chartOption = computed(() => {
  if (!trendData.value || trendData.value.length === 0) {
    return {}
  }
  const xData = trendData.value.map((d) => d.date)
  const closeData = trendData.value.map((d) => d.close)
  const changeData = trendData.value.map((d) => d.changePercent)

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params: any[]) => {
        const close = params[0]?.value ?? 0
        const change = params[1]?.value ?? 0
        return `${params[0]?.axisValue}<br/>收盘: ${close}<br/>涨跌幅: ${change}%`
      },
    },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 0 }],
    xAxis: { type: 'category', data: xData, axisLabel: { rotate: 30 } },
    yAxis: [
      { type: 'value', name: '收盘', position: 'left' },
      { type: 'value', name: '涨跌幅(%)', position: 'right', axisLabel: { formatter: '{value}%' } },
    ],
    series: [
      { name: '收盘', type: 'line', data: closeData, smooth: true, yAxisIndex: 0, areaStyle: { opacity: 0.1 } },
      { name: '涨跌幅', type: 'line', data: changeData, smooth: true, yAxisIndex: 1, lineStyle: { type: 'dashed' } },
    ],
  }
})

async function fetchCompanies() {
  loading.value = true
  try {
    const res = await getIndustryCompanies(
      industryCode.value,
      standard.value,
      undefined,
      { page: page.value, size: size.value },
    )
    companies.value = res.items
    total.value = res.total
  } catch (err) {
    ElMessage.error('加载成分股失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

async function fetchTrend() {
  trendLoading.value = true
  try {
    const res = await getIndustryTrend(industryCode.value, standard.value, period.value)
    trendData.value = res.data
    fallback.value = res.fallback
    industryName.value = res.industryName
  } catch (err) {
    ElMessage.error('加载行业走势失败')
    console.error(err)
  } finally {
    trendLoading.value = false
  }
}

function handleRowClick(row: Company) {
  router.push(`/companies/${row.stockCode}`)
}

watch(period, () => {
  fetchTrend()
})

watch([page, size], () => {
  fetchCompanies()
})

watch(industryCode, () => {
  page.value = 0
  fetchCompanies()
  fetchTrend()
})

onMounted(() => {
  fetchCompanies()
  fetchTrend()
})
</script>

<template>
  <div class="industry-detail">
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem :to="{ path: '/industries' }">行业信息</ElBreadcrumbItem>
      <ElBreadcrumbItem>{{ industryName || industryCode }}</ElBreadcrumbItem>
    </ElBreadcrumb>

    <h2 class="page-title">
      {{ industryName || industryCode }}
      <ElTag size="small" type="info" style="margin-left: 8px; vertical-align: middle">
        {{ standard === 'EM' ? '东财板块' : '申万行业' }}
      </ElTag>
      <span class="subtitle">共 {{ total }} 家公司</span>
    </h2>

    <div class="section" v-loading="trendLoading">
      <div class="section-header">
        <h3 class="section-title">行业指数走势</h3>
        <ElSelect v-model="period" style="width: 120px">
          <ElOption
            v-for="opt in periodOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </ElSelect>
      </div>
      <ElTag v-if="fallback" type="warning" size="small" style="margin-bottom: 8px">
        当前为演示数据，akshare 实时接口暂不可用
      </ElTag>
      <VChart v-if="trendData.length > 0" :option="chartOption" autoresize style="height: 360px" />
      <ElEmpty v-else description="暂无走势数据" />
    </div>

    <div class="section">
      <h3 class="section-title">成分股列表</h3>
      <ElTable
        v-loading="loading"
        :data="companies"
        stripe
        highlight-current-row
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <ElTableColumn prop="stockCode" label="股票代码" width="120" />
        <ElTableColumn prop="stockName" label="公司名称" width="180" />
        <ElTableColumn prop="industry" label="所属行业" width="200" />
        <ElTableColumn prop="region" label="地区" width="120" />
        <ElTableColumn prop="listingDate" label="上市日期" width="120" />
        <ElTableColumn prop="market" label="市场" width="80" />
      </ElTable>
      <ElPagination
        v-if="total > 0"
        :current-page="page + 1"
        :page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="page = $event - 1"
        @size-change="size = $event"
        class="pagination"
      />
    </div>
  </div>
</template>

<style scoped>
.industry-detail {
  padding: 24px;
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: #303133;
  margin: 16px 0 20px;
}
.subtitle {
  font-size: 14px;
  color: #909399;
  font-weight: normal;
  margin-left: 8px;
}
.section {
  margin-top: 24px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
