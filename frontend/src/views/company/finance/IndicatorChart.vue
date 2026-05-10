<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { IndicatorMetric } from '@/types/finance'
import { applyChartTheme } from '@/styles/echarts-theme'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const props = defineProps<{
  metrics: IndicatorMetric[]
  xAxisLabels?: string[]
}>()

const chartOption = computed(() => {
  if (!props.metrics || props.metrics.length === 0) {
    return {}
  }

  // 使用自定义 x 轴标签，或取第一个指标的数据日期作为 x 轴
  const xData = props.xAxisLabels ?? props.metrics[0]?.data.map((d) => d.reportDate) ?? []

  const series = props.metrics.map((m) => {
    const isPercentage = m.unit === '%'
    return {
      name: m.label,
      type: isPercentage ? 'line' : 'bar',
      data: m.data.map((d) => d.value),
      smooth: true,
      yAxisIndex: isPercentage ? 1 : 0,
    }
  })

  return applyChartTheme({
    legend: {
      data: props.metrics.map((m) => m.label),
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { rotate: 30 },
    },
    yAxis: [
      {
        type: 'value',
        name: '金额（元）',
        axisLabel: {
          formatter: (value: number) => {
            if (value >= 1e8) return (value / 1e8).toFixed(0) + '亿'
            if (value >= 1e4) return (value / 1e4).toFixed(0) + '万'
            return value
          },
        },
      },
      {
        type: 'value',
        name: '比率（%）',
        axisLabel: { formatter: '{value}%' },
        splitLine: { show: false },
      },
    ],
    series,
  })
})
</script>

<template>
  <div class="indicator-chart">
    <VChart v-if="metrics.length > 0" :option="chartOption" autoresize style="height: 360px" />
    <div v-else class="empty-tip">暂无指标数据</div>
  </div>
</template>

<style scoped>
.indicator-chart {
  width: 100%;
}
.empty-tip {
  color: var(--text-tertiary);
  padding: 40px 0;
  text-align: center;
}
</style>
