<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElSlider, ElInputNumber, ElMessage } from 'element-plus'
import { calculateDcf } from '@/api/research'
import type { DcfResponse } from '@/types/research'

const props = defineProps<{
  stockCode: string
}>()

const growthRate = ref(10)
const discountRate = ref(8)
const terminalGrowthRate = ref(3)
const projectionYears = ref(10)

const loading = ref(false)
const result = ref<DcfResponse | null>(null)

const upsideColor = computed(() => {
  if (!result.value?.upsidePercent) return 'var(--text-secondary, #9ca3af)'
  return result.value.upsidePercent > 0 ? 'var(--success-color)' : 'var(--danger-color)'
})

async function fetchDcf() {
  if (!props.stockCode) return
  loading.value = true
  try {
    const res = await calculateDcf(props.stockCode, {
      growthRate: growthRate.value / 100,
      discountRate: discountRate.value / 100,
      terminalGrowthRate: terminalGrowthRate.value / 100,
      projectionYears: projectionYears.value,
    })
    result.value = res
  } catch (err) {
    ElMessage.error('DCF计算失败')
  } finally {
    loading.value = false
  }
}

// 初始加载 + 参数变化时自动计算（debounce 300ms）
let timer: ReturnType<typeof setTimeout> | null = null
watch([growthRate, discountRate, terminalGrowthRate, projectionYears], () => {
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => fetchDcf(), 300)
}, { immediate: true })

function formatMoney(val?: number): string {
  if (val == null || isNaN(val)) return '-'
  return val.toFixed(2)
}
</script>

<template>
  <div class="dcf-wrapper" v-loading="loading">
    <div class="dcf-title">简易 DCF 估值</div>
    <div class="dcf-disclaimer">
      此为简易DCF模型，仅供参考，不构成投资建议。
    </div>
    <div class="dcf-body">
      <div class="dcf-inputs">
        <div class="input-row">
          <span class="input-label">预测期增长率</span>
          <ElSlider v-model="growthRate" :min="0" :max="30" :step="1" style="flex:1" />
          <ElInputNumber v-model="growthRate" :min="0" :max="30" :step="1" size="small" style="width:80px" />
          <span class="input-unit">%</span>
        </div>
        <div class="input-row">
          <span class="input-label">折现率</span>
          <ElSlider v-model="discountRate" :min="5" :max="15" :step="0.5" style="flex:1" />
          <ElInputNumber v-model="discountRate" :min="5" :max="15" :step="0.5" size="small" style="width:80px" />
          <span class="input-unit">%</span>
        </div>
        <div class="input-row">
          <span class="input-label">永续增长率</span>
          <ElSlider v-model="terminalGrowthRate" :min="0" :max="5" :step="0.5" style="flex:1" />
          <ElInputNumber v-model="terminalGrowthRate" :min="0" :max="5" :step="0.5" size="small" style="width:80px" />
          <span class="input-unit">%</span>
        </div>
        <div class="input-row">
          <span class="input-label">预测年限</span>
          <ElSlider v-model="projectionYears" :min="5" :max="20" :step="1" style="flex:1" />
          <ElInputNumber v-model="projectionYears" :min="5" :max="20" :step="1" size="small" style="width:80px" />
          <span class="input-unit">年</span>
        </div>
      </div>

      <div class="dcf-result">
        <div class="result-item">
          <span class="result-label">每股公允价</span>
          <span class="result-value">{{ formatMoney(result?.fairPrice) }}</span>
        </div>
        <div class="result-item">
          <span class="result-label">公允区间</span>
          <span class="result-value">
            {{ formatMoney(result?.fairPriceRangeLow) }} ~ {{ formatMoney(result?.fairPriceRangeHigh) }}
          </span>
        </div>
        <div class="result-item">
          <span class="result-label">相对当前价</span>
          <span class="result-value" :style="{ color: upsideColor }">
            {{ result?.upsidePercent != null ? (result.upsidePercent > 0 ? '+' : '') + result.upsidePercent.toFixed(2) + '%' : '-' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dcf-wrapper {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
}
.dcf-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary, #e5e7eb);
  margin-bottom: 4px;
}
.dcf-disclaimer {
  font-size: 12px;
  color: var(--text-secondary, #9ca3af);
  margin-bottom: 16px;
}
.dcf-body {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 24px;
}
.dcf-inputs {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.input-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.input-label {
  font-size: 13px;
  color: var(--text-secondary, #9ca3af);
  width: 90px;
  flex-shrink: 0;
}
.input-unit {
  font-size: 13px;
  color: var(--text-secondary, #9ca3af);
  width: 20px;
}
.dcf-result {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
  background: var(--bg-hover);
  border-radius: 6px;
  padding: 20px;
}
.result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.result-label {
  font-size: 13px;
  color: var(--text-secondary, #9ca3af);
}
.result-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #e5e7eb);
}
@media (max-width: 768px) {
  .dcf-body {
    grid-template-columns: 1fr;
  }
}
</style>
