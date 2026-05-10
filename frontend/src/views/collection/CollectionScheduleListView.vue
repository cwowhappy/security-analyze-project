<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useCollectionStore } from '@/stores/modules/collection'
import type { CollectionTaskSchedule } from '@/types/collection'

const collectionStore = useCollectionStore()

const showModal = ref(false)
const editing = ref(false)
const editId = ref('')

const form = ref({
  name: '',
  taskType: 'stock_full' as string,
  cronExpression: '',
  dataSource: 'akshare',
  taskParams: '',
})

const resetForm = () => {
  form.value = {
    name: '',
    taskType: 'stock_full',
    cronExpression: '',
    dataSource: 'akshare',
    taskParams: '',
  }
  editing.value = false
  editId.value = ''
}

const openCreate = () => {
  resetForm()
  showModal.value = true
}

const openEdit = (schedule: CollectionTaskSchedule) => {
  form.value = {
    name: schedule.name,
    taskType: schedule.taskType,
    cronExpression: schedule.cronExpression,
    dataSource: schedule.dataSource || 'akshare',
    taskParams: schedule.taskParams ? JSON.stringify(schedule.taskParams) : '',
  }
  editing.value = true
  editId.value = schedule.id
  showModal.value = true
}

const save = async () => {
  const data = {
    name: form.value.name,
    taskType: form.value.taskType,
    cronExpression: form.value.cronExpression,
    dataSource: form.value.dataSource,
    taskParams: form.value.taskParams ? JSON.parse(form.value.taskParams) : undefined,
  }
  if (editing.value) {
    await collectionStore.updateSchedule(editId.value, data)
  } else {
    await collectionStore.createSchedule(data)
  }
  showModal.value = false
  collectionStore.fetchSchedules()
}

const remove = async (id: string) => {
  if (confirm('确定删除此定时规则？')) {
    await collectionStore.deleteSchedule(id)
    collectionStore.fetchSchedules()
  }
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

onMounted(() => {
  collectionStore.fetchSchedules()
})
</script>

<template>
  <div>
    <div class="card">
      <div class="card-header-row">
        <div class="card-title" style="margin: 0;">定时规则</div>
        <button class="btn btn-primary" @click="openCreate">新增规则</button>
      </div>

      <div v-if="collectionStore.loading" class="loading-pulse">加载中...</div>

      <table v-else class="table">
        <thead>
          <tr>
            <th>名称</th>
            <th>任务类型</th>
            <th>Cron 表达式</th>
            <th>数据源</th>
            <th>状态</th>
            <th>上次触发</th>
            <th style="width: 120px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="schedule in collectionStore.schedules" :key="schedule.id">
            <td class="font-medium">{{ schedule.name }}</td>
            <td>{{ typeText(schedule.taskType) }}</td>
            <td><code>{{ schedule.cronExpression }}</code></td>
            <td>{{ schedule.dataSource || '-' }}</td>
            <td>
              <span :class="['badge', schedule.isEnabled ? 'badge-success' : 'badge-warning']">
                {{ schedule.isEnabled ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="font-mono">{{ schedule.lastTriggeredAt ? new Date(schedule.lastTriggeredAt).toLocaleString() : '-' }}</td>
            <td>
              <button class="btn btn-primary btn-sm" @click="openEdit(schedule)">编辑</button>
              <button class="btn btn-danger btn-sm" style="margin-left: 6px;" @click="remove(schedule.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!collectionStore.schedules.length">
            <td colspan="7" class="empty-state">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <h3 class="modal-title">{{ editing ? '编辑规则' : '新增规则' }}</h3>
        <div class="form-group">
          <label>名称</label>
          <input v-model="form.name" placeholder="规则名称" />
        </div>
        <div class="form-group">
          <label>任务类型</label>
          <select v-model="form.taskType">
            <option value="stock_full">股票全量</option>
            <option value="company_full">公司全量</option>
            <option value="stock_single">股票单条</option>
            <option value="company_single">公司单条</option>
          </select>
        </div>
        <div class="form-group">
          <label>Cron 表达式</label>
          <input v-model="form.cronExpression" placeholder="0 2 * * *" />
        </div>
        <div class="form-group">
          <label>数据源</label>
          <select v-model="form.dataSource">
            <option value="akshare">akshare</option>
            <option value="tushare">tushare</option>
          </select>
        </div>
        <div class="form-group">
          <label>任务参数（JSON）</label>
          <input v-model="form.taskParams" placeholder='{"stockCode":"000001"}' />
        </div>
        <div class="modal-actions">
          <button class="btn btn-default" @click="showModal = false">取消</button>
          <button class="btn btn-primary" @click="save">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.font-medium {
  font-weight: 500;
  color: var(--text-primary);
}

/* ---- Modal ---- */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(5, 8, 16, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  backdrop-filter: blur(4px);
}

.modal {
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  padding: 24px;
  width: 480px;
  max-width: 90vw;
  box-shadow: var(--shadow-dropdown);
}

.modal-title {
  margin-bottom: 20px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--text-muted);
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  font-size: 13.5px;
  background: var(--bg-input);
  color: var(--text-primary);
  outline: none;
  transition: border-color var(--transition-fast);
}

.form-group input:focus,
.form-group select:focus {
  border-color: var(--border-focus);
}

.form-group input::placeholder {
  color: var(--text-muted);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
}
</style>
