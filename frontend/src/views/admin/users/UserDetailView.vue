<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/modules/admin'
import UserStatusBadge from '@/components/admin/UserStatusBadge.vue'
import UserRoleBadge from '@/components/admin/UserRoleBadge.vue'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const userId = ref(route.params.userId as string)
const editing = ref(false)
const editForm = ref({ displayName: '', role: '' })

watch(() => route.params.userId, (newId) => {
  if (newId) {
    userId.value = newId as string
    loadUser()
  }
})

async function loadUser() {
  await adminStore.fetchUserDetail(userId.value)
  if (adminStore.currentUser) {
    editForm.value.displayName = adminStore.currentUser.displayName
    editForm.value.role = adminStore.currentUser.role
  }
}

function startEdit() {
  editing.value = true
}

async function saveEdit() {
  await adminStore.updateUser(userId.value, {
    displayName: editForm.value.displayName,
    role: editForm.value.role,
  })
  editing.value = false
  await loadUser()
}

async function handleUnlock() {
  if (!confirm('确定要解锁该账户吗？')) return
  await adminStore.unlockUser(userId.value)
  await loadUser()
}

async function handleForcePasswordReset() {
  if (!confirm('确定要强制该用户下次登录时修改密码吗？')) return
  await adminStore.forcePasswordReset(userId.value, '管理员强制修改密码')
}

loadUser()
</script>

<template>
  <div class="user-detail-view">
    <div class="breadcrumb">
      <button class="back-link" @click="router.push('/admin/users')">← 返回用户列表</button>
    </div>

    <div v-if="adminStore.currentUser" class="detail-card">
      <div class="card-header">
        <h2>👤 用户详情</h2>
        <div class="header-actions">
          <button v-if="!editing" class="btn-primary" @click="startEdit">编辑</button>
          <template v-else>
            <button class="btn-secondary" @click="editing = false">取消</button>
            <button class="btn-primary" @click="saveEdit">保存</button>
          </template>
        </div>
      </div>

      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">用户ID</span>
          <span class="info-value">{{ adminStore.currentUser.id }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">用户名</span>
          <span class="info-value">{{ adminStore.currentUser.username }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">邮箱</span>
          <span class="info-value">{{ adminStore.currentUser.email }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">昵称</span>
          <span v-if="!editing" class="info-value">{{ adminStore.currentUser.displayName }}</span>
          <input v-else v-model="editForm.displayName" class="edit-input" />
        </div>
        <div class="info-item">
          <span class="info-label">角色</span>
          <span v-if="!editing" class="info-value"><UserRoleBadge :role="adminStore.currentUser.role" /></span>
          <select v-else v-model="editForm.role" class="edit-select">
            <option value="portfolio_manager">投资经理</option>
            <option value="analyst">分析师</option>
            <option value="viewer">观察者</option>
            <option value="admin">管理员</option>
          </select>
        </div>
        <div class="info-item">
          <span class="info-label">注册时间</span>
          <span class="info-value">{{ new Date(adminStore.currentUser.createdAt).toLocaleString() }}</span>
        </div>
      </div>

      <div class="section">
        <h3>账户状态</h3>
        <div class="status-grid">
          <div class="status-item">
            <span class="status-label">邮箱验证</span>
            <UserStatusBadge :email-verified="adminStore.currentUser.emailVerified" :locked="false" />
          </div>
          <div class="status-item">
            <span class="status-label">账户状态</span>
            <span :class="['status-text', adminStore.currentUser.locked ? 'danger' : 'success']">
              {{ adminStore.currentUser.locked ? '🔒 已锁定' : '🔓 正常' }}
            </span>
          </div>
          <div class="status-item">
            <span class="status-label">登录失败</span>
            <span class="status-text">{{ adminStore.currentUser.failedLoginAttempts }} 次</span>
          </div>
        </div>
      </div>

      <div class="section">
        <h3>操作</h3>
        <div class="action-buttons">
          <button v-if="adminStore.currentUser.locked" class="btn-warning" @click="handleUnlock">
            🔓 解锁账户
          </button>
          <button class="btn-danger" @click="handleForcePasswordReset">
            ⚠️ 强制修改密码
          </button>
        </div>
      </div>
    </div>

    <div v-else-if="adminStore.loading" class="loading">加载中...</div>
    <div v-else class="empty">用户不存在</div>
  </div>
</template>

<style scoped>
.user-detail-view {
  padding: 24px;
}

.breadcrumb {
  margin-bottom: 20px;
}

.back-link {
  background: none;
  border: none;
  color: var(--primary);
  font-size: 14px;
  cursor: pointer;
  padding: 0;
}

.back-link:hover {
  text-decoration: underline;
}

.detail-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.card-header h2 {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn-primary {
  padding: 6px 14px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-secondary {
  padding: 6px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: var(--text-primary);
}

.edit-input,
.edit-select {
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  max-width: 200px;
}

.edit-input:focus,
.edit-select:focus {
  border-color: var(--primary);
}

.section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
}

.section h3 {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-label {
  font-size: 12px;
  color: var(--text-muted);
}

.status-text {
  font-size: 14px;
  font-weight: 600;
}

.status-text.success {
  color: var(--success);
}

.status-text.danger {
  color: var(--danger);
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-warning {
  padding: 8px 16px;
  border: none;
  border-radius: var(--radius-md);
  background: #ff9500;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-danger {
  padding: 8px 16px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--danger);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.loading,
.empty {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
}
</style>
