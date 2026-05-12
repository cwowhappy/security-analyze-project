<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { stockApi } from '@/api/modules/stock'
import type { Stock } from '@/types/stock'

const router = useRouter()

const page = ref(1)
const size = ref(20)
const keyword = ref('')
const market = ref('')
const industry = ref('')
const area = ref('')

const stocks = ref<Stock[]>([])
const total = ref(0)
const loading = ref(false)

const markets = ['主板', '创业板', '科创板', '北交所']

async function fetchData() {
  loading.value = true
  try {
    const result = await stockApi.page(
      { page: page.value, size: size.value },
      market.value || undefined,
      industry.value || undefined,
      area.value || undefined,
      keyword.value || undefined,
    )
    stocks.value = result.list ?? []
    total.value = result.total ?? 0
  } finally {
    loading.value = false
  }
}

function goDetail(stockCode: string) {
  router.push(`/stocks/${stockCode}`)
}

function prevPage() {
  if (page.value > 1) {
    page.value--
    fetchData()
  }
}

function nextPage() {
  if (stocks.value.length === size.value) {
    page.value++
    fetchData()
  }
}

function resetFilters() {
  page.value = 1
  keyword.value = ''
  market.value = ''
  industry.value = ''
  area.value = ''
  fetchData()
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

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="pg-hd">
      <h1 class="pg-t">股票列表</h1>
      <p class="pg-d">全市场 A 股股票基础信息，支持按市场、行业、地域筛选</p>
    </div>

    <div class="bar">
      <div class="sb">
        <input v-model="keyword" placeholder="搜索股票代码或名称..." @keyup.enter="page = 1; fetchData()" />
      </div>
      <div class="fg">
        <label>市场</label>
        <select v-model="market" @change="page = 1; fetchData()">
          <option value="">全部</option>
          <option v-for="m in markets" :key="m" :value="m">{{ m }}</option>
        </select>
      </div>
      <div class="fg">
        <label>行业</label>
        <input v-model="industry" placeholder="行业筛选" @keyup.enter="page = 1; fetchData()" style="min-width:120px" />
      </div>
      <div class="fg">
        <label>地域</label>
        <input v-model="area" placeholder="地域筛选" @keyup.enter="page = 1; fetchData()" style="min-width:120px" />
      </div>
      <button class="btn btn-s" @click="resetFilters">重置</button>
    </div>

    <div class="tc">
      <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-muted)">加载中...</div>
      <table v-else>
        <thead>
          <tr>
            <th>股票代码</th>
            <th>股票简称</th>
            <th>交易所</th>
            <th>市场类型</th>
            <th>行业</th>
            <th>地域</th>
            <th class="nr">总股本(亿股)</th>
            <th class="nr">流通股本(亿股)</th>
            <th>上市日期</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="stock in stocks" :key="stock.id" @click="goDetail(stock.stockCode)" class="cursor-row">
            <td><span class="sc">{{ stock.stockCode }}</span></td>
            <td><span class="sn">{{ stock.name }}</span></td>
            <td><span class="b" :class="exBadgeClass(stock.exchange)">{{ stock.exchange || '-' }}</span></td>
            <td><span class="bm">{{ stock.market || '-' }}</span></td>
            <td>{{ stock.industry || '-' }}</td>
            <td>{{ stock.area || '-' }}</td>
            <td class="nr">{{ yi(stock.totalShares) }}</td>
            <td class="nr">{{ yi(stock.floatShares) }}</td>
            <td>{{ stock.listDate || '-' }}</td>
            <td><button class="lb" @click.stop="goDetail(stock.stockCode)">详情</button></td>
          </tr>
          <tr v-if="!stocks.length">
            <td colspan="10" style="text-align:center;padding:40px;color:var(--text-muted)">无匹配结果</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="stocks.length">
      <button :disabled="page === 1" @click="prevPage">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / size) || 1 }} 页</span>
      <button :disabled="stocks.length < size" @click="nextPage">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.pg-hd {
  margin-bottom: 24px;
}

.pg-t {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 4px;
  color: var(--text-primary);
}

.pg-d {
  font-size: 13px;
  color: var(--text-secondary);
}

.bar {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 14px 18px;
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  box-shadow: var(--shadow-sm);
}

.sb {
  position: relative;
  flex: 1;
  min-width: 180px;
}

.sb input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-family: var(--font);
  color: var(--text-primary);
  background: var(--bg);
  transition: border-color 0.15s, background 0.25s;
}

.sb input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(99, 91, 255, 0.1);
}

.sb::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 14px;
  height: 14px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='%236B7A8D' viewBox='0 0 24 24'%3E%3Cpath d='M21 21l-4.35-4.35A7.5 7.5 0 1 0 16.65 4.35L21 8.65 21 21zM10 4a6 6 0 1 1-6 6 6 6 0 0 1 6-6z'/%3E%3C/svg%3E") center/contain no-repeat;
  opacity: 0.6;
}

.fg {
  display: flex;
  gap: 6px;
  align-items: center;
}

.fg label {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
  white-space: nowrap;
}

select, .fg input {
  padding: 8px 30px 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-family: var(--font);
  color: var(--text-primary);
  background: var(--surface);
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12'%3E%3Cpath d='M6 8L1 3h10z' fill='%236B7A8D'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  transition: background 0.25s;
}

select:focus, .fg input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(99, 91, 255, 0.1);
}

.btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font);
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--primary);
  color: #fff;
}

.btn:hover {
  background: var(--primary-hover);
}

.btn-s {
  background: var(--surface);
  color: var(--text-primary);
  border-color: var(--border);
}

.btn-s:hover {
  background: var(--surface-hover);
}

.tc {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

thead {
  background: var(--bg);
  border-bottom: 1px solid var(--border);
}

th {
  padding: 11px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

td {
  padding: 11px 16px;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}

tbody tr {
  transition: background 0.1s;
}

tbody tr:hover {
  background: var(--surface-hover);
}

tbody tr:last-child td {
  border-bottom: none;
}

.nr {
  text-align: right;
  font-family: var(--mono);
  font-size: 12px;
}

th.nr {
  text-align: right;
}

.sc {
  font-family: var(--mono);
  font-weight: 700;
  color: var(--primary);
  font-size: 13px;
}

.sn {
  font-weight: 500;
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

.cursor-row {
  cursor: pointer;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
}

.pagination button {
  padding: 6px 14px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s;
}

.pagination button:hover:not(:disabled) {
  border-color: var(--border);
  color: var(--text-primary);
  background: var(--surface-hover);
}

.pagination button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.pagination span {
  color: var(--text-muted);
  font-size: 13px;
  padding: 0 8px;
}
</style>
