<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { DupontAnalysis } from '@/types/financial'
import { useFinancialFormatter } from '../composables/useFinancialFormatter'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent])

const props = defineProps<{
  data: DupontAnalysis | null
}>()

const { formatPercent, formatRatio } = useFinancialFormatter()

const chartOption = computed(() => {
  if (!props.data) {
    return { title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999' } } }
  }

  const netMargin = props.data.netMargin ?? 0
  const assetTurnover = (props.data.assetTurnover ?? 0) * 100
  const equityMultiplier = ((props.data.equityMultiplier ?? 0) - 1) * 100
  const roe = props.data.roe ?? 0

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'var(--surface)',
      borderColor: 'var(--border)',
      textStyle: { color: 'var(--text-primary)' },
      formatter: (params: any) => {
        const p = params[0]
        const val = p.dataIndex === 0 ? netMargin : p.dataIndex === 1 ? assetTurnover : p.dataIndex === 2 ? equityMultiplier : roe
        return `${p.name}<br/><b>${val.toFixed(2)}%</b>`
      },
    },
    grid: { left: 120, right: 40, top: 24, bottom: 24 },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: 'var(--border)', type: 'dashed' } },
      axisLabel: { color: 'var(--text-secondary)', formatter: '{value}%' },
    },
    yAxis: {
      type: 'category',
      data: ['权益乘数贡献', '资产周转率贡献', '净利率', 'ROE'],
      axisLine: { lineStyle: { color: 'var(--border)' } },
      axisLabel: { color: 'var(--text-primary)', fontWeight: 600 },
    },
    series: [{
      type: 'bar',
      data: [
        { value: equityMultiplier, itemStyle: { color: '#FF9500', borderRadius: [0, 4, 4, 0] } },
        { value: assetTurnover, itemStyle: { color: '#635BFF', borderRadius: [0, 4, 4, 0] } },
        { value: netMargin, itemStyle: { color: '#00D924', borderRadius: [0, 4, 4, 0] } },
        { value: roe, itemStyle: { color: '#1A56DB', borderRadius: [0, 4, 4, 0] } },
      ],
      barWidth: 28,
      label: {
        show: true,
        position: 'right',
        formatter: (p: any) => `${p.value.toFixed(2)}%`,
        color: 'var(--text-primary)',
      },
    }],
  }
})
</script>

<template>
  <div class="dupont-card">
    <div class="dupont-header">
      <h3 class="dupont-title">杜邦分析</h3>
      <span v-if="data" class="dupont-formula">
        ROE = {{ formatPercent(data.netMargin, 2) }} × {{ formatRatio(data.assetTurnover, 2) }} × {{ formatRatio(data.equityMultiplier, 2) }}
      </span>
    </div>
    <div class="dupont-body">
      <div class="dupont-metrics">
        <div class="dupont-item">
          <div class="dupont-label">净资产收益率 ROE</div>
          <div class="dupont-value strong">{{ data ? formatPercent(data.roe) : '-' }}</div>
        </div>
        <div class="dupont-sep">=</div>
        <div class="dupont-item">
          <div class="dupont-label">净利率</div>
          <div class="dupont-value">{{ data ? formatPercent(data.netMargin) : '-' }}</div>
        </div>
        <div class="dupont-sep">×</div>
        <div class="dupont-item">
          <div class="dupont-label">资产周转率</div>
          <div class="dupont-value">{{ data ? formatRatio(data.assetTurnover) : '-' }}</div>
        </div>
        <div class="dupont-sep">×</div>
        <div class="dupont-item">
          <div class="dupont-label">权益乘数</div>
          <div class="dupont-value">{{ data ? formatRatio(data.equityMultiplier) : '-' }}</div>
        </div>
      </div>
      <v-chart class="chart" :option="chartOption" autoresize />
    </div>
  </div>
</template>

<style scoped>
.dupont-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.dupont-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.dupont-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.dupont-formula {
  font-family: var(--mono);
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
}

.dupont-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: center;
}

@media (max-width: 768px) {
  .dupont-body {
    grid-template-columns: 1fr;
  }
}

.dupont-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dupont-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--bg);
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
}

.dupont-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.dupont-value {
  font-family: var(--mono);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.dupont-value.strong {
  color: var(--primary);
  font-size: 18px;
}

.dupont-sep {
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-muted);
}

.chart {
  width: 100%;
  height: 280px;
}
</style>
