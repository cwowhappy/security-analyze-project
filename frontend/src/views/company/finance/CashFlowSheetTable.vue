<script setup lang="ts">
import { computed } from 'vue'
import { ElTable, ElTableColumn } from 'element-plus'

const props = defineProps<{
  data?: Record<string, any>
}>()

const rows = computed(() => {
  if (!props.data) return []
  const coreFields = [
    { key: 'NETCASH_OPERATE', label: '经营活动现金流净额' },
    { key: 'NETCASH_INVEST', label: '投资活动现金流净额' },
    { key: 'NETCASH_FINANCE', label: '筹资活动现金流净额' },
    { key: 'CCE_ADD', label: '现金及等价物净增加额' },
    { key: 'END_CCE', label: '期末现金及等价物余额' },
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
    <div v-else class="empty-tip">暂无现金流量表数据</div>
  </div>
</template>

<style scoped>
.empty-tip {
  color: #999;
  padding: 40px 0;
  text-align: center;
}
</style>
