<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStockStore } from '@/stores/modules/stock'

const props = defineProps<{
  stockCode: string
}>()

const stockStore = useStockStore()
const router = useRouter()

onMounted(() => {
  stockStore.fetchStockDetail(props.stockCode)
})

const goCompany = (uscCode: string) => {
  router.push(`/companies/${uscCode}`)
}
</script>

<template>
  <div>
    <div v-if="stockStore.loading" class="loading-pulse">加载中...</div>

    <template v-else-if="stockStore.currentStock">
      <!-- 基本信息 -->
      <div class="card">
        <div class="card-header">
          <div>
            <div class="stock-code font-mono">{{ stockStore.currentStock.stockCode }}</div>
            <div class="stock-name">{{ stockStore.currentStock.name }}</div>
          </div>
          <div class="stock-meta">
            <span class="market-tag" :class="`market-${stockStore.currentStock.market?.toLowerCase()}`">
              {{ stockStore.currentStock.market }}
            </span>
            <span class="exchange-tag">{{ stockStore.currentStock.exchange }}</span>
          </div>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">TS 代码</span>
            <span class="info-value font-mono">{{ stockStore.currentStock.tsCode || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">全称</span>
            <span class="info-value">{{ stockStore.currentStock.fullName || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">上市日期</span>
            <span class="info-value font-mono">{{ stockStore.currentStock.listDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">行业</span>
            <span class="info-value">{{ stockStore.currentStock.industry || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">地域</span>
            <span class="info-value">{{ stockStore.currentStock.area || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">总股本</span>
            <span class="info-value font-mono">{{ stockStore.currentStock.totalShares ? stockStore.currentStock.totalShares.toLocaleString() : '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">流通股本</span>
            <span class="info-value font-mono">{{ stockStore.currentStock.floatShares ? stockStore.currentStock.floatShares.toLocaleString() : '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 关联公司 -->
      <div v-if="stockStore.currentStock.company" class="card">
        <div class="card-title">关联公司</div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">公司名称</span>
            <span class="info-value">
              <a @click="goCompany(stockStore.currentStock.company!.unifiedSocialCreditCode)">
                {{ stockStore.currentStock.company.name }}
              </a>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">统一社会信用代码</span>
            <span class="info-value font-mono">{{ stockStore.currentStock.company.unifiedSocialCreditCode }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">法人代表</span>
            <span class="info-value">{{ stockStore.currentStock.company.legalRepresentative || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">注册资本</span>
            <span class="info-value font-mono">{{ stockStore.currentStock.company.regCapital || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">成立日期</span>
            <span class="info-value font-mono">{{ stockStore.currentStock.company.setupDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">主营业务</span>
            <span class="info-value">{{ stockStore.currentStock.company.mainBusiness || '-' }}</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="card empty-state">股票不存在</div>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.stock-code {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 1px;
  line-height: 1.2;
}

.stock-name {
  font-size: 15px;
  color: var(--text-secondary);
  margin-top: 4px;
  font-weight: 500;
}

.stock-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.market-tag,
.exchange-tag {
  display: inline-block;
  padding: 2px 10px;
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

.exchange-tag {
  background: var(--bg-header-row);
  color: var(--text-secondary);
  border: 1px solid var(--border-default);
}
</style>
