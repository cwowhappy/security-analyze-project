<script setup lang="ts">
import { computed } from 'vue'
import { useFinancialFormatter } from '../composables/useFinancialFormatter'
import FinancialStatementTable from './FinancialStatementTable.vue'
import type { FinancialIncome } from '@/types/financial'
import type { TableRow } from './FinancialStatementTable.vue'

const props = defineProps<{
  data: FinancialIncome[]
}>()

const { formatMoney, formatPercent, formatReportDate } = useFinancialFormatter()

const reportTypeLabels: Record<string, string> = {
  Y: '年报',
  Q1: '一季报',
  Q2: '半年报',
  Q3: '三季报',
}

const reportTypeOrder = ['Y', 'Q1', 'Q2', 'Q3']

function buildRows(list: FinancialIncome[]): TableRow[] {
  const sorted = [...list].sort(
    (a, b) => new Date(b.reportDate).getTime() - new Date(a.reportDate).getTime(),
  )

  return [
    { label: '报告期', values: sorted.map((d) => formatReportDate(d.reportDate, d.reportType)) },
    { label: '营业总收入', values: sorted.map((d) => formatMoney(d.totalRevenue)), emphasis: true },
    { label: '营业成本', values: sorted.map((d) => formatMoney(d.operatingCost)) },
    { label: '销售费用', values: sorted.map((d) => formatMoney(d.sellingExpense)) },
    { label: '管理费用', values: sorted.map((d) => formatMoney(d.adminExpense)) },
    { label: '财务费用', values: sorted.map((d) => formatMoney(d.financialExpense)) },
    { label: '研发费用', values: sorted.map((d) => formatMoney(d.rdExpense)) },
    { label: '营业利润', values: sorted.map((d) => formatMoney(d.operatingProfit)), emphasis: true },
    { label: '毛利润', values: sorted.map((d) => formatMoney(d.grossProfit)) },
    { label: '毛利率', values: sorted.map((d) => formatPercent(d.grossMargin)) },
    { label: '利润总额', values: sorted.map((d) => formatMoney(d.totalProfit)) },
    { label: '净利润', values: sorted.map((d) => formatMoney(d.netProfit)), emphasis: true },
    { label: '净利率', values: sorted.map((d) => formatPercent(d.netMargin)) },
    { label: '归母净利润', values: sorted.map((d) => formatMoney(d.npParentCompany)), emphasis: true },
    { label: '基本每股收益', values: sorted.map((d) => (d.basicEps !== null ? `${d.basicEps.toFixed(2)} 元` : '-')), emphasis: true },
  ]
}

const groups = computed(() => {
  const map = new Map<string, FinancialIncome[]>()
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
