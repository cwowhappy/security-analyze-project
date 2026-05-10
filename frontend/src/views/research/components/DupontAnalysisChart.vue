<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { TreeChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { AnnualMetric } from '@/types/research'
import { NEON_COLORS } from '@/styles/echarts-theme'

use([CanvasRenderer, TreeChart, TooltipComponent])

const props = defineProps<{
  metrics: AnnualMetric[]
}>()

function safeDiv(a?: number, b?: number): number | undefined {
  if (a == null || b == null || b === 0) return undefined
  return a / b
}

function safeMul(a?: number, b?: number): number | undefined {
  if (a == null || b == null) return undefined
  return a * b
}

function fmtMoney(val?: number): string {
  if (val == null || Number.isNaN(val)) return '-'
  return (val / 1e8).toFixed(2) + '亿'
}

function fmtPercent(val?: number): string {
  if (val == null || Number.isNaN(val)) return '-'
  return val.toFixed(2) + '%'
}

function fmtRatio(val?: number, unit?: string): string {
  if (val == null || Number.isNaN(val)) return '-'
  return val.toFixed(2) + (unit || '')
}

const latestMetric = computed(() => {
  if (!props.metrics?.length) return null
  // 按 reportYear 升序排列后取最后一个，确保取到最新年度
  const sorted = [...props.metrics].sort((a, b) => a.reportYear - b.reportYear)
  return sorted[sorted.length - 1]
})

const hasData = computed(() => latestMetric.value != null)

const chartOption = computed(() => {
  const m = latestMetric.value
  if (!m) {
    return {
      title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#9CA3AF' } },
    }
  }

  // 优先使用预计算指标；若缺失则用实时计算 fallback（带安全除法）
  const netMargin = m.netMargin
  const assetTurnover = m.assetTurnover ?? safeDiv(m.operateIncome, m.totalAssets)
  const equityMultiplier = m.equityMultiplier ?? safeDiv(m.totalAssets, m.totalEquity)

  // ROE fallback: 若预计算 ROE 缺失，用杜邦公式计算
  let roe = m.roe
  if (roe == null && netMargin != null && assetTurnover != null && equityMultiplier != null) {
    roe = safeMul(safeMul(netMargin, assetTurnover), equityMultiplier)
  }

  const data = {
    name: `ROE\n${fmtRatio(roe, '%')}`,
    itemStyle: { color: NEON_COLORS[4] },
    label: { fontSize: 14, fontWeight: 'bold' },
    children: [
      {
        name: `净利率\n${fmtPercent(netMargin)}`,
        itemStyle: { color: NEON_COLORS[3] },
        children: [
          { name: `归母净利润\n${fmtMoney(m.parentNetProfit)}`, itemStyle: { color: NEON_COLORS[3] + 'aa' } },
          { name: `营业收入\n${fmtMoney(m.operateIncome)}`, itemStyle: { color: NEON_COLORS[3] + 'aa' } },
        ],
      },
      {
        name: `资产周转率\n${fmtRatio(assetTurnover, '次')}`,
        itemStyle: { color: NEON_COLORS[0] },
        children: [
          { name: `营业收入\n${fmtMoney(m.operateIncome)}`, itemStyle: { color: NEON_COLORS[0] + 'aa' } },
          { name: `总资产\n${fmtMoney(m.totalAssets)}`, itemStyle: { color: NEON_COLORS[0] + 'aa' } },
        ],
      },
      {
        name: `权益乘数\n${fmtRatio(equityMultiplier)}`,
        itemStyle: { color: NEON_COLORS[2] },
        children: [
          { name: `总资产\n${fmtMoney(m.totalAssets)}`, itemStyle: { color: NEON_COLORS[2] + 'aa' } },
          { name: `股东权益\n${fmtMoney(m.totalEquity)}`, itemStyle: { color: NEON_COLORS[2] + 'aa' } },
        ],
      },
    ],
  }

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
      backgroundColor: 'rgba(17, 19, 24, 0.95)',
      borderColor: 'rgba(43, 106, 255, 0.20)',
      textStyle: { color: '#E8EAED' },
      formatter: (params: any) => {
        const name = params.name.replace('\n', ': ')
        return `<div style="font-size:13px">${name}</div>`
      },
    },
    series: [
      {
        type: 'tree',
        data: [data],
        top: '5%',
        left: '10%',
        bottom: '5%',
        right: '10%',
        symbolSize: 8,
        orient: 'TB',
        label: {
          position: 'top',
          verticalAlign: 'middle',
          align: 'center',
          fontSize: 12,
          color: '#E8EAED',
          lineHeight: 16,
        },
        leaves: {
          label: {
            position: 'bottom',
            verticalAlign: 'middle',
            align: 'center',
            fontSize: 11,
            color: '#9CA3AF',
          },
        },
        emphasis: {
          focus: 'descendant',
        },
        expandAndCollapse: false,
        animationDuration: 550,
        animationDurationUpdate: 750,
        lineStyle: {
          color: 'rgba(255,255,255,0.15)',
          width: 1.5,
        },
      },
    ],
  }
})
</script>

<template>
  <div class="dupont-chart-wrapper">
    <div class="chart-title">杜邦分析拆解</div>
    <VChart v-if="hasData" :option="chartOption" autoresize style="height: calc(100% - 28px); width: 100%" />
    <div v-else class="dupont-empty">暂无数据</div>
  </div>
</template>

<style scoped>
.dupont-chart-wrapper {
  height: 360px;
  background: var(--card-bg);
  border-radius: var(--radius-md);
  padding: var(--card-padding);
  position: relative;
}
.chart-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary, #e5e7eb);
  margin-bottom: 8px;
}
.dupont-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary, #9ca3af);
  font-size: 13px;
}
</style>
