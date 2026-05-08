<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElBreadcrumb, ElBreadcrumbItem, ElSelect, ElOption, ElButton, ElMessage } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import OverviewCards from './OverviewCards.vue'
import TaskList from './TaskList.vue'
import { getCollectorOverview, getCollectorTasks } from '@/api/collector'
import type { CollectorOverviewItem, CollectorTaskItem } from '@/types/collector'

const overviewItems = ref<CollectorOverviewItem[]>([])
const tasks = ref<CollectorTaskItem[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(20)
const loading = ref(false)
const overviewLoading = ref(false)

const dataTypeFilter = ref('')
const statusFilter = ref('')
const refreshInterval = ref(60)

let intervalId: ReturnType<typeof setInterval> | null = null
let failCount = 0

const loadOverview = async () => {
  overviewLoading.value = true
  try {
    const res = await getCollectorOverview()
    overviewItems.value = res.data
    failCount = 0
  } catch (e) {
    handleError(e)
  } finally {
    overviewLoading.value = false
  }
}

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await getCollectorTasks({
      dataType: dataTypeFilter.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size: size.value,
    })
    tasks.value = res.data
    total.value = res.total
    failCount = 0
  } catch (e) {
    handleError(e)
  } finally {
    loading.value = false
  }
}

const handleError = (e: any) => {
  failCount++
  if (failCount >= 3) {
    ElMessage.error('数据刷新异常，请检查网络')
    failCount = 0
  }
  console.error('Dashboard load error:', e)
}

const refresh = () => {
  loadOverview()
  loadTasks()
}

const setupInterval = () => {
  clearIntervalIfNeeded()
  const seconds = refreshInterval.value
  if (seconds > 0) {
    intervalId = setInterval(refresh, seconds * 1000)
  }
}

const clearIntervalIfNeeded = () => {
  if (intervalId) {
    clearInterval(intervalId)
    intervalId = null
  }
}

const onRefreshIntervalChange = () => {
  setupInterval()
}

const onCardSelect = (dataType: string) => {
  dataTypeFilter.value = dataType
  page.value = 0
}

watch([page, size, dataTypeFilter, statusFilter], () => {
  loadTasks()
}, { deep: true })

onMounted(() => {
  refresh()
  setupInterval()
})

onUnmounted(() => {
  clearIntervalIfNeeded()
})
</script>

<template>
  <div class="dashboard">
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem>采集监控</ElBreadcrumbItem>
    </ElBreadcrumb>

    <div class="header">
      <h2 class="page-title">数据采集监控</h2>
      <div class="controls">
        <ElSelect
          v-model="refreshInterval"
          style="width: 140px"
          @change="onRefreshIntervalChange"
        >
          <ElOption :value="30" label="30秒刷新" />
          <ElOption :value="60" label="1分钟刷新" />
          <ElOption :value="300" label="5分钟刷新" />
          <ElOption :value="0" label="手动刷新" />
        </ElSelect>
        <ElButton
          v-if="refreshInterval === 0"
          :icon="RefreshRight"
          circle
          size="small"
          @click="refresh"
        />
      </div>
    </div>

    <div v-loading="overviewLoading" class="section">
      <h3 class="section-title">采集概览</h3>
      <OverviewCards :items="overviewItems" @select="onCardSelect" />
    </div>

    <div class="section">
      <h3 class="section-title">最近7天任务记录</h3>
      <TaskList
        v-model:data-type-filter="dataTypeFilter"
        v-model:status-filter="statusFilter"
        :tasks="tasks"
        :total="total"
        :page="page"
        :size="size"
        :loading="loading"
        @update:page="page = $event"
        @update:size="size = $event"
      />
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 8px;
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0 24px;
}
.controls {
  display: flex;
  align-items: center;
  gap: 8px;
}
.section {
  margin-bottom: 32px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}
</style>
