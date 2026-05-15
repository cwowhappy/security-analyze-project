<script setup lang="ts">
import { computed } from 'vue'
import { useFinancialFormatter } from '../composables/useFinancialFormatter'
import FinancialStatementTable from './FinancialStatementTable.vue'
import type { FinancialCashflow } from '@/types/financial'
import type { TableRow } from './FinancialStatementTable.vue'

const props = defineProps<{
  data: FinancialCashflow[]
}>()

const { formatMoney, formatPercent, formatReportDate } = useFinancialFormatter()

const reportTypeLabels: Record<string, string> = {
  Y: '年报',
  Q1: '一季报',
  Q2: '半年报',
  Q3: '三季报',
}

const reportTypeOrder = ['Y', 'Q1', 'Q2', 'Q3']

function buildRows(list: FinancialCashflow[]): TableRow[] {
  const sorted = [...list].sort(
    (a, b) => new Date(b.reportDate).getTime() - new Date(a.reportDate).getTime(),
  )

  return [
    { label: '报告期', values: sorted.map((d) => formatReportDate(d.reportDate, d.reportType)) },
    { label: '经营活动现金流', values: sorted.map((d) => formatMoney(d.cfOperating)), emphasis: true },
    { label: '投资活动现金流', values: sorted.map((d) => formatMoney(d.cfInvesting)) },
    { label: '筹资活动现金流', values: sorted.map((d) => formatMoney(d.cfFinancing)) },
    { label: '现金净增加额', values: sorted.map((d) => formatMoney(d.netCashFlow)), emphasis: true },
    { label: '自由现金流', values: sorted.map((d) => formatMoney(d.freeCashFlow)), emphasis: true },
    { label: '购建固定资产', values: sorted.map((d) => formatMoney(d.capex)) },
    { label: '经营活动现金流入', values: sorted.map((d) => formatMoney(d.cashReceivedOperating)) },
    { label: '所得税支付', values: sorted.map((d) => formatMoney(d.taxPaid)) },
    { label: '经营现金流/净利润', values: sorted.map((d) => formatPercent(d.cfoToNetProfit)) },
  ]
}

const groups = computed(() => {
  const map = new Map<string, FinancialCashflow[]>()
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
