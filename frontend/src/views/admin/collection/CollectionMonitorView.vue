<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminCollectionMonitorApi } from '@/api/modules/adminCollectionMonitor'
import { collectionTaskApi } from '@/api/modules/collection'
import type { CollectionMonitorOverview, CollectionMonitorBaseline } from '@/types/monitor'
import type { CollectionTask } from '@/types/collection'

const overview = ref<CollectionMonitorOverview[]>([])
const baseline = ref<CollectionMonitorBaseline | null>(null)
const tasks = ref<CollectionTask[]>([])
const taskTotal = ref(0)
const taskPage = ref(1)
const taskSize = ref(10)
const loading = ref(false)

const taskTypeLabelMap: Record<string, string> = {
  stock_basic: '股票基础信息',
  company_info: '公司信息',
  financial_income: '利润表',
  financial_balance: '资产负债表',
  financial_cashflow: '现金流量表',
  financial_indicator: '财务指标',
  financial_full: '财务全量',
}

function coveragePct(item: CollectionMonitorOverview) {
  if (!item.totalCount) return 0
  return Math.round((item.recentSuccessCount / item.totalCount) * 100)
}

function statusText(s: string) {
  const map: Record<string, string> = {
    success: '成功', running: '执行中', pending: '待执行', failed: '失败',
  }
  return map[s] || s
}

function statusClass(s: string) {
  const map: Record<string, string> = {
    success: 'tag-success',
    running: 'tag-running',
    pending: 'tag-pending',
    failed: 'tag-failed',
  }
  return map[s] || 'tag-pending'
}

function formatTime(t: string | null) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

async function fetchOverview() {
  try {
    overview.value = await adminCollectionMonitorApi.getOverview()
    baseline.value = await adminCollectionMonitorApi.getBaseline()
  } catch (e) {
    console.error('加载监控数据失败', e)
  }
}

async function fetchTasks() {
  loading.value = true
  try {
    const result = await collectionTaskApi.list({ page: taskPage.value, size: taskSize.value })
    tasks.value = result.list ?? []
    taskTotal.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

function prevPage() {
  if (taskPage.value > 1) {
    taskPage.value--
    fetchTasks()
  }
}

function nextPage() {
  if (tasks.value.length === taskSize.value) {
    taskPage.value++
    fetchTasks()
  }
}

onMounted(() => {
  fetchOverview()
  fetchTasks()
})
</script>

<template>
  <div>
    <div class="pg-hd">
      <h1 class="pg-t">采集监控</h1>
      <p class="pg-d">查看数据采集覆盖度、任务执行状态与历史记录</p>
    </div>

    <!-- 数据基线卡 -->
    <div class="stat-row">
      <div class="stat-card baseline">
        <div class="stat-n">{{ baseline?.totalStocks ?? '-' }}</div>
        <div class="stat-l">数据基线 · 系统股票总数</div>
      </div>
    </div>

    <!-- 采集覆盖度卡 -->
    <div class="overview-grid">
      <div v-for="item in overview" :key="item.taskType" class="overview-card">
        <div class="oc-title">{{ taskTypeLabelMap[item.taskType] || item.taskType }}</div>
        <div class="oc-metrics">
          <div class="oc-m">
            <div class="oc-v">{{ item.totalCount.toLocaleString() }}</div>
            <div class="oc-l">总量</div>
          </div>
          <div class="oc-m">
            <div class="oc-v" style="color:var(--success)">{{ item.recentSuccessCount.toLocaleString() }}</div>
            <div class="oc-l">成功未过期</div>
          </div>
          <div class="oc-m">
            <div class="oc-v" style="color:var(--warning)">{{ item.recentExpiredCount.toLocaleString() }}</div>
            <div class="oc-l">成功已过期</div>
          </div>
        </div>
        <div class="oc-bar">
          <div class="oc-fill" :style="{ width: coveragePct(item) + '%' }"></div>
        </div>
        <div class="oc-pct">覆盖率 {{ coveragePct(item) }}%</div>
      </div>
      <div v-if="!overview.length" class="empty">暂无采集记录</div>
    </div>

    <!-- 任务执行列表 -->
    <div class="section-title">任务执行列表</div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="task-table">
      <div class="task-row head">
        <span>任务类型</span>
        <span>模式</span>
        <span>数据源优先级</span>
        <span>状态</span>
        <span>总数</span>
        <span>成功</span>
        <span>失败</span>
        <span>开始时间</span>
      </div>
      <div v-for="task in tasks" :key="task.id" class="task-row">
        <span>{{ taskTypeLabelMap[task.taskType] || task.taskType }}</span>
        <span>
          <span :class="['mode-tag', task.mode === 'single' ? 'mode-single' : 'mode-full']">
            {{ task.mode === 'single' ? '单条' : '全量' }}
          </span>
        </span>
        <span :title="task.sourcePriority ?? '-'">{{ task.sourcePriority ? JSON.parse(task.sourcePriority).join(' > ') : '-' }}</span>
        <span><span :class="['tag', statusClass(task.status)]">{{ statusText(task.status) }}</span></span>
        <span>{{ task.totalCount }}</span>
        <span style="color:var(--success)">{{ task.successCount }}</span>
        <span style="color:var(--danger)">{{ task.failCount }}</span>
        <span class="time">{{ formatTime(task.startedAt) }}</span>
      </div>
      <div v-if="!tasks.length" class="empty">无任务记录</div>
    </div>

    <div class="pagination" v-if="tasks.length">
      <button :disabled="taskPage === 1" @click="prevPage">上一页</button>
      <span>第 {{ taskPage }} 页</span>
      <button :disabled="tasks.length < taskSize" @click="nextPage">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.pg-hd { margin-bottom: 24px; }
.pg-t { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.pg-d { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }

.stat-row { margin-bottom: 20px; }
.stat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  display: inline-block;
  min-width: 200px;
}
.stat-n { font-size: 28px; font-weight: 700; color: var(--primary); }
.stat-l { font-size: 12px; color: var(--text-muted); margin-top: 4px; }

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}
.overview-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 18px;
  box-shadow: var(--shadow-sm);
}
.oc-title { font-size: 14px; font-weight: 700; color: var(--text-primary); margin-bottom: 12px; }
.oc-metrics { display: flex; gap: 16px; margin-bottom: 12px; }
.oc-m { flex: 1; text-align: center; }
.oc-v { font-size: 20px; font-weight: 700; color: var(--text-primary); }
.oc-l { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.oc-bar { height: 6px; background: var(--border); border-radius: 3px; overflow: hidden; }
.oc-fill { height: 100%; background: var(--success); border-radius: 3px; transition: width 0.3s; }
.oc-pct { font-size: 11px; color: var(--text-muted); margin-top: 6px; text-align: right; }

.section-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin-bottom: 12px; }

.task-table { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; }
.task-row { display: grid; grid-template-columns: 1.5fr 0.8fr 1.5fr 0.8fr 0.6fr 0.6fr 0.6fr 1.2fr; gap: 8px; padding: 10px 14px; align-items: center; font-size: 13px; }
.task-row.head { background: var(--bg); font-weight: 700; color: var(--text-secondary); border-bottom: 1px solid var(--border); }
.task-row:not(.head) { border-bottom: 1px solid var(--border); }
.task-row:not(.head):hover { background: var(--surface-hover); }

.tag { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 700; }
.tag-success { background: rgba(0,217,36,0.1); color: var(--success); }
.tag-running { background: rgba(99,91,255,0.1); color: var(--primary); }
.tag-pending { background: var(--surface-hover); color: var(--text-muted); border: 1px solid var(--border); }
.tag-failed { background: rgba(255,59,48,0.1); color: var(--danger); }

.mode-tag { display: inline-flex; padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 700; }
.mode-full { background: var(--surface-hover); color: var(--text-secondary); border: 1px solid var(--border); }
.mode-single { background: rgba(99,91,255,0.1); color: var(--primary); }

.time { color: var(--text-muted); font-size: 12px; }
.empty { text-align: center; padding: 40px; color: var(--text-muted); }
.loading { text-align: center; padding: 40px; color: var(--text-muted); }
.pagination { display: flex; justify-content: center; align-items: center; gap: 6px; margin-top: 16px; }
.pagination button { padding: 6px 14px; border: 1px solid var(--border); background: var(--surface); color: var(--text-secondary); border-radius: var(--radius-md); cursor: pointer; font-size: 13px; }
.pagination button:hover:not(:disabled) { background: var(--surface-hover); }
.pagination button:disabled { opacity: 0.3; cursor: not-allowed; }
</style>
