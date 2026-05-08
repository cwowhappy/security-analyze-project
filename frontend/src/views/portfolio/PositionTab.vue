<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPositions } from '@/api/portfolio'
import type { Position } from '@/types/portfolio'

const props = defineProps<{
  portfolioId: number
}>()

const positions = ref<Position[]>([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    positions.value = await getPositions(props.portfolioId)
  } catch {
    ElMessage.error('加载持仓失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

defineExpose({ refresh: fetchData })
</script>

<template>
  <el-table :data="positions" v-loading="loading" stripe>
    <el-table-column prop="stockCode" label="股票代码" width="110" />
    <el-table-column prop="stockName" label="股票简称" width="120" />
    <el-table-column prop="industry" label="行业" width="120" />
    <el-table-column prop="currentQuantity" label="持仓股数" width="110" align="right">
      <template #default="{ row }">
        {{ row.currentQuantity?.toFixed(2) }}
      </template>
    </el-table-column>
    <el-table-column prop="avgCost" label="平均成本" width="110" align="right">
      <template #default="{ row }">
        {{ row.avgCost?.toFixed(4) }}
      </template>
    </el-table-column>
    <el-table-column prop="closePrice" label="最新价" width="100" align="right">
      <template #default="{ row }">
        {{ row.closePrice?.toFixed(2) ?? '-' }}
      </template>
    </el-table-column>
    <el-table-column prop="marketValue" label="市值" width="120" align="right">
      <template #default="{ row }">
        {{ row.marketValue?.toFixed(2) ?? '-' }}
      </template>
    </el-table-column>
    <el-table-column prop="totalCost" label="持仓成本" width="110" align="right">
      <template #default="{ row }">
        {{ row.totalCost?.toFixed(2) }}
      </template>
    </el-table-column>
    <el-table-column prop="floatingPnl" label="浮动盈亏" width="110" align="right">
      <template #default="{ row }">
        <span v-if="row.floatingPnl != null" :class="row.floatingPnl >= 0 ? 'up' : 'down'">
          {{ row.floatingPnl.toFixed(2) }}
        </span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column prop="floatingPnlRate" label="浮动盈亏率" width="110" align="right">
      <template #default="{ row }">
        <span v-if="row.floatingPnlRate != null" :class="row.floatingPnlRate >= 0 ? 'up' : 'down'">
          {{ (row.floatingPnlRate * 100).toFixed(2) }}%
        </span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column prop="realizedPnl" label="已实现盈亏" width="110" align="right">
      <template #default="{ row }">
        <span v-if="row.realizedPnl != null" :class="row.realizedPnl >= 0 ? 'up' : 'down'">
          {{ row.realizedPnl.toFixed(2) }}
        </span>
        <span v-else>-</span>
      </template>
    </el-table-column>
    <el-table-column prop="weight" label="权重" width="80" align="right">
      <template #default="{ row }">
        {{ row.weight != null ? row.weight.toFixed(2) + '%' : '-' }}
      </template>
    </el-table-column>
    <el-table-column prop="firstBuyDate" label="首次买入" width="110" />
    <el-table-column prop="lastTradeDate" label="最近交易" width="110" />
  </el-table>
  <el-empty v-if="!loading && positions.length === 0" description="暂无持仓" />
</template>

<style scoped>
.up { color: var(--up-color); }
.down { color: var(--down-color); }
</style>
