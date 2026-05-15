<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { PeerComparison } from '@/types/financial'
import { useFinancialFormatter } from '../composables/useFinancialFormatter'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent])

const props = defineProps<{
  data: PeerComparison | null
}>()

const { formatPercent, formatRatio } = useFinancialFormatter()

const isPercentMetric = computed(() => {
  if (!props.data) return false
  const percentMetrics = ['roe', 'roa', 'grossMargin', 'netMargin', 'netMarginExcl', 'revenueGrowth', 'npParentGrowth', 'npExclGrowth', 'debtRatio', 'dividendYield']
  return percentMetrics.includes(props.data.metric)
})

const formatValue = (val: number | null) => {
  if (val === null) return '-'
  return isPercentMetric.value ? formatPercent(val) : formatRatio(val)
}

const chartOption = computed(() => {
  if (!props.data || !props.data.industryAvg) {
    return { title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999' } } }
  }

  const d = props.data
  const categories = ['最低值', '当前股票', '行业中位数', '行业均值', '最高值']
  const values = [
    d.industryMin ?? 0,
    d.stockValue ?? 0,
    d.industryMedian ?? 0,
    d.industryAvg ?? 0,
    d.industryMax ?? 0,
  ]
  const colors = ['#FF3B30', '#635BFF', '#00D924', '#1A56DB', '#FF9500']

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'var(--surface)',
      borderColor: 'var(--border)',
      textStyle: { color: 'var(--text-primary)' },
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/><b>${formatValue(p.value)}</b>`
      },
    },
    grid: { left: 48, right: 24, top: 24, bottom: 24 },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: { lineStyle: { color: 'var(--border)' } },
      axisLabel: { color: 'var(--text-secondary)' },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: 'var(--border)', type: 'dashed' } },
      axisLabel: { color: 'var(--text-secondary)' },
    },
    series: [{
      type: 'bar',
      data: values.map((v, i) => ({
        value: v,
        itemStyle: { color: colors[i], borderRadius: [4, 4, 0, 0] },
      })),
      barWidth: 40,
      label: {
        show: true,
        position: 'top',
        formatter: (p: any) => formatValue(p.value),
        color: 'var(--text-primary)',
        fontSize: 11,
      },
    }],
  }
})

const peerList = computed(() => {
  if (!props.data || !props.data.peers) return []
  return [...props.data.peers].sort((a, b) => (b.value ?? 0) - (a.value ?? 0)).slice(0, 10)
})
</script>

<template>
  <div class="peer-card">
    <div class="peer-header">
      <h3 class="peer-title">
        同业对比 — {{ data?.metricName || data?.metric || '-' }}
      </h3>
      <span v-if="data" class="peer-count">共 {{ data.peers?.length || 0 }} 家同业</span>
    </div>

    <div class="peer-body">
      <v-chart class="chart" :option="chartOption" autoresize />

      <div v-if="peerList.length > 0" class="peer-rank">
        <div class="rank-header">
          <span>排名</span>
          <span>股票</span>
          <span>指标值</span>
        </div>
        <div
          v-for="(peer, idx) in peerList"
          :key="peer.stockCode"
          :class="['rank-row', { highlight: peer.stockCode === data?.stockCode }]"
        >
          <span class="rank-num">{{ idx + 1 }}</span>
          <span class="rank-name">{{ peer.stockName || peer.stockCode }}</span>
          <span class="rank-value">{{ formatValue(peer.value) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.peer-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.peer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.peer-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.peer-count {
  font-size: 13px;
  color: var(--text-muted);
  background: var(--bg);
  padding: 2px 8px;
  border-radius: 4px;
}

.peer-body {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
  align-items: start;
}

@media (max-width: 768px) {
  .peer-body {
    grid-template-columns: 1fr;
  }
}

.chart {
  width: 100%;
  height: 320px;
}

.peer-rank {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  max-height: 320px;
  overflow-y: auto;
}

.rank-header,
.rank-row {
  display: grid;
  grid-template-columns: 40px 1fr 80px;
  gap: 8px;
  padding: 8px 12px;
  font-size: 13px;
}

.rank-header {
  background: var(--surface-variant);
  font-weight: 600;
  color: var(--text-secondary);
  position: sticky;
  top: 0;
  z-index: 2;
}

.rank-row {
  border-bottom: 1px solid var(--border);
  color: var(--text-primary);
}

.rank-row:last-child {
  border-bottom: none;
}

.rank-row.highlight {
  background: var(--primary-light);
}

.rank-row.highlight .rank-name,
.rank-row.highlight .rank-value {
  font-weight: 700;
  color: var(--primary);
}

.rank-num {
  color: var(--text-muted);
  font-family: var(--mono);
}

.rank-value {
  text-align: right;
  font-family: var(--mono);
}
</style>
