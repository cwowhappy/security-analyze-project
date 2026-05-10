<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useCollectionStore } from '@/stores/modules/collection'

const collectionStore = useCollectionStore()

const page = ref(1)
const size = ref(20)
const status = ref('')
const taskType = ref('')

const fetchData = () => {
  collectionStore.fetchTasks(
    { page: page.value, size: size.value },
    status.value || undefined,
    taskType.value || undefined,
  )
}

const createTask = async (type: string) => {
  await collectionStore.createTask(type)
  fetchData()
}

const statusBadge = (s: string) => {
  const map: Record<string, string> = {
    pending: 'badge-warning',
    running: 'badge-info',
    success: 'badge-success',
    failed: 'badge-error',
  }
  return map[s] || 'badge-info'
}

const statusText = (s: string) => {
  const map: Record<string, string> = {
    pending: '待执行',
    running: '执行中',
    success: '成功',
    failed: '失败',
  }
  return map[s] || s
}

const typeText = (t: string) => {
  const map: Record<string, string> = {
    stock_full: '股票全量',
    company_full: '公司全量',
    stock_single: '股票单条',
    company_single: '公司单条',
  }
  return map[t] || t
}

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="card">
      <div class="card-title">采集任务</div>
      <div class="filter-bar">
        <select v-model="status" @change="page = 1; fetchData()">
          <option value="">全部状态</option>
          <option value="pending">待执行</option>
          <option value="running">执行中</option>
          <option value="success">成功</option>
          <option value="failed">失败</option>
        </select>
        <select v-model="taskType" @change="page = 1; fetchData()">
          <option value="">全部类型</option>
          <option value="stock_full">股票全量</option>
          <option value="company_full">公司全量</option>
          <option value="stock_single">股票单条</option>
          <option value="company_single">公司单条</option>
        </select>
        <button class="btn btn-primary btn-sm" @click="page = 1; fetchData()">筛选</button>
        <div class="filter-actions">
          <button class="btn btn-success btn-sm" @click="createTask('stock_full')">采集股票</button>
          <button class="btn btn-success btn-sm" @click="createTask('company_full')">采集公司</button>
        </div>
      </div>

      <div v-if="collectionStore.loading" class="loading-pulse">加载中...</div>

      <template v-else>
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>类型</th>
              <th>状态</th>
              <th>数据源</th>
              <th class="text-right">总数</th>
              <th class="text-right">成功</th>
              <th class="text-right">失败</th>
              <th>开始时间</th>
              <th>完成时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in collectionStore.tasks" :key="task.id">
              <td class="font-mono text-muted">{{ task.id.slice(0, 16) }}...</td>
              <td>{{ typeText(task.taskType) }}</td>
              <td>
                <span :class="['badge', statusBadge(task.status)]">{{ statusText(task.status) }}</span>
              </td>
              <td>{{ task.dataSource || '-' }}</td>
              <td class="text-right font-mono">{{ task.totalCount }}</td>
              <td class="text-right font-mono text-green">{{ task.successCount }}</td>
              <td class="text-right font-mono text-red">{{ task.failCount }}</td>
              <td class="font-mono">{{ task.startedAt ? new Date(task.startedAt).toLocaleString() : '-' }}</td>
              <td class="font-mono">{{ task.completedAt ? new Date(task.completedAt).toLocaleString() : '-' }}</td>
            </tr>
            <tr v-if="!collectionStore.tasks.length">
              <td colspan="9" class="empty-state">暂无数据</td>
            </tr>
          </tbody>
        </table>

        <div class="pagination" v-if="collectionStore.tasks.length">
          <button :disabled="page === 1" @click="page--; fetchData()">上一页</button>
          <span>第 {{ page }} 页</span>
          <button :disabled="collectionStore.tasks.length < size" @click="page++; fetchData()">下一页</button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.filter-actions {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.text-right {
  text-align: right;
}

.text-green { color: var(--green); }
.text-red { color: var(--red); }
</style>
