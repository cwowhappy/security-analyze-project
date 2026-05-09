<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { AnnualMetric } from '@/types/research'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const props = defineProps<{
  metrics: AnnualMetric[]
}>()

const xData = computed(() => props.metrics.map(m => m.reportDate))

const chartOption = computed(() => {
  if (!props.metrics?.length) return {}

  return {
    backgroundColor: 'transparent',
    title: { text: '盈利能力趋势', left: 'center', textStyle: { color: '#e5e7eb', fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(15,21,37,0.9)',
      borderColor: 'rgba(255,255,255,0.1)',
      textStyle: { color: '#e5e7eb' },
    },
    legend: { data: ['营业总收入', '归母净利润', '营业成本', '毛利率', '净利率', 'ROE'], bottom: 0, textStyle: { color: '#9ca3af' } },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: xData.value,
      axisLabel: { color: '#9ca3af' },
      axisLine: { lineStyle: { color: '#4b5563' } },
    },
    yAxis: [
      {
        type: 'value',
        name: '金额（元）',
        nameTextStyle: { color: '#9ca3af' },
        axisLabel: {
          color: '#9ca3af',
          formatter: (value: number) => {
            if (value >= 1e8) return (value / 1e8).toFixed(0) + '亿'
            if (value >= 1e4) return (value / 1e4).toFixed(0) + '万'
            return value
          },
        },
        axisLine: { lineStyle: { color: '#4b5563' } },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
      },
      {
        type: 'value',
        name: '比率（%）',
        nameTextStyle: { color: '#9ca3af' },
        axisLabel: { formatter: '{value}%', color: '#9ca3af' },
        axisLine: { lineStyle: { color: '#4b5563' } },
        splitLine: { show: false },
      },
    ],
    series: [
      { name: '营业总收入', type: 'bar', data: props.metrics.map(m => m.totalRevenue), yAxisIndex: 0, itemStyle: { color: '#00d4ff' } },
      { name: '归母净利润', type: 'bar', data: props.metrics.map(m => m.parentNetProfit), yAxisIndex: 0, itemStyle: { color: '#67c23a' } },
      { name: '营业成本', type: 'bar', data: props.metrics.map(m => m.operateCost), yAxisIndex: 0, itemStyle: { color: '#f56c6c' } },
      { name: '毛利率', type: 'line', data: props.metrics.map(m => m.grossMargin), yAxisIndex: 1, smooth: true, itemStyle: { color: '#ff9500' } },
      { name: '净利率', type: 'line', data: props.metrics.map(m => m.netMargin), yAxisIndex: 1, smooth: true, itemStyle: { color: '#9ca3af' } },
      { name: 'ROE', type: 'line', data: props.metrics.map(m => m.roe), yAxisIndex: 1, smooth: true, itemStyle: { color: '#e6a23c' } },
    ],
  }
})
</script>

<template>
  <div class="chart-wrapper">
    <VChart v-if="metrics.length > 0" :option="chartOption" autoresize style="height: 100%" />
    <div v-else class="empty-tip">暂无数据</div>
  </div>
</template>

<style scoped>
.chart-wrapper { width: 100%; height: 360px; }
.empty-tip { color: var(--text-tertiary); padding: 40px 0; text-align: center; }
</style>
