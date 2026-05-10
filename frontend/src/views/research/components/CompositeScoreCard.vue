<script setup lang="ts">
import type { CompositeScore, ValuationWarning } from '@/types/research'
import { getScoreColor, getWarningColor, getWarningColorDim } from '@/utils/colors'

const props = defineProps<{
  score: CompositeScore
  warnings: ValuationWarning[]
}>()

function getWarningClass(level: string): string {
  switch (level) {
    case 'high': return 'warning-high'
    case 'medium': return 'warning-medium'
    default: return 'warning-low'
  }
}
</script>

<template>
  <div class="score-card">
    <div class="score-main">
      <div class="score-ring-wrapper">
        <svg class="score-ring" viewBox="0 0 120 120">
          <circle class="ring-bg" cx="60" cy="60" r="50" />
          <circle
            class="ring-fill"
            cx="60" cy="60" r="50"
            :stroke="getScoreColor(score.overallScore)"
            :stroke-dasharray="`${score.overallScore * 3.14} 314`"
          />
        </svg>
        <div class="score-number" :style="{ color: getScoreColor(score.overallScore) }">
          {{ score.overallScore }}
        </div>
      </div>
      <div class="score-label">综合评分</div>
    </div>
    <div class="score-details">
      <div class="score-item">
        <span class="score-item-label">财务健康</span>
        <span class="score-item-value" :style="{ color: getScoreColor(score.financialHealthScore) }">
          {{ score.financialHealthScore }}
        </span>
      </div>
      <div class="score-item">
        <span class="score-item-label">估值吸引力</span>
        <span class="score-item-value" :style="{ color: getScoreColor(score.valuationAppealScore) }">
          {{ score.valuationAppealScore }}
        </span>
      </div>
    </div>
    <div v-if="warnings.length > 0" class="warnings">
      <div
        v-for="(w, idx) in warnings"
        :key="idx"
        class="warning-tag"
        :class="getWarningClass(w.level)"
      >
        {{ w.message }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.score-card {
  display: flex;
  align-items: center;
  gap: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}
.score-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.score-ring-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
}
.score-ring {
  width: 80px;
  height: 80px;
  transform: rotate(-90deg);
}
.ring-bg {
  fill: none;
  stroke: rgba(255, 255, 255, 0.08);
  stroke-width: 8;
}
.ring-fill {
  fill: none;
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dasharray 0.6s ease;
}
.score-number {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 22px;
  font-weight: 700;
}
.score-label {
  font-size: 12px;
  color: var(--text-secondary, #9ca3af);
}
.score-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-width: 120px;
}
.score-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}
.score-item-label {
  color: var(--text-secondary, #9ca3af);
}
.score-item-value {
  font-weight: 600;
  font-size: 16px;
}
.warnings {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 200px;
}
.warning-tag {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
}
.warning-high {
  background: var(--danger-color-dim);
  color: var(--danger-color);
  border: 1px solid rgba(239, 68, 68, 0.2);
}
.warning-medium {
  background: var(--warning-color-dim);
  color: var(--warning-color);
  border: 1px solid rgba(255, 202, 58, 0.25);
}
.warning-low {
  background: var(--success-color-dim);
  color: var(--success-color);
  border: 1px solid rgba(16, 185, 129, 0.2);
}
@media (max-width: 768px) {
  .score-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
