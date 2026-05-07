<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElCard,
  ElBreadcrumb,
  ElBreadcrumbItem,
  ElMessage,
  ElRadioGroup,
  ElRadioButton,
  ElTag,
  ElInput,
  ElButton,
  ElEmpty,
} from 'element-plus'
import { Grid, ArrowLeft, Search } from '@element-plus/icons-vue'
import { getIndustryList } from '@/api/industry'
import type { IndustryCategoryDto } from '@/types/industry'

const router = useRouter()
const standard = ref<'EM' | 'SW'>('EM')
const industries = ref<IndustryCategoryDto[]>([])
const loading = ref(false)
const filterKeyword = ref('')

// SW 层级状态
const selectedL1 = ref<string | null>(null)
const selectedL1Name = ref('')

async function fetchIndustries() {
  loading.value = true
  try {
    if (standard.value === 'EM') {
      const res = await getIndustryList('EM', 2)
      industries.value = res.data
    } else {
      // SW: 如果未选一级，显示一级；否则显示对应二级
      if (!selectedL1.value) {
        const res = await getIndustryList('SW', 1)
        industries.value = res.data
      } else {
        const res = await getIndustryList('SW', 2, selectedL1.value)
        industries.value = res.data
      }
    }
  } catch (err) {
    ElMessage.error('加载行业列表失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

const filteredIndustries = computed(() => {
  const kw = filterKeyword.value.trim()
  if (!kw) return industries.value
  const lower = kw.toLowerCase()
  return industries.value.filter(
    (item) =>
      item.name.toLowerCase().includes(lower) ||
      item.code.toLowerCase().includes(lower),
  )
})

function goToDetail(industryCode: string) {
  router.push({
    path: `/industries/${encodeURIComponent(industryCode)}`,
    query: { standard: standard.value },
  })
}

function selectL1(item: IndustryCategoryDto) {
  selectedL1.value = item.code
  selectedL1Name.value = item.name
  filterKeyword.value = ''
  fetchIndustries()
}

function backToL1() {
  selectedL1.value = null
  selectedL1Name.value = ''
  filterKeyword.value = ''
  fetchIndustries()
}

watch(standard, () => {
  selectedL1.value = null
  selectedL1Name.value = ''
  filterKeyword.value = ''
  fetchIndustries()
})

onMounted(() => {
  fetchIndustries()
})
</script>

<template>
  <div class="industry-list" v-loading="loading">
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem>行业信息</ElBreadcrumbItem>
    </ElBreadcrumb>

    <div class="page-header">
      <h2 class="page-title">
        行业信息
        <span class="subtitle">共 {{ industries.length }} 个行业分类</span>
      </h2>
      <ElRadioGroup v-model="standard" size="small">
        <ElRadioButton label="EM">东财板块</ElRadioButton>
        <ElRadioButton label="SW">申万行业</ElRadioButton>
      </ElRadioGroup>
    </div>

    <!-- SW 二级返回导航 -->
    <div v-if="standard === 'SW' && selectedL1" class="sw-nav">
      <ElButton link :icon="ArrowLeft" @click="backToL1">
        返回一级行业
      </ElButton>
      <ElTag size="small" type="info" effect="plain" style="margin-left: 8px">
        {{ selectedL1Name }}
      </ElTag>
    </div>

    <!-- 搜索过滤 -->
    <div class="filter-bar">
      <ElInput
        v-model="filterKeyword"
        placeholder="搜索行业名称或代码"
        clearable
        style="width: 320px"
      >
        <template #prefix>
          <Search style="width: 16px; height: 16px; color: var(--text-tertiary)" />
        </template>
      </ElInput>
    </div>

    <!-- 行业卡片网格 -->
    <div v-if="filteredIndustries.length > 0" class="card-grid">
      <ElCard
        v-for="item in filteredIndustries"
        :key="item.code"
        class="industry-card"
        shadow="hover"
        @click="standard === 'SW' && !selectedL1 ? selectL1(item) : goToDetail(item.code)"
      >
        <div class="card-content">
          <Grid class="card-icon" />
          <div class="card-title">{{ item.name }}</div>
          <div class="card-code">{{ item.code }}</div>
          <div class="card-count">{{ item.companyCount ?? 0 }} 家公司</div>
          <ElTag
            v-if="standard === 'SW' && !selectedL1"
            size="small"
            type="primary"
            effect="plain"
            style="margin-top: 8px"
          >
            查看二级
          </ElTag>
        </div>
      </ElCard>
    </div>

    <ElEmpty v-else description="未找到匹配的行业分类" />
  </div>
</template>

<style scoped>
.industry-list {
  padding: 8px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0 20px;
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
}
.subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: normal;
  margin-left: 8px;
}
.sw-nav {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
.filter-bar {
  margin-bottom: 20px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.industry-card {
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}
.industry-card:hover {
  transform: translateY(-4px);
  border-color: var(--accent-primary);
  box-shadow: var(--shadow-glow);
}
.card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 16px;
}
.card-icon {
  width: 40px;
  height: 40px;
  color: var(--accent-primary);
}
.card-title {
  margin-top: 12px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-primary);
}
.card-code {
  margin-top: 4px;
  font-size: 12px;
  color: var(--accent-primary);
  font-family: var(--font-mono);
}
.card-count {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
