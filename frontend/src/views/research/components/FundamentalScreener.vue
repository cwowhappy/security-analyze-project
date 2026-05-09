<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { ElInput, ElSelect, ElOption, ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { screenCompanies } from '@/api/research'
import type { ScreenCompanyItem } from '@/types/research'

const emit = defineEmits<{
  select: [stockCode: string]
  addCompare: [stockCode: string]
}>()

const keyword = ref('')
const industry = ref('')
const market = ref('')
const loading = ref(false)
const companies = ref<ScreenCompanyItem[]>([])
const selectedStock = ref('')

const marketOptions = [
  { value: '', label: '全部市场' },
  { value: 'SH', label: '上海' },
  { value: 'SZ', label: '深圳' },
  { value: 'BJ', label: '北京' },
]

const industryOptions = ref<{ value: string; label: string }[]>([
  { value: '', label: '全部行业' },
])

async function fetchCompanies() {
  loading.value = true
  try {
    const res = await screenCompanies({
      keyword: keyword.value || undefined,
      industry: industry.value || undefined,
      market: market.value || undefined,
      page: 0,
      size: 50,
    })
    companies.value = res.items
  } catch (err) {
    ElMessage.error('加载公司列表失败')
  } finally {
    loading.value = false
  }
}

function selectCompany(stockCode: string) {
  selectedStock.value = stockCode
  emit('select', stockCode)
}

// 防抖搜索
let debounceTimer: ReturnType<typeof setTimeout>
watch([keyword, industry, market], () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(fetchCompanies, 300)
}, { immediate: true })

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
})
</script>

<template>
  <div class="screener">
    <div class="screener-title">股票筛选</div>

    <!-- 搜索框 -->
    <ElInput
      v-model="keyword"
      placeholder="输入代码或名称"
      :prefix-icon="Search"
      clearable
      size="small"
      class="screener-search"
    />

    <!-- 筛选条件 -->
    <div class="screener-filters">
      <ElSelect v-model="industry" placeholder="行业" size="small" clearable>
        <ElOption
          v-for="opt in industryOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </ElSelect>
      <ElSelect v-model="market" placeholder="市场" size="small" clearable>
        <ElOption
          v-for="opt in marketOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </ElSelect>
    </div>

    <!-- 公司列表 -->
    <div v-loading="loading" class="company-list">
      <div
        v-for="item in companies"
        :key="item.stockCode"
        class="company-item"
        :class="{ active: selectedStock === item.stockCode }"
      >
        <div class="company-header" @click="selectCompany(item.stockCode)">
          <span class="company-name">{{ item.stockName }}</span>
          <span class="company-code">{{ item.stockCode }}</span>
        </div>
        <div class="company-meta" @click="selectCompany(item.stockCode)">
          <span class="meta-tag">{{ item.industry || '-' }}</span>
          <span class="meta-tag">{{ item.market || '-' }}</span>
        </div>
        <div class="company-actions">
          <button class="action-btn" @click.stop="emit('addCompare', item.stockCode)">
            + 对比
          </button>
        </div>
      </div>
      <div v-if="!loading && companies.length === 0" class="empty-tip">
        未找到匹配公司
      </div>
    </div>
  </div>
</template>

<style scoped>
.screener {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.screener-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #e5e7eb);
}
.screener-search {
  width: 100%;
}
.screener-filters {
  display: flex;
  gap: 8px;
}
.screener-filters :deep(.el-select) {
  flex: 1;
}
.company-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 200px;
}
.company-item {
  padding: 12px;
  border-radius: 8px;
  background: var(--card-bg, rgba(255,255,255,0.03));
  cursor: pointer;
  transition: background 0.2s;
}
.company-item:hover {
  background: var(--card-hover-bg, rgba(255,255,255,0.06));
}
.company-item.active {
  background: rgba(64, 158, 255, 0.15);
  border: 1px solid rgba(64, 158, 255, 0.3);
}
.company-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.company-name {
  font-weight: 500;
  color: var(--text-primary, #e5e7eb);
  font-size: 14px;
}
.company-code {
  font-size: 12px;
  color: var(--text-secondary, #9ca3af);
}
.company-meta {
  display: flex;
  gap: 6px;
}
.meta-tag {
  font-size: 12px;
  color: var(--text-secondary, #9ca3af);
  background: rgba(255,255,255,0.05);
  padding: 2px 6px;
  border-radius: 4px;
}
.empty-tip {
  color: var(--text-secondary, #9ca3af);
  text-align: center;
  padding: 24px 0;
  font-size: 13px;
}
.company-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}
.action-btn {
  background: transparent;
  border: 1px solid rgba(64, 158, 255, 0.4);
  color: #409eff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.action-btn:hover {
  background: rgba(64, 158, 255, 0.1);
  border-color: #409eff;
}
</style>
