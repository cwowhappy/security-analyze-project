<script setup lang="ts">
import IndicatorCard from './IndicatorCard.vue'
import type { FinancialIndicator } from '@/types/financial'

const props = defineProps<{
  indicator: FinancialIndicator | null
}>()

const cards = [
  { key: 'roe', label: 'ROE', isPercent: true },
  { key: 'roa', label: 'ROA', isPercent: true },
  { key: 'grossMargin', label: '毛利率', isPercent: true },
  { key: 'netMargin', label: '净利率', isPercent: true },
  { key: 'debtRatio', label: '资产负债率', isPercent: true },
  { key: 'currentRatio', label: '流动比率', isRatio: true },
  { key: 'revenueGrowth', label: '营收增速', isPercent: true },
  { key: 'npParentGrowth', label: '归母净利增速', isPercent: true },
  { key: 'pe', label: 'PE-TTM', isRatio: true },
  { key: 'pb', label: 'PB', isRatio: true },
  { key: 'assetTurnover', label: '总资产周转率', isRatio: true },
  { key: 'cfoToNp', label: '经营现金流/净利润', isPercent: true },
]

function getValue(key: string): number | null {
  if (!props.indicator) return null
  return (props.indicator as unknown as Record<string, number | null>)[key] ?? null
}
</script>

<template>
  <div class="indicator-grid">
    <IndicatorCard
      v-for="card in cards"
      :key="card.key"
      :label="card.label"
      :value="getValue(card.key)"
      :is-percent="card.isPercent"
      :is-ratio="card.isRatio"
    />
  </div>
</template>

<style scoped>
.indicator-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

@media (max-width: 1024px) {
  .indicator-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .indicator-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}
</style>
