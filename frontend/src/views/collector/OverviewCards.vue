<script setup lang="ts">
import { computed } from 'vue'
import { ElCard, ElTag, ElRow, ElCol } from 'element-plus'
import { DataAnalysis, Document, OfficeBuilding } from '@element-plus/icons-vue'
import type { CollectorOverviewItem } from '@/types/collector'

const props = defineProps<{
  items: CollectorOverviewItem[]
}>()

const emit = defineEmits<{
  (e: 'select', dataType: string): void
}>()

const statusType = (status?: string) => {
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

const iconMap: Record<string, any> = {
  company: OfficeBuilding,
  security: Document,
  finance_report: DataAnalysis,
}

const formatDuration = (seconds?: number) => {
  if (seconds == null) return '-'
  if (seconds < 60) return `${seconds}秒`
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return s > 0 ? `${m}分${s}秒` : `${m}分`
}

const formatTime = (time?: string) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}
</script>

<template>
  <ElRow :gutter="16">
    <ElCol :xs="24" :sm="12" :md="8" v-for="item in items" :key="item.dataType">
      <ElCard
        class="overview-card"
        shadow="hover"
        @click="emit('select', item.dataType)"
      >
        <div class="card-header">
          <component :is="iconMap[item.dataType] || DataAnalysis" class="card-icon" />
          <span class="card-title">{{ item.dataTypeLabel }}</span>
        </div>
        <div class="card-body">
          <div class="metric">
            <div class="metric-value">{{ item.totalRows?.toLocaleString() ?? '-' }}</div>
            <div class="metric-label">总条数</div>
          </div>
          <div class="meta">
            <div class="meta-row">
              <span class="meta-label">最近采集</span>
              <ElTag :type="statusType(item.lastTaskStatus)" size="small">
                {{ item.lastTaskStatus || '未知' }}
              </ElTag>
            </div>
            <div class="meta-row">
              <span class="meta-label">耗时</span>
              <span>{{ formatDuration(item.lastTaskDurationSeconds) }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">更新时间</span>
              <span>{{ formatTime(item.lastUpdatedAt) }}</span>
            </div>
          </div>
        </div>
      </ElCard>
    </ElCol>
  </ElRow>
</template>

<style scoped>
.overview-card {
  cursor: pointer;
  transition: transform 0.2s;
  margin-bottom: 16px;
}
.overview-card:hover {
  transform: translateY(-4px);
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.card-icon {
  width: 24px;
  height: 24px;
  color: #409eff;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.card-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.metric {
  text-align: center;
  min-width: 80px;
}
.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}
.metric-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}
.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;
}
.meta-label {
  color: #909399;
}
</style>
