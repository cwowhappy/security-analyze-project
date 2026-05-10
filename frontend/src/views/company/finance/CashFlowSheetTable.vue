<script setup lang="ts">
import { computed } from 'vue'
import { ElTable, ElTableColumn, ElEmpty } from 'element-plus'

const props = defineProps<{
  data?: Record<string, any>
}>()

const rows = computed(() => {
  if (!props.data) return []
  const operateCash = Math.abs(Number(props.data!['NETCASH_OPERATE']) || 0)
  const coreFields = [
    { key: 'NETCASH_OPERATE', label: '经营活动现金流净额', highlight: true, accent: true },
    { key: 'NETCASH_INVEST', label: '投资活动现金流净额', highlight: false },
    { key: 'NETCASH_FINANCE', label: '筹资活动现金流净额', highlight: false },
    { key: 'CCE_ADD', label: '现金及等价物净增加额', highlight: true },
    { key: 'END_CCE', label: '期末现金及等价物余额', highlight: true, accent: true },
  ]
  return coreFields
    .map((f) => {
      const val = props.data![f.key]
      let ratio: number | null = null
      if (val != null && !Number.isNaN(Number(val)) && operateCash > 0) {
        ratio = Number(((Math.abs(Number(val)) / operateCash) * 100).toFixed(1))
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
    <div class="sheet-header">现金流量表</div>
    <ElTable v-if="rows.length > 0" :data="rows" stripe class="finance-table">
      <ElTableColumn prop="label" label="项目" min-width="240">
        <template #default="{ row }">
          <span :class="{ 'highlight-row': row.highlight, 'accent-row': row.accent }">{{ row.label }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="value" label="金额" align="right" width="180">
        <template #default="{ row }">
          <span :class="{ 'highlight-row': row.highlight, 'accent-row': row.accent }">{{ formatValue(row.value) }}</span>
        </template>
      </ElTableColumn>
      <ElTableColumn label="相对经营现金流" align="right" width="140">
        <template #default="{ row }">
          <span v-if="row.ratio != null" class="ratio-value">{{ row.ratio }}%</span>
          <span v-else class="ratio-empty">—</span>
        </template>
      </ElTableColumn>
    </ElTable>
    <ElEmpty v-else description="暂无现金流量表数据" />
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
