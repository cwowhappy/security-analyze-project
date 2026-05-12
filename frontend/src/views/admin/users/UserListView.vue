<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/modules/admin'
import UserTable from '@/components/admin/UserTable.vue'

const router = useRouter()
const adminStore = useAdminStore()

const filters = reactive({
  keyword: '',
  role: '',
  emailVerified: '',
  locked: '',
})

const page = ref(1)
const size = ref(20)

const roleOptions = [
  { value: '', label: '全部角色' },
  { value: 'admin', label: '管理员' },
  { value: 'portfolio_manager', label: '投资经理' },
  { value: 'analyst', label: '分析师' },
  { value: 'viewer', label: '观察者' },
]

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'true', label: '已验证' },
  { value: 'false', label: '未验证' },
]

const lockOptions = [
  { value: '', label: '全部锁定' },
  { value: 'true', label: '已锁定' },
  { value: 'false', label: '未锁定' },
]

async function loadUsers() {
  await adminStore.fetchUsers({
    page: page.value,
    size: size.value,
    keyword: filters.keyword || undefined,
    role: filters.role || undefined,
    emailVerified: filters.emailVerified ? filters.emailVerified === 'true' : undefined,
    locked: filters.locked ? filters.locked === 'true' : undefined,
  })
}

function handleSearch() {
  page.value = 1
  loadUsers()
}

function handlePageChange(newPage: number) {
  page.value = newPage
  loadUsers()
}

function handleView(userId: string) {
  router.push(`/admin/users/${userId}`)
}

loadUsers()
</script>

<template>
  <div class="user-list-view">
    <h1 class="page-title">用户管理</h1>

    <div class="filter-bar">
      <div class="filter-group">
        <input
          v-model="filters.keyword"
          type="text"
          class="filter-input"
          placeholder="搜索用户名/邮箱/昵称..."
          @keyup.enter="handleSearch"
        />
        <select v-model="filters.role" class="filter-select" @change="handleSearch">
          <option v-for="opt in roleOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <select v-model="filters.emailVerified" class="filter-select" @change="handleSearch">
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <select v-model="filters.locked" class="filter-select" @change="handleSearch">
          <option v-for="opt in lockOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>
    </div>

    <UserTable :users="adminStore.users" :loading="adminStore.loading" @view="handleView" />

    <div v-if="adminStore.total > 0" class="pagination">
      <span class="page-info">
        显示 {{ (page - 1) * size + 1 }}-{{ Math.min(page * size, adminStore.total) }} / 共 {{ adminStore.total }} 条
      </span>
      <div class="page-buttons">
        <button class="page-btn" :disabled="page <= 1" @click="handlePageChange(page - 1)">←</button>
        <button
          v-for="p in Math.min(5, Math.ceil(adminStore.total / size))"
          :key="p"
          class="page-btn"
          :class="{ active: p === page }"
          @click="handlePageChange(p)"
        >
          {{ p }}
        </button>
        <button class="page-btn" :disabled="page >= Math.ceil(adminStore.total / size)" @click="handlePageChange(page + 1)">→</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-list-view {
  padding: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
  color: var(--text-primary);
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-input {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text-primary);
  font-size: 13px;
  width: 220px;
  outline: none;
}

.filter-input:focus {
  border-color: var(--primary);
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  cursor: pointer;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.page-info {
  font-size: 13px;
  color: var(--text-muted);
}

.page-buttons {
  display: flex;
  gap: 6px;
}

.page-btn {
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-btn.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}
</style>
