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

function goBack() {
  router.push('/companies')
}

function goStock(stockCode: string) {
  router.push(`/stocks/${stockCode}`)
}

function fmt(n: number | null) {
  if (!n) return '-'
  return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function ctBadgeClass(t: string | null) {
  const map: Record<string, string> = { '国企': 'bs-gq', '民营': 'bs-my', '外资': 'bs-wz', '其他': 'bs-qt' }
  return map[t || '其他'] || 'bs-qt'
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
  <div v-if="companyStore.loading" style="text-align:center;padding:40px;color:var(--text-muted)">加载中...</div>
  <div v-else-if="companyStore.currentCompany">
    <button class="bk" @click="goBack">← 返回公司列表</button>

    <div class="dc">
      <div class="dh">
        <div class="dht">
          <div class="dt">
            <span>{{ companyStore.currentCompany.name }}</span>
            <span class="bs" :class="ctBadgeClass(companyStore.currentCompany.controllerType)">
              {{ companyStore.currentCompany.controllerType || '其他' }}
            </span>
          </div>
          <button class="dl" @click="goStock(companyStore.currentCompany.stocks?.[0]?.stockCode || '')" v-if="companyStore.currentCompany.stocks?.length">
            📈 查看关联股票
          </button>
        </div>
        <div class="ds">{{ companyStore.currentCompany.shortName || '' }} {{ companyStore.currentCompany.englishName ? '| ' + companyStore.currentCompany.englishName : '' }}</div>
      </div>

      <div class="db">
        <div class="dsec">
          <h3 class="stl">工商注册信息</h3>
          <div class="dg">
            <div class="df"><span class="fl">统一社会信用代码</span><span class="fv m">{{ companyStore.currentCompany.unifiedSocialCreditCode }}</span></div>
            <div class="df"><span class="fl">公司全称</span><span class="fv">{{ companyStore.currentCompany.name }}</span></div>
            <div class="df"><span class="fl">公司简称</span><span class="fv">{{ companyStore.currentCompany.shortName || '-' }}</span></div>
            <div class="df"><span class="fl">英文名称</span><span class="fv">{{ companyStore.currentCompany.englishName || '-' }}</span></div>
            <div class="df"><span class="fl">曾用简称</span><span class="fv">{{ companyStore.currentCompany.formerName || '-' }}</span></div>
            <div class="df"><span class="fl">注册资本（万元）</span><span class="fv m">{{ fmt(companyStore.currentCompany.regCapital) }}</span></div>
          </div>
        </div>

        <div class="dsec">
          <h3 class="stl">成立与地址信息</h3>
          <div class="dg">
            <div class="df"><span class="fl">成立日期</span><span class="fv">{{ companyStore.currentCompany.setupDate || '-' }}</span></div>
            <div class="df"><span class="fl">所在省份</span><span class="fv">{{ companyStore.currentCompany.province || '-' }}</span></div>
            <div class="df"><span class="fl">所在城市</span><span class="fv">{{ companyStore.currentCompany.city || '-' }}</span></div>
            <div class="df"><span class="fl">注册地址</span><span class="fv">{{ companyStore.currentCompany.regAddress || '-' }}</span></div>
            <div class="df"><span class="fl">办公地址</span><span class="fv">{{ companyStore.currentCompany.officeAddress || '-' }}</span></div>
            <div class="df"><span class="fl">官方网站</span><span class="fv">{{ companyStore.currentCompany.website || '-' }}</span></div>
          </div>
        </div>

        <div class="dsec">
          <h3 class="stl">管理层信息</h3>
          <div class="dg">
            <div class="df"><span class="fl">法人代表</span><span class="fv">{{ companyStore.currentCompany.legalRepresentative || '-' }}</span></div>
            <div class="df"><span class="fl">董事长</span><span class="fv">{{ companyStore.currentCompany.chairman || '-' }}</span></div>
            <div class="df"><span class="fl">总经理</span><span class="fv">{{ companyStore.currentCompany.manager || '-' }}</span></div>
            <div class="df"><span class="fl">董事会秘书</span><span class="fv">{{ companyStore.currentCompany.secretary || '-' }}</span></div>
            <div class="df"><span class="fl">员工人数</span><span class="fv">{{ fmt(companyStore.currentCompany.employees) }}</span></div>
          </div>
        </div>

        <div class="dsec">
          <h3 class="stl">业务与治理信息</h3>
          <div class="dg">
            <div class="df"><span class="fl">所属行业</span><span class="fv">{{ companyStore.currentCompany.industry || '-' }}</span></div>
            <div class="df"><span class="fl">实控人名称</span><span class="fv">{{ companyStore.currentCompany.controllerName || '-' }}</span></div>
            <div class="df"><span class="fl">实控人性质</span><span class="fv">{{ companyStore.currentCompany.controllerType || '-' }}</span></div>
          </div>
        </div>

        <div class="dsec">
          <h3 class="stl">主营业务</h3>
          <div class="txt-box">{{ companyStore.currentCompany.mainBusiness || '暂无数据' }}</div>
        </div>

        <div class="dsec">
          <h3 class="stl">经营范围</h3>
          <div class="txt-box">{{ companyStore.currentCompany.businessScope || '暂无数据' }}</div>
        </div>

        <div class="dsec">
          <h3 class="stl">公司简介</h3>
          <div class="txt-box">{{ companyStore.currentCompany.introduction || '暂无数据' }}</div>
        </div>

        <div class="dsec" v-if="companyStore.currentCompany.stocks?.length">
          <h3 class="stl">关联股票</h3>
          <div class="rl">
            <div class="ri" v-for="stock in companyStore.currentCompany.stocks" :key="stock.stockCode">
              <div>
                <span class="ric">{{ stock.stockCode }}</span>
                <span class="rin" style="margin-left:12px">{{ stock.name }}</span>
                <span class="b" :class="exBadgeClass(stock.exchange)">{{ stock.exchange || '-' }}</span>
              </div>
              <button class="lb" @click="goStock(stock.stockCode)">查看详情 →</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else style="text-align:center;padding:40px;color:var(--text-muted)">公司不存在</div>
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

.bs {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.bs-gq {
  background: rgba(0, 120, 220, 0.12);
  color: #0078DC;
}

[data-theme="dark"] .bs-gq {
  background: rgba(0, 120, 220, 0.2);
  color: #5BABFF;
}

.bs-my {
  background: rgba(0, 180, 80, 0.12);
  color: #00B450;
}

[data-theme="dark"] .bs-my {
  background: rgba(0, 180, 80, 0.2);
  color: #4DE080;
}

.bs-wz {
  background: rgba(220, 100, 0, 0.12);
  color: #DC6400;
}

[data-theme="dark"] .bs-wz {
  background: rgba(220, 100, 0, 0.2);
  color: #FF9F4A;
}

.bs-qt {
  background: var(--surface-hover);
  color: var(--text-secondary);
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

.txt-box {
  padding: 12px 16px;
  background: var(--bg);
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-primary);
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

.ric {
  font-family: var(--font);
  font-weight: 700;
  color: var(--primary);
  font-size: 13px;
}

.rin {
  font-size: 13px;
  color: var(--text-primary);
}

.b {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.3px;
  margin-left: 8px;
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
