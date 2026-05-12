<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { collectionTaskApi } from '@/api/modules/collection'
import type { CollectionTask } from '@/types/collection'

const page = ref(1)
const size = ref(20)
const keyword = ref('')
const status = ref('')
const taskType = ref('')

const tasks = ref<CollectionTask[]>([])
const total = ref(0)
const loading = ref(false)

const statusOptions = [
  { label: '待执行', value: 'pending' },
  { label: '执行中', value: 'running' },
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' },
]

const taskTypeOptions = [
  { label: '股票全量', value: 'stock_full' },
  { label: '公司全量', value: 'company_full' },
  { label: '股票单条', value: 'stock_single' },
  { label: '公司单条', value: 'company_single' },
  { label: '字段补充', value: 'field_supplement' },
]

const stats = computed(() => {
  const all = tasks.value
  return {
    total: total.value,
    success: all.filter(t => t.status === 'success').length,
    running: all.filter(t => t.status === 'running').length,
    failed: all.filter(t => t.status === 'failed').length,
  }
})

async function fetchData() {
  loading.value = true
  try {
    const result = await collectionTaskApi.list(
      { page: page.value, size: size.value },
      status.value || undefined,
      taskType.value || undefined,
    )
    tasks.value = result.list ?? []
    total.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

function prevPage() {
  if (page.value > 1) {
    page.value--
    fetchData()
  }
}

function nextPage() {
  if (tasks.value.length === size.value) {
    page.value++
    fetchData()
  }
}

function resetFilters() {
  page.value = 1
  status.value = ''
  taskType.value = ''
  fetchData()
}

function fmt(n: number) {
  return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function statusClass(s: string) {
  const map: Record<string, string> = {
    success: 'ss',
    running: 'sr',
    pending: 'sp',
    failed: 'sf',
  }
  return map[s] || 'sp'
}

function statusDotClass(s: string) {
  const map: Record<string, string> = {
    success: 'sd-s',
    running: 'sd-r',
    pending: 'sd-p',
    failed: 'sd-f',
  }
  return map[s] || 'sd-p'
}

function statusText(s: string) {
  const map: Record<string, string> = {
    success: '成功',
    running: '执行中',
    pending: '待执行',
    failed: '失败',
  }
  return map[s] || s
}

function typeText(t: string) {
  const map: Record<string, string> = {
    stock_full: '股票全量',
    company_full: '公司全量',
    stock_single: '股票单条',
    company_single: '公司单条',
    field_supplement: '字段补充',
  }
  return map[t] || t
}

function progressPct(task: CollectionTask) {
  if (!task.totalCount) return 0
  return Math.round((task.successCount / task.totalCount) * 100)
}

function formatTime(t: string | null) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

async function createTask() {
  try {
    await collectionTaskApi.create({ taskType: 'stock_full' })
    fetchData()
  } catch (e) {
    alert('创建任务失败')
  }
}

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="pg-hd">
      <h1 class="pg-t">采集任务</h1>
      <p class="pg-d">管理数据采集任务，查看执行状态与历史记录</p>
    </div>

    <div class="bar">
      <div style="display:flex;gap:12px;width:100%;justify-content:space-between;align-items:center;flex-wrap:wrap">
        <div class="sb" style="flex:1;min-width:200px">
          <input v-model="keyword" placeholder="搜索任务名称或类型..." @keyup.enter="page = 1; fetchData()" />
        </div>
        <div class="fg">
          <label>状态</label>
          <select v-model="status" @change="page = 1; fetchData()">
            <option value="">全部</option>
            <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
        </div>
        <div class="fg">
          <label>任务类型</label>
          <select v-model="taskType" @change="page = 1; fetchData()">
            <option value="">全部</option>
            <option v-for="t in taskTypeOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
        </div>
        <button class="btn btn-s" @click="resetFilters">重置</button>
        <button class="btn btn-p" @click="createTask">+ 触发采集任务</button>
      </div>
    </div>

    <!-- Stats -->
    <div class="stat-row">
      <div class="stat-card" style="text-align:center;padding:18px">
        <div class="stat-n">{{ stats.total }}</div>
        <div class="stat-l">总任务数</div>
      </div>
      <div class="stat-card" style="text-align:center;padding:18px">
        <div class="stat-n" style="color:var(--success)">{{ stats.success }}</div>
        <div class="stat-l">执行成功</div>
      </div>
      <div class="stat-card" style="text-align:center;padding:18px">
        <div class="stat-n" style="color:var(--primary)">{{ stats.running }}</div>
        <div class="stat-l">执行中</div>
      </div>
      <div class="stat-card" style="text-align:center;padding:18px">
        <div class="stat-n" style="color:var(--danger)">{{ stats.failed }}</div>
        <div class="stat-l">执行失败</div>
      </div>
    </div>

    <!-- Task Cards -->
    <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-muted)">加载中...</div>
    <div v-else class="tlist">
      <div v-for="task in tasks" :key="task.id" class="tcard">
        <div class="tinf">
          <div class="tnm">{{ typeText(task.taskType) }}</div>
          <div class="tmeta">
            <span>{{ task.dataSource || '-' }}</span>
            <span>{{ formatTime(task.startedAt) }}</span>
            <span>{{ fmt(task.totalCount) }}条</span>
            <span style="color:var(--success)">{{ fmt(task.successCount) }}成功</span>
            <span v-if="task.failCount > 0" style="color:var(--danger)">{{ fmt(task.failCount) }}失败</span>
          </div>
          <div class="pb">
            <div class="pf" :style="{ width: progressPct(task) + '%' }"></div>
          </div>
        </div>
        <div style="display:flex;align-items:center;gap:10px;flex-shrink:0">
          <span class="tst" :class="statusClass(task.status)">
            <span class="sd" :class="statusDotClass(task.status)"></span>
            {{ statusText(task.status) }}
          </span>
        </div>
      </div>
      <div v-if="!tasks.length" style="text-align:center;padding:40px;color:var(--text-muted)">无匹配任务</div>
    </div>

    <div class="pagination" v-if="tasks.length">
      <button :disabled="page === 1" @click="prevPage">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) || 1 }} 页</span>
      <button :disabled="tasks.length < size" @click="nextPage">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.pg-hd {
  margin-bottom: 24px;
}

.pg-t {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.pg-d {
  font-size: 13px;
  color: var(--text-secondary);
}

.bar {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 14px 18px;
  margin-bottom: 22px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  box-shadow: var(--shadow-sm);
}

.sb {
  position: relative;
}

.sb input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-family: var(--font);
  color: var(--text-primary);
  background: var(--bg);
  transition: border-color 0.15s, background 0.25s;
}

.sb input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(99, 91, 255, 0.1);
}

.sb::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 14px;
  height: 14px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='%236B7A8D' viewBox='0 0 24 24'%3E%3Cpath d='M21 21l-4.35-4.35A7.5 7.5 0 1 0 16.65 4.35L21 8.65 21 21zM10 4a6 6 0 1 1-6 6 6 6 0 0 1 6-6z'/%3E%3C/svg%3E") center/contain no-repeat;
  opacity: 0.6;
}

.btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--primary);
  color: #fff;
}

.btn:hover {
  background: var(--primary-hover);
}

.btn-p {
  background: var(--primary);
  color: #fff;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 22px;
}

.stat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  transition: all 0.2s;
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.stat-n {
  font-size: 26px;
  font-weight: 700;
  font-family: var(--mono);
  color: var(--text-primary);
}

.stat-l {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.tlist {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tcard {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
}

.tcard:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.tinf {
  flex: 1;
  min-width: 0;
}

.tnm {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-primary);
}

.tmeta {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.tst {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.ss {
  background: rgba(0, 217, 36, 0.1);
  color: var(--success);
}

[data-theme="dark"] .ss {
  background: rgba(0, 217, 36, 0.15);
}

.sr {
  background: rgba(99, 91, 255, 0.1);
  color: var(--primary);
}

[data-theme="dark"] .sr {
  background: rgba(124, 115, 255, 0.15);
}

.sp {
  background: var(--surface-hover);
  color: var(--text-muted);
  border: 1px solid var(--border);
}

.sf {
  background: rgba(255, 59, 48, 0.1);
  color: var(--danger);
}

[data-theme="dark"] .sf {
  background: rgba(255, 59, 48, 0.15);
}

.sd {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.sd-s {
  background: var(--success);
}

.sd-r {
  background: var(--primary);
  animation: pu 1.5s infinite;
}

.sd-p {
  background: var(--text-muted);
}

.sd-f {
  background: var(--danger);
}

@keyframes pu {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

.pb {
  width: 100%;
  height: 5px;
  background: var(--border);
  border-radius: 3px;
  overflow: hidden;
  margin-top: 8px;
}

.pf {
  height: 100%;
  background: var(--primary);
  border-radius: 3px;
  transition: width 0.3s;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
}

.pagination button {
  padding: 6px 14px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s;
}

.pagination button:hover:not(:disabled) {
  border-color: var(--border);
  color: var(--text-primary);
  background: var(--surface-hover);
}

.pagination button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.pagination span {
  color: var(--text-muted);
  font-size: 13px;
  padding: 0 8px;
}

@media (max-width: 900px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
