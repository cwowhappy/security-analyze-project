<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { TransactionRequest, TradeType } from '@/types/portfolio'

const props = defineProps<{
  modelValue: boolean
  portfolioId: number
  editData?: TransactionRequest & { id?: number }
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'submit', data: TransactionRequest & { id?: number }): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const form = ref<TransactionRequest>({
  stockCode: '',
  tradeDate: '',
  tradeType: 'BUY',
  price: undefined,
  quantity: 0,
  fee: 0,
  tax: 0,
  remark: '',
})

const isEdit = computed(() => !!props.editData?.id)

watch(() => props.editData, (val) => {
  if (val) {
    form.value = { ...val }
  } else {
    form.value = {
      stockCode: '',
      tradeDate: new Date().toISOString().slice(0, 10),
      tradeType: 'BUY',
      price: undefined,
      quantity: 0,
      fee: 0,
      tax: 0,
      remark: '',
    }
  }
}, { immediate: true })

const showPrice = computed(() => {
  const type = form.value.tradeType
  return type === 'BUY' || type === 'SELL' || type === 'RIGHTS'
})

const showQuantity = computed(() => {
  const type = form.value.tradeType
  return type !== 'DIVIDEND'
})

function handleSubmit() {
  if (!form.value.stockCode) {
    ElMessage.warning('请输入股票代码')
    return
  }
  if (!form.value.tradeDate) {
    ElMessage.warning('请选择交易日期')
    return
  }
  if (showPrice.value && (form.value.price === undefined || form.value.price === null)) {
    ElMessage.warning('请输入成交价格')
    return
  }
  if (showQuantity.value && form.value.quantity <= 0) {
    ElMessage.warning('成交股数必须大于0')
    return
  }
  emit('submit', { ...form.value, id: props.editData?.id })
}
</script>

<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑成交' : '录入成交'" width="520px">
    <el-form label-width="90px">
      <el-form-item label="股票代码" required>
        <el-input v-model="form.stockCode" placeholder="如 600519" />
      </el-form-item>
      <el-form-item label="交易日期" required>
        <el-date-picker v-model="form.tradeDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="交易类型" required>
        <el-select v-model="form.tradeType" style="width: 100%">
          <el-option label="买入" value="BUY" />
          <el-option label="卖出" value="SELL" />
          <el-option label="现金分红" value="DIVIDEND" />
          <el-option label="送股" value="BONUS" />
          <el-option label="配股" value="RIGHTS" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="showPrice" label="成交价格" required>
        <el-input-number v-model="form.price" :precision="4" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item v-if="showQuantity" label="成交股数" required>
        <el-input-number v-model="form.quantity" :precision="2" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item v-else label="分红金额">
        <el-input-number v-model="form.tax" :precision="4" :min="0" style="width: 100%" placeholder="税费/分红金额" />
      </el-form-item>
      <el-form-item v-if="showPrice" label="交易费用">
        <el-input-number v-model="form.fee" :precision="4" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item v-if="showPrice" label="税费">
        <el-input-number v-model="form.tax" :precision="4" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>
