<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElTabs, ElTabPane, ElBreadcrumb, ElBreadcrumbItem, ElMessage, ElCard } from 'element-plus'
import { getCompanyDetail } from '@/api/company'
import type { CompanyDetail } from '@/types/company'

const route = useRoute()
const router = useRouter()

const stockCode = route.params.stockCode as string
const loading = ref(false)
const company = ref<CompanyDetail | null>(null)
const activeTab = ref('basic')

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getCompanyDetail(stockCode)
    company.value = res
  } catch (err) {
    ElMessage.error('加载公司详情失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="company-detail" v-loading="loading">
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem :to="{ path: '/companies' }">公司列表</ElBreadcrumbItem>
      <ElBreadcrumbItem>{{ company?.stockName || stockCode }}</ElBreadcrumbItem>
    </ElBreadcrumb>

    <h2 style="margin-top: 16px">
      {{ company?.stockName || stockCode }}
      <span v-if="company?.stockCode" class="stock-code">({{ company.stockCode }})</span>
    </h2>

    <ElTabs v-model="activeTab" style="margin-top: 16px">
      <ElTabPane label="基本信息" name="basic">
        <div v-if="company" class="info-grid">
          <div class="info-item">
            <span class="label">股票代码</span>
            <span class="value">{{ company.stockCode }}</span>
          </div>
          <div class="info-item">
            <span class="label">公司名称</span>
            <span class="value">{{ company.stockName }}</span>
          </div>
          <div class="info-item">
            <span class="label">所属行业</span>
            <span class="value">{{ company.industry || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">地区</span>
            <span class="value">{{ company.region || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">成立日期</span>
            <span class="value">{{ company.establishDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">注册资本</span>
            <span class="value">
              {{ company.registeredCapital ? company.registeredCapital + ' 万元' : '-' }}
            </span>
          </div>
          <div class="info-item">
            <span class="label">上市日期</span>
            <span class="value">{{ company.listingDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">市场板块</span>
            <span class="value">{{ company.market || '-' }}</span>
          </div>
        </div>
      </ElTabPane>

      <ElTabPane label="关联证券" name="securities">
        <ElCard v-if="company">
          <template #header>
            <span>{{ company.stockName }} ({{ company.stockCode }})</span>
          </template>
          <div>市场板块：{{ company.market || '-' }}</div>
          <div style="margin-top: 8px">上市日期：{{ company.listingDate || '-' }}</div>
        </ElCard>
        <div v-else class="empty-tip">暂无数据</div>
      </ElTabPane>

      <ElTabPane label="财务报告" name="finance">
        <div class="empty-tip">财务报告模块开发中</div>
      </ElTabPane>

      <ElTabPane label="历史变更" name="history">
        <div class="empty-tip">历史变更记录开发中</div>
      </ElTabPane>
    </ElTabs>
  </div>
</template>

<style scoped>
.company-detail {
  padding: 24px;
}

.stock-code {
  font-size: 16px;
  color: #666;
  font-weight: normal;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  max-width: 600px;
}

.info-item {
  display: flex;
  align-items: baseline;
}

.info-item .label {
  color: #666;
  width: 100px;
  flex-shrink: 0;
}

.info-item .value {
  font-weight: 500;
}

.empty-tip {
  color: #999;
  padding: 40px 0;
  text-align: center;
}
</style>
