<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElEmpty } from 'element-plus'
import { getValuationOverview, getValuationHistory } from '@/api/research'
import type { ValuationOverview, ValuationHistoryItem } from '@/types/research'
import ValuationGaugePanel from '@/views/research/components/ValuationGaugePanel.vue'
import CompositeScoreCard from '@/views/research/components/CompositeScoreCard.vue'
import ValuationTrendChart from '@/views/research/components/ValuationTrendChart.vue'
import DcfCalculator from '@/views/research/components/DcfCalculator.vue'

const props = defineProps<{
  stockCode: string
}>()

const loading = ref(false)
const valuationOverview = ref<ValuationOverview | null>(null)
const valuationHistory = ref<ValuationHistoryItem[]>([])

async function fetchData() {
  loading.value = true
  try {
    const [valRes, valHistRes] = await Promise.all([
      getValuationOverview(props.stockCode),
      getValuationHistory(props.stockCode),
    ])
    valuationOverview.value = valRes
    valuationHistory.value = valHistRes.items
  } catch (err) {
    valuationOverview.value = null
    valuationHistory.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
watch(() => props.stockCode, fetchData)
</script>

<template>
  <div v-loading="loading">
    <template v-if="valuationOverview">
      <ValuationGaugePanel :data="valuationOverview" />
      <CompositeScoreCard
        :score="valuationOverview.compositeScore"
        :warnings="valuationOverview.warnings"
      />
      <ValuationTrendChart :items="valuationHistory" />
      <DcfCalculator :stock-code="props.stockCode" />
    </template>
    <ElEmpty
      v-else
      description="暂无估值数据，请确认行情采集和估值计算任务已完成"
      :image-size="80"
    />
  </div>
</template>
