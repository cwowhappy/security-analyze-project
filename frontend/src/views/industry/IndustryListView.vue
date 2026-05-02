<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElCard, ElMessage } from 'element-plus'
import { Grid } from '@element-plus/icons-vue'
import { getIndustryList } from '@/api/industry'
import type { IndustryListItem } from '@/types/industry'

const router = useRouter()
const industries = ref<IndustryListItem[]>([])
const loading = ref(false)

async function fetchIndustries() {
  loading.value = true
  try {
    const res = await getIndustryList()
    industries.value = res.data
  } catch (err) {
    ElMessage.error('加载行业列表失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

function goToDetail(industryName: string) {
  router.push(`/industries/${encodeURIComponent(industryName)}`)
}

onMounted(() => {
  fetchIndustries()
})
</script>

<template>
  <div class="industry-list" v-loading="loading">
    <h2>行业信息</h2>
    <p class="subtitle">共 {{ industries.length }} 个行业分类</p>

    <div class="card-grid">
      <ElCard
        v-for="item in industries"
        :key="item.industryName"
        class="industry-card"
        shadow="hover"
        @click="goToDetail(item.industryName)"
      >
        <div class="card-content">
          <Grid class="card-icon" />
          <div class="card-title">{{ item.industryName }}</div>
          <div class="card-count">{{ item.companyCount }} 家公司</div>
        </div>
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.industry-list {
  padding: 24px;
}
.subtitle {
  color: #666;
  margin-bottom: 20px;
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
