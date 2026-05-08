<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Upload } from '@element-plus/icons-vue'
import { getTransactions, createTransaction, updateTransaction, deleteTransaction, importTransactions } from '@/api/portfolio'
import TransactionFormDialog from './TransactionFormDialog.vue'
import ImportResultDialog from './ImportResultDialog.vue'
import type { Transaction, TransactionRequest, TradeType } from '@/types/portfolio'
import type { ImportResult } from '@/api/portfolio'

const props = defineProps<{
  portfolioId: number
}>()

const transactions = ref<Transaction[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(20)
const dialogVisible = ref(false)
const editData = ref<(TransactionRequest & { id?: number }) | undefined>(undefined)
const importResult = ref<ImportResult | null>(null)
const importResultVisible = ref(false)

// 筛选条件
const filterStockCode = ref('')
const filterTradeType = ref<TradeType | ''>('')
const filterDateRange = ref<string[]>([])

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value - 1, size: size.value }
    if (filterStockCode.value) params.stockCode = filterStockCode.value
    if (filterTradeType.value) params.tradeType = filterTradeType.value
    if (filterDateRange.value && filterDateRange.value.length === 2) {
      params.startDate = filterDateRange.value[0]
      params.endDate = filterDateRange.value[1]
    }
    const res = await getTransactions(props.portfolioId, params)
    transactions.value = res.items
    total.value = res.total
  } catch {
    ElMessage.error('加载成交记录失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function openCreate() {
  editData.value = undefined
  dialogVisible.value = true
}

function openEdit(row: Transaction) {
  editData.value = {
    id: row.id,
    stockCode: row.stockCode,
    tradeDate: row.tradeDate,
    tradeType: row.tradeType,
    price: row.price,
    quantity: row.quantity,
    fee: row.fee,
    tax: row.tax,
    remark: row.remark,
  }
  dialogVisible.value = true
}

async function handleSubmit(data: TransactionRequest & { id?: number }) {
  try {
    if (data.id) {
      await updateTransaction(data.id, data)
      ElMessage.success('修改成功')
    } else {
      await createTransaction(props.portfolioId, data)
      ElMessage.success('录入成功')
    }
    dialogVisible.value = false
    await fetchData()
    emit('changed')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(row: Transaction) {
  try {
    await ElMessageBox.confirm('确定删除该成交记录吗？', '确认删除', { type: 'warning' })
    await deleteTransaction(row.id)
    ElMessage.success('删除成功')
    await fetchData()
    emit('changed')
  } catch {
    // cancel
  }
}

function handlePageChange(p: number) {
  page.value = p
  fetchData()
}

async function handleFileUpload(file: File) {
  try {
    const result = await importTransactions(props.portfolioId, file)
    importResult.value = result
    importResultVisible.value = true
    if (result.success > 0) {
      await fetchData()
      emit('changed')
    }
  } catch {
    ElMessage.error('导入失败')
  }
}

onMounted(fetchData)

const emit = defineEmits<{
  (e: 'changed'): void
}>()

defineExpose({ refresh: fetchData })
</script>

<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" size="small" @click="openCreate">录入成交</el-button>
      <el-upload
        action=""
        :auto-upload="false"
        :show-file-list="false"
        :on-change="(uploadFile: any) => handleFileUpload(uploadFile.raw)"
        style="display: inline-block; margin-left: 8px;"
      >
        <el-button :icon="Upload" size="small">批量导入 CSV</el-button>
      </el-upload>

      <div class="filter-bar">
        <el-input v-model="filterStockCode" placeholder="股票代码" size="small" style="width: 120px" clearable />
        <el-select v-model="filterTradeType" placeholder="交易类型" size="small" style="width: 120px" clearable>
          <el-option label="买入" value="BUY" />
          <el-option label="卖出" value="SELL" />
          <el-option label="现金分红" value="DIVIDEND" />
          <el-option label="送股" value="BONUS" />
          <el-option label="配股" value="RIGHTS" />
        </el-select>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          size="small"
          style="width: 240px"
        />
        <el-button :icon="Search" size="small" @click="handleSearch">筛选</el-button>
      </div>
    </div>

    <el-table :data="transactions" v-loading="loading" stripe>
      <el-table-column prop="tradeDate" label="交易日期" width="120" />
      <el-table-column prop="stockCode" label="股票代码" width="100" />
      <el-table-column prop="tradeTypeLabel" label="类型" width="90" />
      <el-table-column prop="price" label="成交价格" width="120" align="right">
        <template #default="{ row }"> {{ row.price?.toFixed(4) }} </template>
      </el-table-column>
      <el-table-column prop="quantity" label="成交股数" width="120" align="right">
        <template #default="{ row }"> {{ row.quantity?.toFixed(2) }} </template>
      </el-table-column>
      <el-table-column prop="amount" label="成交金额" width="120" align="right">
        <template #default="{ row }"> {{ row.amount?.toFixed(2) }} </template>
      </el-table-column>
      <el-table-column prop="fee" label="费用" width="100" align="right">
        <template #default="{ row }"> {{ row.fee?.toFixed(4) }} </template>
      </el-table-column>
      <el-table-column prop="tax" label="税费" width="100" align="right">
        <template #default="{ row }"> {{ row.tax?.toFixed(4) }} </template>
      </el-table-column>
      <el-table-column prop="realizedPnl" label="已实现盈亏" width="120" align="right">
        <template #default="{ row }">
          <span :class="row.realizedPnl >= 0 ? 'up' : 'down'">
            {{ row.realizedPnl?.toFixed(2) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link :icon="Edit" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :icon="Delete" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="prev, pager, next"
      class="pagination"
      @current-change="handlePageChange"
    />

    <TransactionFormDialog
      v-model="dialogVisible"
      :portfolio-id="portfolioId"
      :edit-data="editData"
      @submit="handleSubmit"
    />
    <ImportResultDialog v-model="importResultVisible" :result="importResult" />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 8px;
}
.filter-bar {
  margin-left: auto;
  display: flex;
  gap: 8px;
  align-items: center;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.up { color: var(--up-color); }
.down { color: var(--down-color); }
</style>
