<script setup lang="ts">
interface MetricCard {
  label: string
  value: string
  color: string
  yoyValue?: number
  yoyDirection?: 'up' | 'down' | 'flat'
}

defineProps<{
  cards: MetricCard[]
}>()
</script>

<template>
  <div class="metric-dashboard">
    <div
      v-for="card in cards"
      :key="card.label"
      class="dashboard-card"
      :style="{ borderTop: `3px solid ${card.color}` }"
    >
      <div class="dashboard-label">{{ card.label }}</div>
      <div class="dashboard-value" :style="{ color: card.color }">{{ card.value }}</div>
      <div v-if="card.yoyValue != null" class="dashboard-yoy" :class="card.yoyDirection">
        <span class="yoy-arrow">{{ card.yoyDirection === 'up' ? '▲' : card.yoyDirection === 'down' ? '▼' : '—' }}</span>
        <span class="yoy-text">{{ Math.abs(card.yoyValue).toFixed(2) }}%</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.metric-dashboard {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
@media (max-width: 768px) {
  .metric-dashboard {
    grid-template-columns: repeat(2, 1fr);
  }
}
.dashboard-card {
  background: var(--card-bg);
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}
.dashboard-label {
  font-size: 13px;
  color: var(--text-secondary, #9ca3af);
  margin-bottom: 8px;
}
.dashboard-value {
  font-size: 18px;
  font-weight: 600;
  font-family: var(--font-mono, monospace);
}
.dashboard-yoy {
  font-size: 11px;
  margin-top: 4px;
  font-family: var(--font-mono, monospace);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}
.dashboard-yoy.up {
  color: var(--up-color);
}
.dashboard-yoy.down {
  color: var(--down-color);
}
.dashboard-yoy.flat {
  color: var(--text-tertiary);
}
.yoy-arrow {
  font-size: 10px;
}
.yoy-text {
  font-size: 11px;
}
</style>
