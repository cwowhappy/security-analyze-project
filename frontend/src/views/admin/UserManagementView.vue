<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, ElBreadcrumb, ElBreadcrumbItem } from 'element-plus'
import { getUserList, approveUser, disableUser, enableUser } from '@/api/adminUser'
import type { UserListItem } from '@/api/adminUser'

const users = ref<UserListItem[]>([])
const loading = ref(false)

const pendingCount = computed(() => users.value.filter(u => u.status === 'PENDING').length)
const totalCount = computed(() => users.value.length)

async function fetchUsers() {
  loading.value = true
  try {
    users.value = await getUserList()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

async function handleApprove(id: number) {
  try {
    await approveUser(id)
    ElMessage.success('审批通过')
    await fetchUsers()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

async function handleDisable(id: number) {
  try {
    await ElMessageBox.confirm('确定要禁用该用户吗？', '提示', { type: 'warning' })
    await disableUser(id)
    ElMessage.success('已禁用')
    await fetchUsers()
  } catch {
    // 取消操作
  }
}

async function handleEnable(id: number) {
  try {
    await enableUser(id)
    ElMessage.success('已启用')
    await fetchUsers()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

function statusTagType(status: string): any {
  switch (status) {
    case 'APPROVED': return 'success'
    case 'PENDING': return 'warning'
    case 'DISABLED': return 'danger'
    default: return 'info'
  }
}

function statusLabel(status: string): string {
  switch (status) {
    case 'APPROVED': return '已通过'
    case 'PENDING': return '待审批'
    case 'DISABLED': return '已禁用'
    default: return status
  }
}

onMounted(fetchUsers)
</script>

<template>
  <div class="user-management">
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem>用户管理</ElBreadcrumbItem>
    </ElBreadcrumb>

    <h2 class="page-title">用户管理</h2>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="12">
        <el-card>
          <div class="stat-label">总用户数</div>
          <div class="stat-value">{{ totalCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div class="stat-label">待审批</div>
          <div class="stat-value">{{ pendingCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table :data="users" v-loading="loading" stripe style="margin-top: 24px;">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="真实姓名" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.role === 'ADMIN'" type="danger">管理员</el-tag>
          <el-tag v-else>普通用户</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            type="success"
            size="small"
            @click="handleApprove(row.id)"
          >
            通过审批
          </el-button>
          <el-button
            v-else-if="row.status === 'APPROVED'"
            type="danger"
            size="small"
            @click="handleDisable(row.id)"
          >
            禁用
          </el-button>
          <el-button
            v-else-if="row.status === 'DISABLED'"
            type="primary"
            size="small"
            @click="handleEnable(row.id)"
          >
            启用
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.user-management {
  padding: 8px;
  max-width: 1200px;
  margin: 0 auto;
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 16px 0;
}
.stats-row {
  margin-top: 16px;
}
.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--accent-primary);
  font-family: var(--font-mono);
}
</style>
