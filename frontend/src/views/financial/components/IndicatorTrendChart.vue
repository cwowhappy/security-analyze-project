<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, DataZoomComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { TrendData } from '@/types/financial'
import { useFinancialFormatter } from '../composables/useFinancialFormatter'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent])

const props = defineProps<{
  data: TrendData[]
}>()

const { formatReportDate } = useFinancialFormatter()

const metricNames: Record<string, string> = {
  roe: 'ROE',
  roa: 'ROA',
  grossMargin: '毛利率',
  netMargin: '净利率',
  revenueGrowth: '营收增速',
  npParentGrowth: '归母净利增速',
  debtRatio: '资产负债率',
  currentRatio: '流动比率',
  pe: 'PE-TTM',
  pb: 'PB',
}

const chartOption = computed(() => {
  if (!props.data || props.data.length === 0) {
    return { title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999' } } }
  }

  // 统一日期轴：取所有 series 中所有日期的并集并排序
  const allDatesSet = new Set<string>()
  props.data.forEach((s) => s.data.forEach((p) => { if (p.value !== null) allDatesSet.add(p.reportDate) }))
  const allDates = Array.from(allDatesSet).sort()

  // 根据 reportDate 推断 reportType，用于格式化日期显示
  const dateToType = new Map<string, string>()
  props.data.forEach((s) => s.data.forEach((p) => {
    if (p.reportDate && !dateToType.has(p.reportDate)) {
      // 从原始数据中提取 reportType：reportDate 第5-7位为月份，可推断类型
      const month = parseInt(p.reportDate.substring(5, 7), 10)
      if (month === 3) dateToType.set(p.reportDate, 'Q1')
      else if (month === 6) dateToType.set(p.reportDate, 'Q2')
      else if (month === 9) dateToType.set(p.reportDate, 'Q3')
      else dateToType.set(p.reportDate, 'Y')
    }
  }))

  const series = props.data.map((s) => ({
    name: metricNames[s.metric] || s.metric,
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    data: allDates.map((d) => {
      const point = s.data.find((p) => p.reportDate === d)
      return point && point.value !== null ? point.value : null
    }),
    connectNulls: true,
  }))

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'var(--surface)',
      borderColor: 'var(--border)',
      textStyle: { color: 'var(--text-primary)' },
    },
    legend: {
      data: series.map((s) => s.name),
      bottom: 0,
      textStyle: { color: 'var(--text-secondary)' },
    },
    grid: { left: 48, right: 24, top: 24, bottom: 48 },
    xAxis: {
      type: 'category',
      data: allDates.map((d) => formatReportDate(d, dateToType.get(d) || 'Y')),
      axisLine: { lineStyle: { color: 'var(--border)' } },
      axisLabel: { color: 'var(--text-secondary)' },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: 'var(--border)', type: 'dashed' } },
      axisLabel: { color: 'var(--text-secondary)' },
    },
    series,
    dataZoom: allDates.length > 10 ? [{ type: 'inside', start: Math.max(0, (allDates.length - 10) / allDates.length * 100), end: 100 }] : undefined,
  }
})
</script>

<template>
  <div class="chart-card">
    <v-chart class="chart" :option="chartOption" autoresize />
  </div>
</template>

<style scoped>
.chart-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 16px;
  height: 360px;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
