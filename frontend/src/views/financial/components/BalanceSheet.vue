<script setup lang="ts">
import { computed } from 'vue'
import { useFinancialFormatter } from '../composables/useFinancialFormatter'
import FinancialStatementTable from './FinancialStatementTable.vue'
import type { FinancialBalance } from '@/types/financial'
import type { TableRow } from './FinancialStatementTable.vue'

const props = defineProps<{
  data: FinancialBalance[]
}>()

const { formatMoney, formatPercent, formatReportDate } = useFinancialFormatter()

const reportTypeLabels: Record<string, string> = {
  Y: '年报',
  Q1: '一季报',
  Q2: '半年报',
  Q3: '三季报',
}

const reportTypeOrder = ['Y', 'Q1', 'Q2', 'Q3']

function buildRows(list: FinancialBalance[]): TableRow[] {
  const sorted = [...list].sort(
    (a, b) => new Date(b.reportDate).getTime() - new Date(a.reportDate).getTime(),
  )

  return [
    { label: '报告期', values: sorted.map((d) => formatReportDate(d.reportDate, d.reportType)) },
    { label: '货币资金', values: sorted.map((d) => formatMoney(d.cashEquivalents)) },
    { label: '应收账款', values: sorted.map((d) => formatMoney(d.accountsReceivable)) },
    { label: '存货', values: sorted.map((d) => formatMoney(d.inventories)) },
    { label: '流动资产', values: sorted.map((d) => formatMoney(d.currentAssets)) },
    { label: '非流动资产', values: sorted.map((d) => formatMoney(d.nonCurrentAssets)) },
    { label: '总资产', values: sorted.map((d) => formatMoney(d.totalAssets)), emphasis: true },
    { label: '短期借款', values: sorted.map((d) => formatMoney(d.shortTermBorrowings)) },
    { label: '应付账款', values: sorted.map((d) => formatMoney(d.accountsPayable)) },
    { label: '流动负债', values: sorted.map((d) => formatMoney(d.currentLiabilities)) },
    { label: '非流动负债', values: sorted.map((d) => formatMoney(d.nonCurrentLiabilities)) },
    { label: '总负债', values: sorted.map((d) => formatMoney(d.totalLiabilities)), emphasis: true },
    { label: '长期借款', values: sorted.map((d) => formatMoney(d.longTermBorrowings)) },
    { label: '商誉', values: sorted.map((d) => formatMoney(d.goodwill)) },
    { label: '归母权益', values: sorted.map((d) => formatMoney(d.equityParentCompany)), emphasis: true },
    { label: '总权益', values: sorted.map((d) => formatMoney(d.totalEquity)), emphasis: true },
    { label: '资产负债率', values: sorted.map((d) => formatPercent(d.debtRatio)), emphasis: true },
  ]
}

const groups = computed(() => {
  const map = new Map<string, FinancialBalance[]>()
  for (const item of props.data) {
    if (!map.has(item.reportType)) {
      map.set(item.reportType, [])
    }
    map.get(item.reportType)!.push(item)
  }
  return reportTypeOrder
    .filter((type) => map.has(type))
    .map((type) => ({
      type,
      label: reportTypeLabels[type] || type,
      rows: buildRows(map.get(type)!),
    }))
})
</script>

<template>
  <div class="statement-groups">
    <div v-for="group in groups" :key="group.type" class="statement-group">
      <h3 class="group-title">{{ group.label }}</h3>
      <FinancialStatementTable :rows="group.rows" unit="万元" />
    </div>
    <div v-if="groups.length === 0" class="empty-state">暂无数据</div>
  </div>
</template>

<style scoped>
.statement-groups {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  color: var(--text-muted);
  font-size: 13px;
  font-family: var(--font);
}
</style>
