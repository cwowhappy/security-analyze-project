<script setup lang="ts">
import { computed } from 'vue'
import type { IndustryRankItem } from '@/types/research'

const props = defineProps<{
  data: IndustryRankItem[]
  currentStockCode?: string
  sortBy?: string
  order?: string
}>()

const emit = defineEmits<{
  select: [stockCode: string]
  sort: [field: string]
}>()

const headers = [
  { key: 'rank', label: '排名', width: '60px' },
  { key: 'stockCode', label: '代码', width: '90px' },
  { key: 'stockName', label: '名称', width: '120px' },
  { key: 'totalRevenue', label: '营收', width: '100px', sortable: true, sortKey: 'revenue' },
  { key: 'parentNetProfit', label: '净利润', width: '100px', sortable: true, sortKey: 'profit' },
  { key: 'grossMargin', label: '毛利率', width: '90px', sortable: true, sortKey: 'grossMargin' },
  { key: 'roe', label: 'ROE', width: '90px', sortable: true, sortKey: 'roe' },
  { key: 'debtRatio', label: '负债率', width: '90px', sortable: true, sortKey: 'debtRatio' },
]

function formatMoney(val?: number): string {
  if (val == null) return '-'
  const abs = Math.abs(val)
  if (abs >= 1e8) return (val / 1e8).toFixed(2) + ' 亿'
  if (abs >= 1e4) return (val / 1e4).toFixed(2) + ' 万'
  return val.toLocaleString()
}

function formatPercent(val?: number): string {
  if (val == null) return '-'
  return val.toFixed(2) + '%'
}

function onSort(field: string) {
  emit('sort', field)
}

const sortedData = computed(() => {
  // 后端已排序，前端只需展示
  return props.data.map((item, index) => ({
    ...item,
    rank: index + 1,
  }))
})
</script>

<template>
  <div class="rank-table-wrapper">
    <table class="rank-table">
      <thead>
        <tr>
          <th
            v-for="h in headers"
            :key="h.key"
            :style="{ width: h.width }"
            :class="{ sortable: h.sortable, active: sortBy === h.sortKey }"
            @click="h.sortable ? onSort(h.sortKey!) : undefined"
          >
            {{ h.label }}
            <span v-if="h.sortable" class="sort-icon">
              {{ sortBy === h.sortKey ? (order === 'asc' ? '▲' : '▼') : '⇅' }}
            </span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="row in sortedData"
          :key="row.stockCode"
          :class="{ highlight: row.stockCode === currentStockCode }"
          @click="emit('select', row.stockCode)"
        >
          <td>{{ row.rank }}</td>
          <td>{{ row.stockCode }}</td>
          <td>{{ row.stockName }}</td>
          <td>{{ formatMoney(row.totalRevenue) }}</td>
          <td>{{ formatMoney(row.parentNetProfit) }}</td>
          <td>{{ formatPercent(row.grossMargin) }}</td>
          <td>{{ formatPercent(row.roe) }}</td>
          <td>{{ formatPercent(row.debtRatio) }}</td>
        </tr>
        <tr v-if="!sortedData.length">
          <td colspan="8" class="empty-cell">暂无排名数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.rank-table-wrapper {
  overflow-x: auto;
}
.rank-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.rank-table thead th {
  text-align: left;
  padding: 10px 8px;
  color: var(--text-secondary, #9ca3af);
  font-weight: 500;
  border-bottom: 1px solid var(--border-color, rgba(255,255,255,0.06));
  white-space: nowrap;
}
.rank-table thead th.sortable {
  cursor: pointer;
  user-select: none;
}
.rank-table thead th.sortable:hover {
  color: var(--text-primary, #e5e7eb);
}
.rank-table thead th.active {
  color: #409eff;
}
.sort-icon {
  font-size: 10px;
  margin-left: 2px;
  opacity: 0.6;
}
.rank-table tbody td {
  padding: 10px 8px;
  color: var(--text-primary, #e5e7eb);
  border-bottom: 1px solid var(--border-color, rgba(255,255,255,0.04));
}
.rank-table tbody tr {
  cursor: pointer;
  transition: background 0.15s;
}
.rank-table tbody tr:hover {
  background: var(--card-hover-bg, rgba(255,255,255,0.04));
}
.rank-table tbody tr.highlight {
  background: rgba(64, 158, 255, 0.1);
}
.rank-table tbody tr.highlight td {
  font-weight: 600;
}
.empty-cell {
  text-align: center;
  color: var(--text-secondary, #9ca3af);
  padding: 24px 0;
}
</style>
