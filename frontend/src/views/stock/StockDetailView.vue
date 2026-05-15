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

function goBack() {
  router.push('/stocks')
}

function goCompany() {
  const stock = stockStore.currentStock
  if (stock?.company?.id) {
    router.push(`/companies/${stock.company.id}`)
  }
}

function goFinancial() {
  router.push(`/stocks/${props.stockCode}/financial`)
}

function fmt(n: number | null) {
  if (!n) return '-'
  return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function yi(n: number | null) {
  if (!n) return '-'
  return (n / 1e8).toFixed(2)
}

function exBadgeClass(ex: string | null) {
  const m = ex?.toLowerCase()
  if (m === 'sse' || m === 'sh') return 'b-sh'
  if (m === 'szse' || m === 'sz') return 'b-sz'
  if (m === 'bse' || m === 'bj') return 'b-bj'
  return ''
}
</script>

<template>
  <div v-if="stockStore.loading" style="text-align:center;padding:40px;color:var(--text-muted)">加载中...</div>
  <div v-else-if="stockStore.currentStock">
    <button class="bk" @click="goBack">← 返回股票列表</button>

    <div class="dc">
      <div class="dh">
        <div class="dht">
          <div class="dt">
            <span class="sc">{{ stockStore.currentStock.stockCode }}</span>
            <span>{{ stockStore.currentStock.name }}</span>
            <span class="b" :class="exBadgeClass(stockStore.currentStock.exchange)">
              {{ stockStore.currentStock.exchange || '-' }}
            </span>
            <span class="bm">{{ stockStore.currentStock.market || '-' }}</span>
          </div>
          <div class="dh-actions">
            <button class="dl" @click="goFinancial">
              📊 财务分析
            </button>
            <button class="dl" @click="goCompany" v-if="stockStore.currentStock.company">
              🏠 关联公司
            </button>
          </div>
        </div>
        <div class="ds">{{ stockStore.currentStock.fullName || stockStore.currentStock.name }}</div>
      </div>

      <div class="db">
        <div class="dsec">
          <h3 class="stl">基础信息</h3>
          <div class="dg">
            <div class="df"><span class="fl">股票代码</span><span class="fv m">{{ stockStore.currentStock.stockCode }}</span></div>
            <div class="df"><span class="fl">Tushare代码</span><span class="fv m">{{ stockStore.currentStock.tsCode || '-' }}</span></div>
            <div class="df"><span class="fl">股票简称</span><span class="fv">{{ stockStore.currentStock.name }}</span></div>
            <div class="df"><span class="fl">股票全称</span><span class="fv">{{ stockStore.currentStock.fullName || '-' }}</span></div>
            <div class="df"><span class="fl">交易所代码</span><span class="fv">{{ stockStore.currentStock.exchange || '-' }}</span></div>
            <div class="df"><span class="fl">市场类型</span><span class="fv">{{ stockStore.currentStock.market || '-' }}</span></div>
          </div>
        </div>

        <div class="dsec">
          <h3 class="stl">上市信息</h3>
          <div class="dg">
            <div class="df"><span class="fl">上市日期</span><span class="fv">{{ stockStore.currentStock.listDate || '-' }}</span></div>
            <div class="df"><span class="fl">所属行业</span><span class="fv">{{ stockStore.currentStock.industry || '-' }}</span></div>
            <div class="df"><span class="fl">所属地域</span><span class="fv">{{ stockStore.currentStock.area || '-' }}</span></div>
          </div>
        </div>

        <div class="dsec">
          <h3 class="stl">股本信息</h3>
          <div class="dg">
            <div class="df"><span class="fl">总股本（股）</span><span class="fv m">{{ fmt(stockStore.currentStock.totalShares) }}</span></div>
            <div class="df"><span class="fl">流通股本（股）</span><span class="fv m">{{ fmt(stockStore.currentStock.floatShares) }}</span></div>
            <div class="df"><span class="fl">总股本（亿股）</span><span class="fv">{{ yi(stockStore.currentStock.totalShares) }}</span></div>
            <div class="df"><span class="fl">流通股本（亿股）</span><span class="fv">{{ yi(stockStore.currentStock.floatShares) }}</span></div>
          </div>
        </div>

        <div class="dsec" v-if="stockStore.currentStock.company">
          <h3 class="stl">关联公司</h3>
          <div class="rl">
            <div class="ri">
              <div>
                <div class="rin">{{ stockStore.currentStock.company.name }}</div>
                <div class="ts">统一信用代码: {{ stockStore.currentStock.company.unifiedSocialCreditCode }}</div>
              </div>
              <button class="lb" @click="goCompany">查看详情 →</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else style="text-align:center;padding:40px;color:var(--text-muted)">股票不存在</div>
</template>

<style scoped>
.bk {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-family: var(--font);
  padding: 6px 0;
  margin-bottom: 14px;
  transition: color 0.15s;
}

.bk:hover {
  color: var(--primary);
}

.dc {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.dh {
  padding: 22px 24px;
  border-bottom: 1px solid var(--border);
  background: var(--bg);
}

.dht {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 10px;
}

.dh-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.dt {
  font-size: 19px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--text-primary);
}

.ds {
  font-size: 13px;
  color: var(--text-secondary);
}

.db {
  padding: 22px 24px;
}

.dsec {
  margin-bottom: 28px;
}

.dsec:last-child {
  margin-bottom: 0;
}

.stl {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

.dg {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

@media (min-width: 1100px) {
  .dg {
    grid-template-columns: repeat(3, 1fr);
  }
}

.df {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.fl {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
  letter-spacing: 0.3px;
}

.fv {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.fv.m {
  font-family: var(--mono);
  font-size: 12px;
}

.sc {
  font-family: var(--mono);
  font-weight: 700;
  color: var(--primary);
  font-size: 18px;
}

.b {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.b-sh {
  background: #D6E4FF;
  color: #1A56DB;
}

[data-theme="dark"] .b-sh {
  background: rgba(26, 86, 219, 0.25);
  color: #7EB2FF;
}

.b-sz {
  background: #FFE4CC;
  color: #C24A00;
}

[data-theme="dark"] .b-sz {
  background: rgba(194, 74, 0, 0.25);
  color: #FF9F5A;
}

.b-bj {
  background: #EDD6FF;
  color: #6B1FA2;
}

[data-theme="dark"] .b-bj {
  background: rgba(107, 31, 162, 0.25);
  color: #C9A0FF;
}

.bm {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  background: var(--bg);
  color: var(--text-secondary);
  border: 1px solid var(--border);
}

.dl {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--primary);
  font-weight: 600;
  cursor: pointer;
  padding: 8px 16px;
  background: rgba(99, 91, 255, 0.08);
  border-radius: var(--radius-md);
  border: 1px solid rgba(99, 91, 255, 0.2);
  font-size: 13px;
  font-family: var(--font);
  transition: all 0.15s;
}

.dl:hover {
  background: rgba(99, 91, 255, 0.16);
}

.rl {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.ri {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--bg);
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  flex-wrap: wrap;
  gap: 8px;
}

.rin {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.ts {
  font-size: 12px;
  color: var(--text-muted);
}

.lb {
  color: var(--primary);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font);
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.1s;
}

.lb:hover {
  background: rgba(99, 91, 255, 0.1);
}
</style>
