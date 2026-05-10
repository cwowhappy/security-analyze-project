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
  ElButton,
} from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
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
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(17, 19, 24, 0.95)',
      borderColor: 'rgba(43, 106, 255, 0.20)',
      textStyle: { color: '#E8EAED' },
      formatter: (params: any[]) => {
        const close = params[0]?.value ?? 0
        const change = params[1]?.value ?? 0
        return `${params[0]?.axisValue}<br/>收盘: ${close}<br/>涨跌幅: ${change}%`
      },
    },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 0, textStyle: { color: '#9ca3af' } }],
    xAxis: { type: 'category', data: xData, axisLine: { lineStyle: { color: '#4B5563' } }, axisLabel: { color: '#9CA3AF', rotate: 30 } },
    yAxis: [
      { type: 'value', name: '收盘', position: 'left', axisLine: { lineStyle: { color: '#4B5563' } }, axisLabel: { color: '#9CA3AF' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } } },
      { type: 'value', name: '涨跌幅(%)', position: 'right', axisLabel: { formatter: '{value}%', color: '#9CA3AF' }, axisLine: { lineStyle: { color: '#4B5563' } }, splitLine: { show: false } },
    ],
    series: [
      { name: '收盘', type: 'line', data: closeData, smooth: true, yAxisIndex: 0, lineStyle: { color: '#2B6AFF' }, itemStyle: { color: '#2B6AFF' }, areaStyle: { opacity: 0.1, color: 'rgba(43,106,255,0.2)' } },
      { name: '涨跌幅', type: 'line', data: changeData, smooth: true, yAxisIndex: 1, lineStyle: { type: 'dashed', color: '#F59E0B' }, itemStyle: { color: '#F59E0B' } },
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

    <div class="page-header">
      <h2 class="page-title">
        {{ industryName || industryCode }}
        <ElTag size="small" type="info" style="margin-left: 8px; vertical-align: middle">
          {{ standard === 'EM' ? '东财板块' : '申万行业' }}
        </ElTag>
        <span class="subtitle">共 {{ total }} 家公司</span>
      </h2>
      <ElButton link :icon="ArrowLeft" @click="router.push('/industries')">
        返回行业列表
      </ElButton>
    </div>

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
        <ElTableColumn prop="stockCode" label="股票代码" min-width="120" />
        <ElTableColumn prop="stockName" label="公司名称" min-width="180" />
        <ElTableColumn prop="industry" label="所属行业" min-width="200" />
        <ElTableColumn prop="region" label="地区" min-width="120" />
        <ElTableColumn prop="listingDate" label="上市日期" min-width="120" />
        <ElTableColumn prop="market" label="市场" min-width="80" />
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
  padding: 8px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0 20px;
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
}
.subtitle {
  font-size: 14px;
  color: var(--text-secondary);
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
  color: var(--text-primary);
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
