<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElTabs, ElTabPane, ElBreadcrumb, ElBreadcrumbItem, ElMessage, ElCard, ElLink, ElEmpty, ElTag } from 'element-plus'
import { getCompanyDetail } from '@/api/company'
import type { CompanyDetail } from '@/types/company'
import FinanceReportTab from './finance/FinanceReportTab.vue'

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
      <ElBreadcrumbItem :to="{ path: '/companies' }">公司信息</ElBreadcrumbItem>
      <ElBreadcrumbItem>{{ company?.stockName || stockCode }}</ElBreadcrumbItem>
    </ElBreadcrumb>

    <h2 class="page-title">
      {{ company?.stockName || stockCode }}
      <span v-if="company?.stockCode" class="subtitle">({{ company.stockCode }})</span>
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
            <div v-if="company.industries && company.industries.length > 0" class="industry-tags">
              <div v-for="ind in company.industries" :key="ind.standardCode + ind.level2Code" class="industry-tag-row">
                <ElTag size="small" :type="ind.standardCode === 'SW' ? 'success' : 'primary'" style="margin-right: 4px">
                  {{ ind.standardName }}
                </ElTag>
                <ElLink
                  type="primary"
                  @click="router.push({ path: `/industries/${encodeURIComponent(ind.level2Code || '')}`, query: { standard: ind.standardCode } })"
                >
                  {{ ind.level2Name || ind.level1Name || '-' }}
                </ElLink>
                <ElTag v-if="!ind.primary" size="small" type="info" style="margin-left: 4px">次</ElTag>
              </div>
            </div>
            <ElLink
              v-else-if="company.industry"
              type="primary"
              @click="router.push({ path: `/industries/${encodeURIComponent(company.industry)}`, query: { standard: 'EM' } })"
            >
              {{ company.industry }}
            </ElLink>
            <span v-else class="value">-</span>
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
        <div v-if="company?.securities && company.securities.length > 0" class="securities-grid">
          <ElCard
            v-for="sec in company.securities"
            :key="sec.stockCode"
            class="security-card"
            shadow="hover"
          >
            <template #header>
              <div class="security-header">
                <span class="security-name">{{ sec.stockName }}</span>
                <span class="security-code">({{ sec.stockCode }})</span>
              </div>
            </template>
            <div class="security-info">
              <div class="security-row">
                <span class="security-label">市场板块</span>
                <span class="security-value">{{ sec.market || '-' }}</span>
              </div>
              <div class="security-row">
                <span class="security-label">证券类型</span>
                <span class="security-value">{{ sec.securityType || '-' }}</span>
              </div>
              <div class="security-row">
                <span class="security-label">上市日期</span>
                <span class="security-value">{{ sec.listingDate || '-' }}</span>
              </div>
              <div class="security-row">
                <span class="security-label">上市状态</span>
                <span class="security-value">{{ sec.listingStatus || '-' }}</span>
              </div>
            </div>
          </ElCard>
        </div>
        <ElEmpty v-else description="暂无关联证券数据" />
      </ElTabPane>

      <ElTabPane label="财务报告" name="finance">
        <FinanceReportTab :stock-code="stockCode" />
      </ElTabPane>

      <ElTabPane label="历史变更" name="history">
        <ElEmpty description="历史变更记录开发中" />
      </ElTabPane>
    </ElTabs>
  </div>
</template>

<style scoped>
.company-detail {
  padding: 8px;
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 16px 0 0;
}
.subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: normal;
  margin-left: 8px;
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
  color: var(--text-secondary);
  width: 100px;
  flex-shrink: 0;
}
.info-item .value {
  font-weight: 500;
  color: var(--text-primary);
}
.securities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.security-card {
  cursor: default;
}
.security-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.security-name {
  font-weight: 600;
  font-size: 16px;
  color: var(--text-primary);
}
.security-code {
  color: var(--text-secondary);
  font-size: 14px;
}
.security-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.security-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.security-label {
  color: var(--text-secondary);
  font-size: 14px;
}
.security-value {
  font-weight: 500;
  font-size: 14px;
  color: var(--text-primary);
}
.industry-tags {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.industry-tag-row {
  display: flex;
  align-items: center;
}
</style>
