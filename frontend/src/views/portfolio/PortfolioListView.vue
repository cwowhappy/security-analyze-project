<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElBreadcrumb, ElBreadcrumbItem } from 'element-plus'
import { Plus, Delete, Edit } from '@element-plus/icons-vue'
import { getPortfolios, createPortfolio, updatePortfolio, deletePortfolio } from '@/api/portfolio'
import type { Portfolio, PortfolioRequest } from '@/types/portfolio'

const router = useRouter()
const portfolios = ref<Portfolio[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)

const form = ref<PortfolioRequest>({
  name: '',
  type: 'REAL',
  broker: '',
  description: '',
})

async function fetchData() {
  loading.value = true
  try {
    portfolios.value = await getPortfolios()
  } catch {
    ElMessage.error('加载组合列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  form.value = { name: '', type: 'REAL', broker: '', description: '' }
  dialogVisible.value = true
}

function openEdit(item: Portfolio) {
  isEdit.value = true
  editId.value = item.id
  form.value = {
    name: item.name,
    type: item.type,
    broker: item.broker || '',
    description: item.description || '',
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name) {
    ElMessage.warning('请输入组合名称')
    return
  }
  try {
    if (isEdit.value && editId.value) {
      await updatePortfolio(editId.value, form.value)
      ElMessage.success('修改成功')
    } else {
      await createPortfolio(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchData()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该组合吗？相关的成交记录和持仓也将被删除', '确认删除', { type: 'warning' })
    await deletePortfolio(id)
    ElMessage.success('删除成功')
    await fetchData()
  } catch {
    // cancel
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="portfolio-list">
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem>持仓管理</ElBreadcrumbItem>
    </ElBreadcrumb>

    <div class="page-header">
      <h2 class="page-title">持仓管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增组合</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="item in portfolios" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card class="portfolio-card" shadow="hover" @click="router.push(`/portfolios/${item.id}`)">
          <div class="card-header">
            <span class="name">{{ item.name }}</span>
            <el-tag size="small" :type="item.type === 'REAL' ? 'success' : 'info'">
              {{ item.type === 'REAL' ? '实盘' : '模拟盘' }}
            </el-tag>
          </div>
          <div class="card-meta">
            <div v-if="item.broker">券商：{{ item.broker }}</div>
            <div v-if="item.description" class="desc">{{ item.description }}</div>
          </div>
          <div class="card-actions" @click.stop>
            <el-button link :icon="Edit" size="small" @click="openEdit(item)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" size="small" @click="handleDelete(item.id)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && portfolios.length === 0" description="暂无组合，请创建" />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑组合' : '新增组合'" width="500px">
      <el-form label-width="80px">
        <el-form-item label="组合名称" required>
          <el-input v-model="form.name" placeholder="请输入组合名称" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.type">
            <el-radio label="REAL">实盘</el-radio>
            <el-radio label="SIMULATION">模拟盘</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="券商">
          <el-input v-model="form.broker" placeholder="可选" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.portfolio-list {
  padding: 8px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 16px 0;
}
.portfolio-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.portfolio-card:hover {
  border-color: var(--border-color-accent);
  box-shadow: var(--shadow-glow);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
.card-meta {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 12px;
}
.desc {
  margin-top: 4px;
  color: var(--text-tertiary);
}
.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}
</style>
