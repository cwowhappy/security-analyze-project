<script setup lang="ts">
import { computed } from 'vue'
import { useFinancialFormatter } from '../composables/useFinancialFormatter'

const props = defineProps<{
  label: string
  value: number | null
  unit?: string
  isPercent?: boolean
  isRatio?: boolean
  compareValue?: number | null
  color?: string
}>()

const { formatPercent, formatRatio } = useFinancialFormatter()

const displayValue = computed(() => {
  if (props.value === null || props.value === undefined) return '-'
  if (props.isPercent) return formatPercent(props.value)
  if (props.isRatio) return formatRatio(props.value)
  return `${props.value.toLocaleString('zh-CN')}${props.unit || ''}`
})

const compareDisplay = computed(() => {
  if (props.compareValue === null || props.compareValue === undefined) return null
  const sign = props.compareValue >= 0 ? '+' : ''
  return `${sign}${props.compareValue.toFixed(2)}pp`
})

const valueClass = computed(() => {
  if (props.color) return props.color
  if (props.value === null) return 'neutral'
  if (props.value > 0) return 'positive'
  if (props.value < 0) return 'negative'
  return 'neutral'
})
</script>

<template>
  <div class="indicator-card">
    <div class="indicator-label">{{ label }}</div>
    <div class="indicator-value" :class="valueClass">
      {{ displayValue }}
    </div>
    <div v-if="compareDisplay" class="indicator-compare" :class="compareValue && compareValue > 0 ? 'up' : 'down'">
      {{ compareValue && compareValue > 0 ? '↑' : '↓' }} {{ compareDisplay }}
    </div>
  </div>
</template>

<style scoped>
.indicator-card {
  padding: var(--fin-card-pad, 20px);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  transition: box-shadow 0.15s, border-color 0.15s;
}

.indicator-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--primary);
}

.indicator-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.indicator-value {
  font-family: var(--mono);
  font-size: 26px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  margin-bottom: 6px;
}

.indicator-value.positive { color: var(--indicator-positive); }
.indicator-value.negative { color: var(--indicator-negative); }
.indicator-value.neutral { color: var(--text-muted); }

.indicator-compare {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
  font-size: 12px;
}

.indicator-compare.up {
  background: rgba(0, 217, 36, 0.1);
  color: var(--indicator-positive);
}

.indicator-compare.down {
  background: rgba(255, 59, 48, 0.1);
  color: var(--indicator-negative);
}
</style>
