<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { AnnualMetric } from '@/types/research'
import { NEON_COLORS } from '@/styles/echarts-theme'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const props = defineProps<{
  metrics: AnnualMetric[]
}>()

const xData = computed(() => props.metrics.map(m => m.reportDate))

const chartOption = computed(() => {
  if (!props.metrics?.length) return {}

  return {
    backgroundColor: 'transparent',
    title: { text: '资产负债结构', left: 'center', textStyle: { color: '#E8EAED', fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(17, 19, 24, 0.95)',
      borderColor: 'rgba(43, 106, 255, 0.20)',
      textStyle: { color: '#E8EAED' },
      formatter: (params: any[]) => {
        let html = `<div style="font-weight:600;margin-bottom:4px">${params[0].axisValue}</div>`
        params.forEach((p: any) => {
          const val = p.value
          let display: string
          if (val == null) {
            display = '<span style="color:#999">数据缺失</span>'
          } else if (p.seriesName === '资产负债率') {
            display = val.toFixed(2) + '%'
          } else {
            const abs = Math.abs(val)
            if (abs >= 1e8) display = (val / 1e8).toFixed(2) + ' 亿'
            else if (abs >= 1e4) display = (val / 1e4).toFixed(2) + ' 万'
            else display = val.toLocaleString()
          }
          html += `<div style="display:flex;align-items:center;gap:6px;margin:2px 0">
            <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color}"></span>
            <span>${p.seriesName}:</span>
            <span style="margin-left:auto;font-weight:500">${display}</span>
          </div>`
        })
        return html
      },
    },
    legend: { data: ['流动资产', '非流动资产', '流动负债', '非流动负债', '资产负债率'], bottom: 0, textStyle: { color: '#9CA3AF' } },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: xData.value,
      axisLabel: { color: '#9CA3AF' },
      axisLine: { lineStyle: { color: '#4B5563' } },
    },
    yAxis: [
      {
        type: 'value',
        name: '金额（元）',
        nameTextStyle: { color: '#9CA3AF' },
        axisLabel: {
          color: '#9CA3AF',
          formatter: (value: number) => {
            const abs = Math.abs(value)
            if (abs >= 1e8) return (value / 1e8).toFixed(0) + '亿'
            if (abs >= 1e4) return (value / 1e4).toFixed(0) + '万'
            return value
          },
        },
        axisLine: { lineStyle: { color: '#4B5563' } },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
      },
      {
        type: 'value',
        name: '比率（%）',
        nameTextStyle: { color: '#9CA3AF' },
        axisLabel: { formatter: '{value}%', color: '#9CA3AF' },
        axisLine: { lineStyle: { color: '#4B5563' } },
        splitLine: { show: false },
      },
    ],
    series: [
      { name: '流动资产', type: 'bar', stack: 'assets', data: props.metrics.map(m => m.totalCurrentAssets), yAxisIndex: 0, itemStyle: { color: NEON_COLORS[0] } },
      { name: '非流动资产', type: 'bar', stack: 'assets', data: props.metrics.map(m => m.totalNoncurrentAssets), yAxisIndex: 0, itemStyle: { color: NEON_COLORS[3] } },
      { name: '流动负债', type: 'bar', stack: 'liabilities', data: props.metrics.map(m => m.totalCurrentLiabilities), yAxisIndex: 0, itemStyle: { color: NEON_COLORS[6] } },
      { name: '非流动负债', type: 'bar', stack: 'liabilities', data: props.metrics.map(m => m.totalNoncurrentLiabilities), yAxisIndex: 0, itemStyle: { color: NEON_COLORS[4] } },
      { name: '资产负债率', type: 'line', data: props.metrics.map(m => m.debtRatio), yAxisIndex: 1, smooth: true, itemStyle: { color: NEON_COLORS[2] } },
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
