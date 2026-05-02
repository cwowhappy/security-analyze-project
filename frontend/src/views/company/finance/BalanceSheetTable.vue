<script setup lang="ts">
import { computed } from 'vue'
import { ElTable, ElTableColumn } from 'element-plus'

const props = defineProps<{
  data?: Record<string, any>
}>()

const rows = computed(() => {
  if (!props.data) return []
  const coreFields = [
    { key: 'MONETARYFUNDS', label: '货币资金' },
    { key: 'ACCOUNTS_RECE', label: '应收账款' },
    { key: 'INVENTORY', label: '存货' },
    { key: 'CURRENT_ASSET_BALANCE', label: '流动资产合计' },
    { key: 'NONCURRENT_ASSET_BALANCE', label: '非流动资产合计' },
    { key: 'ASSET_BALANCE', label: '资产总计' },
    { key: 'CURRENT_LIAB_BALANCE', label: '流动负债合计' },
    { key: 'NONCURRENT_LIAB_BALANCE', label: '非流动负债合计' },
    { key: 'LIAB_BALANCE', label: '负债合计' },
    { key: 'EQUITY_BALANCE', label: '所有者权益合计' },
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
    <div v-else class="empty-tip">暂无资产负债表数据</div>
  </div>
</template>

<style scoped>
.empty-tip {
  color: #999;
  padding: 40px 0;
  text-align: center;
}
</style>
