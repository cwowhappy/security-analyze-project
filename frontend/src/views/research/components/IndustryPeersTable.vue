<script setup lang="ts">
import { ElEmpty } from 'element-plus'
import type { PeerMetric } from '@/types/research'

const props = defineProps<{
  peers: PeerMetric[]
}>()

const emit = defineEmits<{
  select: [stockCode: string]
}>()

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
</script>

<template>
  <table v-if="peers.length > 0" class="peers-table">
    <thead>
      <tr>
        <th>股票代码</th>
        <th>公司名称</th>
        <th>营收</th>
        <th>净利润</th>
        <th>ROE</th>
        <th>负债率</th>
      </tr>
    </thead>
    <tbody>
      <tr
        v-for="peer in peers"
        :key="peer.stockCode"
        class="peer-row"
        @click="emit('select', peer.stockCode)"
      >
        <td>{{ peer.stockCode }}</td>
        <td>{{ peer.stockName }}</td>
        <td>{{ formatMoney(peer.totalRevenue) }}</td>
        <td>{{ formatMoney(peer.parentNetProfit) }}</td>
        <td>{{ formatPercent(peer.roe) }}</td>
        <td>{{ formatPercent(peer.debtRatio) }}</td>
      </tr>
    </tbody>
  </table>
  <ElEmpty v-else description="暂无同行业对比数据" />
</template>

<style scoped>
.peers-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.peers-table th,
.peers-table td {
  padding: 10px 12px;
  text-align: right;
  border-bottom: 1px solid var(--border-color, rgba(255,255,255,0.06));
}
.peers-table th {
  color: var(--text-secondary, #9ca3af);
  font-weight: 500;
}
.peers-table td {
  color: var(--text-primary, #e5e7eb);
}
.peer-row {
  cursor: pointer;
}
.peer-row:hover {
  background: var(--bg-card);
}
</style>
