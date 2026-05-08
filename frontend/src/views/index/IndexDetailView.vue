<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElTabs,
  ElTabPane,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElMessage,
  ElEmpty,
  ElRadioGroup,
  ElRadioButton,
  ElTable,
  ElTableColumn,
} from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, DataZoomComponent } from 'echarts/components'
import { getIndexDetail, getIndexTrend, getIndexEtfs } from '@/api/index'
import type { IndexDetailResponse, IndexTrendItem, EtfListItem } from '@/types/index'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent])

const route = useRoute()
const router = useRouter()

const indexCode = route.params.indexCode as string
const loading = ref(false)
const indexInfo = ref<IndexDetailResponse | null>(null)
const activeTab = ref('basic')

const trendGranularity = ref<'day' | 'week' | 'month'>('day')
const trendData = ref<IndexTrendItem[]>([])
const trendLoading = ref(false)

const etfData = ref<EtfListItem[]>([])
const etfLoading = ref(false)

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getIndexDetail(indexCode)
    indexInfo.value = res
  } catch (err) {
    ElMessage.error('加载指数详情失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

async function fetchTrend() {
  trendLoading.value = true
  try {
    const res = await getIndexTrend(indexCode, trendGranularity.value)
    trendData.value = res.items
  } catch (err) {
    ElMessage.error('加载趋势数据失败')
    console.error(err)
  } finally {
    trendLoading.value = false
  }
}

async function fetchEtfs() {
  etfLoading.value = true
  try {
    const res = await getIndexEtfs(indexCode)
    etfData.value = res
  } catch (err) {
    ElMessage.error('加载ETF数据失败')
    console.error(err)
  } finally {
    etfLoading.value = false
  }
}

const chartOption = computed(() => {
  const dates = trendData.value.map((d) => d.tradeDate)
  const closes = trendData.value.map((d) => d.closePrice)
  const volumes = trendData.value.map((d) => d.volume)

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(15,21,37,0.9)',
      borderColor: 'rgba(255,255,255,0.1)',
      textStyle: { color: '#e5e7eb' },
    },
    legend: {
      data: ['收盘价', '成交量'],
      top: 0,
      textStyle: { color: '#9ca3af' },
    },
    grid: [
      { left: '10%', right: '8%', height: '50%' },
      { left: '10%', right: '8%', top: '68%', height: '16%' },
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        scale: true,
        boundaryGap: false,
        axisLine: { onZero: false, lineStyle: { color: '#4b5563' } },
        splitLine: { show: false },
        axisLabel: { color: '#9ca3af' },
        min: 'dataMin',
        max: 'dataMax',
      },
      {
        type: 'category',
        gridIndex: 1,
        data: dates,
        scale: true,
        boundaryGap: false,
        axisLine: { onZero: false, lineStyle: { color: '#4b5563' } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        min: 'dataMin',
        max: 'dataMax',
      },
    ],
    yAxis: [
      {
        scale: true,
        axisLine: { lineStyle: { color: '#4b5563' } },
        axisLabel: { color: '#9ca3af' },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
      },
      {
        scale: true,
        gridIndex: 1,
        splitNumber: 2,
        axisLabel: { show: false },
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false },
      },
    ],
    dataZoom: [
      { type: 'inside', xAxisIndex: [0, 1], start: 0, end: 100 },
      { show: true, xAxisIndex: [0, 1], type: 'slider', top: '88%', start: 0, end: 100, textStyle: { color: '#9ca3af' } },
    ],
    series: [
      {
        name: '收盘价',
        type: 'line',
        data: closes,
        smooth: true,
        lineStyle: { width: 2, color: '#00d4ff' },
        itemStyle: { color: '#00d4ff' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(0,212,255,0.3)' },
              { offset: 1, color: 'rgba(0,212,255,0.05)' },
            ],
          },
        },
      },
      {
        name: '成交量',
        type: 'line',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: volumes,
        smooth: true,
        lineStyle: { width: 1, color: '#67c23a' },
        itemStyle: { color: '#67c23a' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(103,194,58,0.3)' },
              { offset: 1, color: 'rgba(103,194,58,0.05)' },
            ],
          },
        },
      },
    ],
  }
})

watch(trendGranularity, () => {
  fetchTrend()
})

watch(activeTab, (tab) => {
  if (tab === 'trend' && trendData.value.length === 0) {
    fetchTrend()
  }
  if (tab === 'etfs' && etfData.value.length === 0) {
    fetchEtfs()
  }
})

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="index-detail" v-loading="loading">
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem :to="{ path: '/indexes' }">指数信息</ElBreadcrumbItem>
      <ElBreadcrumbItem>{{ indexInfo?.indexName || indexCode }}</ElBreadcrumbItem>
    </ElBreadcrumb>

    <h2 class="page-title">
      {{ indexInfo?.indexName || indexCode }}
      <span v-if="indexInfo?.indexCode" class="subtitle">({{ indexInfo.indexCode }})</span>
    </h2>

    <ElTabs v-model="activeTab" style="margin-top: 16px">
      <ElTabPane label="基本信息" name="basic">
        <div v-if="indexInfo" class="info-grid">
          <div class="info-item">
            <span class="label">指数代码</span>
            <span class="value">{{ indexInfo.indexCode }}</span>
          </div>
          <div class="info-item">
            <span class="label">指数名称</span>
            <span class="value">{{ indexInfo.indexName }}</span>
          </div>
          <div class="info-item">
            <span class="label">指数类型</span>
            <span class="value">{{ indexInfo.indexType || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">市场</span>
            <span class="value">{{ indexInfo.market || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">基日</span>
            <span class="value">{{ indexInfo.baseDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">基点</span>
            <span class="value">{{ indexInfo.basePoint != null ? indexInfo.basePoint : '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">成分股数量</span>
            <span class="value">{{ indexInfo.componentCount != null ? indexInfo.componentCount : '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">发布日期</span>
            <span class="value">{{ indexInfo.publishDate || '-' }}</span>
          </div>
        </div>
      </ElTabPane>

      <ElTabPane label="趋势分析" name="trend">
        <div class="trend-controls">
          <ElRadioGroup v-model="trendGranularity">
            <ElRadioButton value="day">日线</ElRadioButton>
            <ElRadioButton value="week">周线</ElRadioButton>
            <ElRadioButton value="month">月线</ElRadioButton>
          </ElRadioGroup>
        </div>
        <div v-loading="trendLoading" class="chart-wrapper">
          <VChart v-if="trendData.length > 0" :option="chartOption" autoresize style="height: 500px; width: 100%" />
          <ElEmpty v-else description="暂无趋势数据" />
        </div>
      </ElTabPane>

      <ElTabPane label="关联ETF" name="etfs">
        <div v-loading="etfLoading">
          <ElTable v-if="etfData.length > 0" :data="etfData" style="width: 100%">
            <ElTableColumn prop="etfCode" label="ETF代码" width="120" />
            <ElTableColumn prop="etfName" label="ETF名称" />
            <ElTableColumn prop="trackingIndexCode" label="跟踪指数" width="120" />
            <ElTableColumn prop="fundSize" label="基金规模" width="140">
              <template #default="{ row }">
                {{ row.fundSize != null ? (row.fundSize / 100000000).toFixed(2) + ' 亿' : '-' }}
              </template>
            </ElTableColumn>
            <ElTableColumn prop="establishDate" label="成立日期" width="120" />
            <ElTableColumn prop="market" label="市场" width="80" />
          </ElTable>
          <ElEmpty v-else description="暂无关联ETF数据" />
        </div>
      </ElTabPane>
    </ElTabs>
  </div>
</template>

<style scoped>
.index-detail {
  padding: 8px;
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 16px 0 0;
}
.subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: normal;
  margin-left: 8px;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  max-width: 600px;
}
.info-item {
  display: flex;
  align-items: baseline;
}
.info-item .label {
  color: var(--text-secondary);
  width: 100px;
  flex-shrink: 0;
}
.info-item .value {
  font-weight: 500;
  color: var(--text-primary);
}
.trend-controls {
  margin-bottom: 16px;
}
.chart-wrapper {
  width: 100%;
}
</style>
