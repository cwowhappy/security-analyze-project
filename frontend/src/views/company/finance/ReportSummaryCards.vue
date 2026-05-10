<script setup lang="ts">
import { ElCard } from 'element-plus'
import type { FinanceSummary } from '@/types/finance'
import { getChartColor } from '@/utils/colors'

const props = defineProps<{
  summary: FinanceSummary
}>()

function formatMoney(val?: number): string {
  if (val === undefined || val === null) return '-'
  const abs = Math.abs(val)
  if (abs >= 1e8) return (val / 1e8).toFixed(2) + ' 亿'
  if (abs >= 1e4) return (val / 1e4).toFixed(2) + ' 万'
  return val.toLocaleString()
}

const cards = [
  { label: '营业总收入', key: 'totalRevenue' as const, color: getChartColor(0) },
  { label: '归母净利润', key: 'parentNetProfit' as const, color: getChartColor(1) },
  { label: '总资产', key: 'totalAssets' as const, color: getChartColor(2) },
  { label: '净资产', key: 'totalEquity' as const, color: getChartColor(3) },
  { label: '经营现金流', key: 'operatingCashFlow' as const, color: getChartColor(7) },
]
</script>

<template>
  <div class="summary-cards">
    <ElCard
      v-for="card in cards"
      :key="card.key"
      class="summary-card"
      shadow="hover"
      :body-style="{ padding: '16px' }"
    >
      <div class="card-label">{{ card.label }}</div>
      <div class="card-value" :style="{ color: card.color }">
        {{ formatMoney(props.summary[card.key]) }}
      </div>
    </ElCard>
  </div>
</template>

<style scoped>
.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.summary-card {
  text-align: center;
}
.card-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: var(--font-mono);
}
</style>
