<script setup lang="ts">
import { computed } from 'vue'
import { ElTable, ElTableColumn } from 'element-plus'

const props = defineProps<{
  data?: Record<string, any>
}>()

const rows = computed(() => {
  if (!props.data) return []
  const coreFields = [
    { key: 'TOTAL_OPERATE_INCOME', label: '营业总收入' },
    { key: 'OPERATE_INCOME', label: '营业收入' },
    { key: 'OPERATE_COST', label: '营业成本' },
    { key: 'SALE_EXPENSE', label: '销售费用' },
    { key: 'MANAGE_EXPENSE', label: '管理费用' },
    { key: 'RESEARCH_EXPENSE', label: '研发费用' },
    { key: 'FINANCE_EXPENSE', label: '财务费用' },
    { key: 'OPERATE_PROFIT', label: '营业利润' },
    { key: 'TOTAL_PROFIT', label: '利润总额' },
    { key: 'NETPROFIT', label: '净利润' },
    { key: 'PARENT_NETPROFIT', label: '归母净利润' },
  ]
  return coreFields
    .map((f) => ({
      label: f.label,
      value: props.data![f.key],
    }))
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
    <ElTable v-if="rows.length > 0" :data="rows" stripe style="width: 100%">
      <ElTableColumn prop="label" label="项目" min-width="200" />
      <ElTableColumn prop="value" label="金额" align="right" width="200">
        <template #default="{ row }">
          {{ formatValue(row.value) }}
        </template>
      </ElTableColumn>
    </ElTable>
    <div v-else class="empty-tip">暂无利润表数据</div>
  </div>
</template>

<style scoped>
.empty-tip {
  color: #999;
  padding: 40px 0;
  text-align: center;
}
</style>
