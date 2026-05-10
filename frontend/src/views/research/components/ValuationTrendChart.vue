<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { ValuationHistoryItem } from '@/types/research'
import { NEON_COLORS } from '@/styles/echarts-theme'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const props = defineProps<{
  items: ValuationHistoryItem[]
}>()

const chartOption = computed(() => {
  if (!props.items?.length) return {}
  const dates = props.items.map(i => i.tradeDate)
  const peTtm = props.items.map(i => i.peTtm)
  const pb = props.items.map(i => i.pb)
  const closePrice = props.items.map(i => i.closePrice)

  return {
    backgroundColor: 'transparent',
    title: { text: '估值历史趋势（近5年）', left: 'center', textStyle: { color: '#E8EAED', fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      backgroundColor: 'rgba(17, 19, 24, 0.95)',
      borderColor: 'rgba(43, 106, 255, 0.20)',
      textStyle: { color: '#E8EAED' },
    },
    legend: { data: ['PE(TTM)', 'PB', '股价'], bottom: 0, textStyle: { color: '#9CA3AF' } },
    grid: { left: '3%', right: '4%', bottom: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { color: '#9CA3AF' },
      axisLine: { lineStyle: { color: '#4B5563' } },
    },
    yAxis: [
      {
        type: 'value',
        name: '估值',
        nameTextStyle: { color: '#9CA3AF' },
        position: 'left',
        axisLabel: { color: '#9CA3AF' },
        axisLine: { lineStyle: { color: '#4B5563' } },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
      },
      {
        type: 'value',
        name: '股价',
        nameTextStyle: { color: '#9CA3AF' },
        position: 'right',
        axisLabel: { color: '#9CA3AF' },
        axisLine: { lineStyle: { color: '#4B5563' } },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: 'PE(TTM)',
        type: 'line',
        data: peTtm,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: NEON_COLORS[0], width: 2 },
        itemStyle: { color: NEON_COLORS[0] },
      },
      {
        name: 'PB',
        type: 'line',
        data: pb,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: NEON_COLORS[3], width: 2 },
        itemStyle: { color: NEON_COLORS[3] },
      },
      {
        name: '股价',
        type: 'line',
        yAxisIndex: 1,
        data: closePrice,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: NEON_COLORS[4], width: 1.5, type: 'dashed' },
        itemStyle: { color: NEON_COLORS[4] },
      },
    ],
  }
})
</script>

<template>
  <div class="chart-wrapper">
    <VChart class="chart-container" :option="chartOption" autoresize />
  </div>
</template>

<style scoped>
.chart-wrapper {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: var(--card-padding);
  margin-bottom: 24px;
}
.chart-container {
  width: 100%;
  height: 360px;
}
</style>
