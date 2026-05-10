<script setup lang="ts">
import { computed } from 'vue'
import { ElTable, ElTableColumn, ElEmpty } from 'element-plus'

const props = defineProps<{
  data?: Record<string, any>
}>()

const rows = computed(() => {
  if (!props.data) return []
  const totalRevenue = Number(props.data!['TOTAL_OPERATE_INCOME']) || 0
  const coreFields = [
    { key: 'TOTAL_OPERATE_INCOME', label: '营业总收入', highlight: false },
    { key: 'OPERATE_INCOME', label: '营业收入', highlight: false },
    { key: 'OPERATE_COST', label: '营业成本', highlight: false },
    { key: 'SALE_EXPENSE', label: '销售费用', highlight: false },
    { key: 'MANAGE_EXPENSE', label: '管理费用', highlight: false },
    { key: 'RESEARCH_EXPENSE', label: '研发费用', highlight: false },
    { key: 'FINANCE_EXPENSE', label: '财务费用', highlight: false },
    { key: 'OPERATE_PROFIT', label: '营业利润', highlight: true },
    { key: 'TOTAL_PROFIT', label: '利润总额', highlight: true },
    { key: 'NETPROFIT', label: '净利润', highlight: true, accent: true },
    { key: 'PARENT_NETPROFIT', label: '归母净利润', highlight: true, accent: true },
  ]
  return coreFields
    .map((f) => {
      const val = props.data![f.key]
      let ratio: number | null = null
      if (val != null && !Number.isNaN(Number(val)) && totalRevenue > 0) {
        ratio = Number(((Number(val) / totalRevenue) * 100).toFixed(1))
      }
      return {
        label: f.label,
        value: val,
        highlight: f.highlight,
        accent: f.accent,
        ratio,
      }
    })
    .filter((r) => r.value !== undefined && r.value !== null && !Number.isNaN(r.value))
})

function formatValue(val: any): string {
  if (val === undefined || val === null || val === '') return '-'
  const num = Number(val)
  if (Number.isNaN(num)) return String(val)
  const abs = Math.abs(num)
  if (abs >= 1e8) return (num / 1e8).toFixed(2) + ' 亿'
  if (abs >= 1e4) return (num / 1e4).toFixed(2) + ' 万'
  return num.toLocaleString()
}
</script>

<template>
  <div class="sheet-table">
    <div class="sheet-header">利润表</div>
    <ElTable v-if="rows.length > 0" :data="rows" stripe class="finance-table">
      <ElTableColumn prop="label" label="项目" min-width="200">
        <template #default="{ row }">
          <span :class="{ 'highlight-row': row.highlight, 'accent-row': row.accent }">{{ row.label }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="value" label="金额" align="right" width="180">
        <template #default="{ row }">
          <span :class="{ 'highlight-row': row.highlight, 'accent-row': row.accent }">{{ formatValue(row.value) }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="占收入比" align="right" width="120">
        <template #default="{ row }">
          <span v-if="row.ratio != null" class="ratio-value">{{ row.ratio }}%</span>
          <span v-else class="ratio-empty">—</span>
        </template>
      </ElTableColumn>
    </ElTable>
    <ElEmpty v-else description="暂无利润表数据" />
  </div>
</template>

<style scoped>
.sheet-table {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
}
.sheet-header {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color);
}
.finance-table :deep(.highlight-row) {
  font-weight: 600;
}
.finance-table :deep(.accent-row) {
  font-weight: 700;
  color: var(--accent-primary);
}
.ratio-value {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-secondary);
}
.ratio-empty {
  color: var(--text-muted);
}
</style>
