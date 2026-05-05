<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElCard, ElBreadcrumb, ElBreadcrumbItem, ElMessage, ElRadioGroup, ElRadioButton, ElTag } from 'element-plus'
import { Grid } from '@element-plus/icons-vue'
import { getIndustryList } from '@/api/industry'
import type { IndustryCategoryDto } from '@/types/industry'

const router = useRouter()
const standard = ref<'EM' | 'SW'>('EM')
const industries = ref<IndustryCategoryDto[]>([])
const loading = ref(false)

// SW 层级状态
const selectedL1 = ref<string | null>(null)

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

function goToDetail(industryCode: string) {
  router.push({
    path: `/industries/${encodeURIComponent(industryCode)}`,
    query: { standard: standard.value },
  })
}

function selectL1(code: string) {
  selectedL1.value = code
  fetchIndustries()
}

function backToL1() {
  selectedL1.value = null
  fetchIndustries()
}

watch(standard, () => {
  selectedL1.value = null
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

    <!-- SW 面包屑导航 -->
    <div v-if="standard === 'SW' && selectedL1" class="sw-breadcrumb">
      <ElTag type="info" style="cursor: pointer" @click="backToL1">
        ← 返回一级行业
      </ElTag>
    </div>

    <div class="card-grid">
      <ElCard
        v-for="item in industries"
        :key="item.code"
        class="industry-card"
        shadow="hover"
        @click="standard === 'SW' && !selectedL1 ? selectL1(item.code) : goToDetail(item.code)"
      >
        <div class="card-content">
          <Grid class="card-icon" />
          <div class="card-title">{{ item.name }}</div>
          <div class="card-count">{{ item.companyCount ?? 0 }} 家公司</div>
          <ElTag v-if="standard === 'SW' && !selectedL1" size="small" type="info" style="margin-top: 8px">
            查看二级
          </ElTag>
        </div>
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.industry-list {
  padding: 24px;
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
  color: #303133;
  margin: 0;
}
.subtitle {
  font-size: 14px;
  color: #909399;
  font-weight: normal;
  margin-left: 8px;
}
.sw-breadcrumb {
  margin-bottom: 16px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.industry-card {
  cursor: pointer;
  transition: transform 0.2s;
}
.industry-card:hover {
  transform: translateY(-4px);
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
  color: #409eff;
}
.card-title {
  margin-top: 12px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
}
.card-count {
  margin-top: 8px;
  font-size: 13px;
  color: #666;
}
</style>
