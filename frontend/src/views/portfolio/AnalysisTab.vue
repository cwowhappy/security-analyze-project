<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPositions, getTransactions } from '@/api/portfolio'
import type { Position, Transaction } from '@/types/portfolio'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'

use([CanvasRenderer, PieChart, LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const props = defineProps<{
  portfolioId: number
}>()

const positions = ref<Position[]>([])
const transactions = ref<Transaction[]>([])
const loading = ref(false)

const totalRealizedPnl = computed(() =>
  positions.value.reduce((sum, p) => sum + (p.realizedPnl || 0), 0)
)

const totalFloatingPnl = computed(() =>
  positions.value.reduce((sum, p) => sum + (p.floatingPnl || 0), 0)
)

const totalReturn = computed(() => totalRealizedPnl.value + totalFloatingPnl.value)

// 行业分布饼图数据
const industryPieData = computed(() => {
  const map: Record<string, number> = {}
  positions.value.forEach(p => {
    const industry = p.industry || '未知'
    const mv = p.marketValue || 0
    map[industry] = (map[industry] || 0) + mv
  })
  return Object.entries(map).map(([name, value]) => ({ name, value: Number(value.toFixed(2)) }))
})

const industryPieOption = computed(() => ({
  backgroundColor: 'transparent',
  title: { text: '行业分布（按市值）', left: 'center', textStyle: { color: '#E8EAED' } },
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)', backgroundColor: 'rgba(17, 19, 24, 0.95)', borderColor: 'rgba(43, 106, 255, 0.20)', textStyle: { color: '#E8EAED' } },
  legend: { textStyle: { color: '#9CA3AF' } },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 10, borderColor: '#0D0F14', borderWidth: 2 },
    label: { show: false, position: 'center' },
    emphasis: { label: { show: true, fontSize: 20, fontWeight: 'bold', color: '#E8EAED' } },
    data: industryPieData.value,
  }],
}))

// 盈亏趋势：按交易日期汇总已实现盈亏
const pnlTrendData = computed(() => {
  const map: Record<string, number> = {}
  transactions.value.forEach(tx => {
    if (tx.realizedPnl) {
      map[tx.tradeDate] = (map[tx.tradeDate] || 0) + tx.realizedPnl
    }
  })
  const sorted = Object.entries(map).sort((a, b) => a[0].localeCompare(b[0]))
  return {
    dates: sorted.map(([d]) => d),
    values: sorted.map(([, v]) => Number(v.toFixed(2))),
  }
})

const pnlTrendOption = computed(() => ({
  backgroundColor: 'transparent',
  title: { text: '已实现盈亏趋势', left: 'center', textStyle: { color: '#E8EAED' } },
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(17, 19, 24, 0.95)', borderColor: 'rgba(43, 106, 255, 0.20)', textStyle: { color: '#E8EAED' } },
  xAxis: { type: 'category', data: pnlTrendData.value.dates, axisLine: { lineStyle: { color: '#4B5563' } }, axisLabel: { color: '#9CA3AF' } },
  yAxis: { type: 'value', name: '盈亏', axisLine: { lineStyle: { color: '#4B5563' } }, axisLabel: { color: '#9CA3AF' }, splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } } },
  series: [{
    data: pnlTrendData.value.values,
    type: 'line',
    smooth: true,
    lineStyle: { color: '#2B6AFF' },
    itemStyle: { color: '#2B6AFF' },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(43,106,255,0.25)' },
          { offset: 1, color: 'rgba(43,106,255,0.04)' },
        ],
      },
    },
  }],
}))

async function fetchData() {
  loading.value = true
  try {
    const [posRes, txRes] = await Promise.all([
      getPositions(props.portfolioId),
      getTransactions(props.portfolioId, { size: 1000 }),
    ])
    positions.value = posRes
    transactions.value = txRes.items
  } catch {
    ElMessage.error('加载分析数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

defineExpose({ refresh: fetchData })
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="16" class="summary-row">
      <el-col :span="8">
        <el-card>
          <div class="stat-label">累计已实现盈亏</div>
          <div class="stat-value" :class="totalRealizedPnl >= 0 ? 'up' : 'down'">
            {{ totalRealizedPnl.toFixed(2) }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-label">浮动盈亏</div>
          <div class="stat-value" :class="totalFloatingPnl >= 0 ? 'up' : 'down'">
            {{ totalFloatingPnl.toFixed(2) }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <div class="stat-label">总收益</div>
          <div class="stat-value" :class="totalReturn >= 0 ? 'up' : 'down'">
            {{ totalReturn.toFixed(2) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card>
          <VChart class="chart" :option="industryPieOption" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <VChart class="chart" :option="pnlTrendOption" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.summary-row {
  margin-bottom: 20px;
}
.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  font-family: var(--font-mono);
}
.up { color: var(--up-color); }
.down { color: var(--down-color); }
.chart-row {
  margin-bottom: 20px;
}
.chart {
  width: 100%;
  height: 300px;
}
</style>
