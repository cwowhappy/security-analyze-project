<script setup lang="ts">
import { computed } from 'vue'
import {
  ElTable,
  ElTableColumn,
  ElPagination,
  ElSelect,
  ElOption,
  ElTag,
  ElEmpty,
} from 'element-plus'
import type { CollectorTaskItem } from '@/types/collector'
import { DATA_TYPE_LABELS, STATUS_LABELS } from '@/types/collector'

const props = defineProps<{
  tasks: CollectorTaskItem[]
  total: number
  page: number
  size: number
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'update:page', page: number): void
  (e: 'update:size', size: number): void
  (e: 'filter', dataType?: string, status?: string): void
}>()

const dataTypeFilter = defineModel<string>('dataTypeFilter')
const statusFilter = defineModel<string>('statusFilter')

const statusType = (status: string) => {
  switch (status) {
    case 'success':
      return 'success'
    case 'failed':
      return 'danger'
    case 'running':
      return 'primary'
    default:
      return 'info'
  }
}

const formatTime = (time?: string) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

const formatDuration = (seconds?: number) => {
  if (seconds == null) return '-'
  if (seconds < 60) return `${seconds}秒`
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return s > 0 ? `${m}分${s}秒` : `${m}分`
}

const onPageChange = (page: number) => {
  emit('update:page', page - 1)
}

const onSizeChange = (size: number) => {
  emit('update:size', size)
}

const onFilterChange = () => {
  emit('filter', dataTypeFilter.value, statusFilter.value)
}
</script>

<template>
  <div class="task-list">
    <div class="filter-bar">
      <ElSelect
        v-model="dataTypeFilter"
        placeholder="数据类型"
        clearable
        style="width: 160px"
        @change="onFilterChange"
      >
        <ElOption
          v-for="(label, value) in DATA_TYPE_LABELS"
          :key="value"
          :label="label"
          :value="value"
        />
      </ElSelect>
      <ElSelect
        v-model="statusFilter"
        placeholder="状态"
        clearable
        style="width: 120px"
        @change="onFilterChange"
      >
        <ElOption
          v-for="(label, value) in STATUS_LABELS"
          :key="value"
          :label="label"
          :value="value"
        />
      </ElSelect>
    </div>

    <ElTable v-loading="loading" :data="tasks" style="width: 100%">
      <ElTableColumn prop="taskName" label="任务名称" min-width="180" />
      <ElTableColumn prop="taskType" label="数据类型" width="120">
        <template #default="{ row }">
          {{ DATA_TYPE_LABELS[row.taskType] || row.taskType }}
        </template>
      </ElTableColumn>
      <ElTableColumn prop="startedAt" label="开始时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.startedAt) }}
        </template>
      </ElTableColumn>
      <ElTableColumn prop="endedAt" label="结束时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.endedAt) }}
        </template>
      </ElTableColumn>
      <ElTableColumn prop="status" label="状态" width="90">
        <template #default="{ row }">
          <ElTag :type="statusType(row.status)" size="small">
            {{ STATUS_LABELS[row.status] || row.status }}
          </ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn prop="rowsAffected" label="影响行数" width="100">
        <template #default="{ row }">
          {{ row.rowsAffected?.toLocaleString() ?? '-' }}
        </template>
      </ElTableColumn>
      <ElTableColumn prop="durationSeconds" label="耗时" width="100">
        <template #default="{ row }">
          {{ formatDuration(row.durationSeconds) }}
        </template>
      </ElTableColumn>
    </ElTable>

    <ElEmpty v-if="!loading && tasks.length === 0" description="暂无任务记录" />

    <ElPagination
      v-if="total > 0"
      :current-page="page + 1"
      :page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @current-change="onPageChange"
      @size-change="onSizeChange"
      class="pagination"
    />
  </div>
</template>

<style scoped>
.task-list {
  margin-top: 16px;
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
