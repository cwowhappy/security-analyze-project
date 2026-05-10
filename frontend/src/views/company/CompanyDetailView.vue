<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCompanyStore } from '@/stores/modules/company'

const props = defineProps<{
  uscCode: string
}>()

const companyStore = useCompanyStore()
const router = useRouter()

onMounted(() => {
  companyStore.fetchCompanyDetail(props.uscCode)
})

const goStock = (stockCode: string) => {
  router.push(`/stocks/${stockCode}`)
}
</script>

<template>
  <div>
    <div v-if="companyStore.loading" class="loading-pulse">加载中...</div>

    <template v-else-if="companyStore.currentCompany">
      <!-- 公司概况 -->
      <div class="card">
        <div class="card-header">
          <div>
            <div class="company-name">{{ companyStore.currentCompany.name }}</div>
            <div class="company-code font-mono">{{ companyStore.currentCompany.unifiedSocialCreditCode }}</div>
          </div>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">简称</span>
            <span class="info-value">{{ companyStore.currentCompany.shortName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">英文名称</span>
            <span class="info-value">{{ companyStore.currentCompany.englishName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">法人代表</span>
            <span class="info-value">{{ companyStore.currentCompany.legalRepresentative || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">董事长</span>
            <span class="info-value">{{ companyStore.currentCompany.chairman || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">总经理</span>
            <span class="info-value">{{ companyStore.currentCompany.manager || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">董秘</span>
            <span class="info-value">{{ companyStore.currentCompany.secretary || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">注册资本</span>
            <span class="info-value font-mono">{{ companyStore.currentCompany.regCapital || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">成立日期</span>
            <span class="info-value font-mono">{{ companyStore.currentCompany.setupDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">省份</span>
            <span class="info-value">{{ companyStore.currentCompany.province || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">城市</span>
            <span class="info-value">{{ companyStore.currentCompany.city || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">注册地址</span>
            <span class="info-value">{{ companyStore.currentCompany.regAddress || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">办公地址</span>
            <span class="info-value">{{ companyStore.currentCompany.officeAddress || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">官网</span>
            <span class="info-value">{{ companyStore.currentCompany.website || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">行业</span>
            <span class="info-value">{{ companyStore.currentCompany.industry || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">员工人数</span>
            <span class="info-value font-mono">{{ companyStore.currentCompany.employees || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">实控人</span>
            <span class="info-value">{{ companyStore.currentCompany.controllerName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">实控人类型</span>
            <span class="info-value">{{ companyStore.currentCompany.controllerType || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 主营业务 -->
      <div class="card">
        <div class="card-title">主营业务</div>
        <p class="text-block">{{ companyStore.currentCompany.mainBusiness || '暂无数据' }}</p>
      </div>

      <!-- 经营范围 -->
      <div class="card">
        <div class="card-title">经营范围</div>
        <p class="text-block">{{ companyStore.currentCompany.businessScope || '暂无数据' }}</p>
      </div>

      <!-- 公司简介 -->
      <div class="card">
        <div class="card-title">公司简介</div>
        <p class="text-block">{{ companyStore.currentCompany.introduction || '暂无数据' }}</p>
      </div>

      <!-- 关联股票 -->
      <div v-if="companyStore.currentCompany.stocks?.length" class="card">
        <div class="card-title">关联股票</div>
        <table class="table">
          <thead>
            <tr>
              <th>股票代码</th>
              <th>名称</th>
              <th>市场</th>
              <th>交易所</th>
              <th>上市日期</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="stock in companyStore.currentCompany.stocks"
              :key="stock.stockCode"
              @click="goStock(stock.stockCode)"
              class="cursor-row"
            >
              <td class="font-mono text-primary">
                <a @click.stop="goStock(stock.stockCode)">{{ stock.stockCode }}</a>
              </td>
              <td>{{ stock.name }}</td>
              <td>
                <span class="market-tag" :class="`market-${stock.market?.toLowerCase()}`">
                  {{ stock.market || '-' }}
                </span>
              </td>
              <td>{{ stock.exchange || '-' }}</td>
              <td class="font-mono">{{ stock.listDate || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <div v-else class="card empty-state">公司不存在</div>
  </div>
</template>

<style scoped>
.card-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.company-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.3px;
}

.company-code {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.text-block {
  color: var(--text-secondary);
  line-height: 1.75;
  font-size: 13.5px;
}

.cursor-row {
  cursor: pointer;
}

.market-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 100px;
  font-size: 11px;
  font-weight: 600;
  font-family: var(--font-mono);
}

.market-sh {
  background: rgba(231, 76, 60, 0.15);
  color: #e74c3c;
}

.market-sz {
  background: rgba(45, 140, 240, 0.15);
  color: #2d8cf0;
}

.market-bj {
  background: rgba(39, 174, 96, 0.15);
  color: #27ae60;
}
</style>
