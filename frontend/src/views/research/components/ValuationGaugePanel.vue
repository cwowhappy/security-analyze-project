<script setup lang="ts">
import { computed } from 'vue'
import type { ValuationOverview } from '@/types/research'

const props = defineProps<{
  data: ValuationOverview
}>()

function formatNumber(val?: number): string {
  if (val == null || isNaN(val)) return '-'
  return val.toFixed(2)
}

function formatMoney(val?: number): string {
  if (val == null || isNaN(val)) return '-'
  const abs = Math.abs(val)
  if (abs >= 1e12) return (val / 1e12).toFixed(2) + ' 万亿'
  if (abs >= 1e8) return (val / 1e8).toFixed(2) + ' 亿'
  if (abs >= 1e4) return (val / 1e4).toFixed(2) + ' 万'
  return val.toLocaleString()
}

import { getPercentileColor } from '@/utils/colors'

function getPercentileColorHex(p?: number): string {
  if (p == null) return 'var(--text-tertiary)'
  return getPercentileColor(p)
}

function getPercentileLabel(p?: number): string {
  if (p == null) return '无数据'
  if (p < 0.3) return '低估'
  if (p > 0.7) return '高估'
  return '合理'
}

const gauges = computed(() => [
  {
    label: '当前股价',
    value: formatNumber(props.data.currentPrice),
    sub: '市值 ' + formatMoney(props.data.marketCap),
    percent: null as number | null,
  },
  {
    label: 'PE(TTM)',
    value: formatNumber(props.data.peTtm),
    sub: getPercentileLabel(props.data.peTtmPercentile),
    percent: props.data.peTtmPercentile != null ? props.data.peTtmPercentile * 100 : null,
    color: getPercentileColorHex(props.data.peTtmPercentile),
  },
  {
    label: 'PB',
    value: formatNumber(props.data.pb),
    sub: getPercentileLabel(props.data.pbPercentile),
    percent: props.data.pbPercentile != null ? props.data.pbPercentile * 100 : null,
    color: getPercentileColorHex(props.data.pbPercentile),
  },
  {
    label: 'PS(TTM)',
    value: formatNumber(props.data.psTtm),
    sub: getPercentileLabel(props.data.psTtmPercentile),
    percent: props.data.psTtmPercentile != null ? props.data.psTtmPercentile * 100 : null,
    color: getPercentileColorHex(props.data.psTtmPercentile),
  },
])
</script>

<template>
  <div class="gauge-panel">
    <div
      v-for="(g, idx) in gauges"
      :key="idx"
      class="gauge-card"
    >
      <div class="gauge-label">{{ g.label }}</div>
      <div class="gauge-value" :style="{ color: g.color || 'var(--text-primary, #e5e7eb)' }">
        {{ g.value }}
      </div>
      <div class="gauge-sub" :style="{ color: g.color || 'var(--text-secondary, #9ca3af)' }">
        {{ g.sub }}
      </div>
      <div v-if="g.percent != null" class="gauge-bar-bg">
        <div
          class="gauge-bar-fill"
          :style="{ width: g.percent + '%', background: g.color }"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.gauge-panel {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.gauge-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}
.gauge-label {
  font-size: 13px;
  color: var(--text-secondary, #9ca3af);
  margin-bottom: 8px;
}
.gauge-value {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
}
.gauge-sub {
  font-size: 12px;
  margin-bottom: 10px;
}
.gauge-bar-bg {
  height: 4px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
  overflow: hidden;
}
.gauge-bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.5s ease;
}
@media (max-width: 768px) {
  .gauge-panel {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
