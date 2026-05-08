<script setup lang="ts">
import type { ImportResult } from '@/api/portfolio'

const props = defineProps<{
  modelValue: boolean
  result: ImportResult | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
}>()
</script>

<template>
  <el-dialog :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" title="导入结果" width="600px">
    <div v-if="result">
      <p>总行数: <strong>{{ result.total }}</strong></p>
      <p>成功: <strong style="color: var(--down-color);">{{ result.success }}</strong></p>
      <p>失败: <strong style="color: var(--up-color);">{{ result.errors.length }}</strong></p>

      <el-table v-if="result.errors.length > 0" :data="result.errors" size="small" stripe height="300">
        <el-table-column prop="line" label="行号" width="80" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column prop="message" label="错误信息" show-overflow-tooltip />
      </el-table>
    </div>
  </el-dialog>
</template>
