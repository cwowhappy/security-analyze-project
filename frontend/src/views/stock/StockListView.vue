<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStockStore } from '@/stores/modules/stock'

const stockStore = useStockStore()
const router = useRouter()

const page = ref(1)
const size = ref(20)
const industry = ref('')
const market = ref('')

const fetchData = () => {
  stockStore.fetchStockPage(
    { page: page.value, size: size.value },
    industry.value || undefined,
    market.value || undefined,
  )
}

const goDetail = (stockCode: string) => {
  router.push(`/stocks/${stockCode}`)
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    fetchData()
  }
}

const nextPage = () => {
  if (stockStore.stocks.length === size.value) {
    page.value++
    fetchData()
  }
}

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="card">
      <div class="card-title">股票列表</div>
      <div class="filter-bar">
        <select v-model="market" @change="page = 1; fetchData()">
          <option value="">全部市场</option>
          <option value="SZ">深市</option>
          <option value="SH">沪市</option>
          <option value="BJ">北交所</option>
        </select>
        <input v-model="industry" placeholder="行业筛选" @keyup.enter="page = 1; fetchData()" />
        <button class="btn btn-primary btn-sm" @click="page = 1; fetchData()">筛选</button>
      </div>

      <div v-if="stockStore.loading" class="loading-pulse">加载中...</div>

      <template v-else>
        <table class="table">
          <thead>
            <tr>
              <th>代码</th>
              <th>名称</th>
              <th>市场</th>
              <th>交易所</th>
              <th>行业</th>
              <th>地域</th>
              <th class="text-right">总股本</th>
              <th class="text-right">流通股本</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="stock in stockStore.stocks"
              :key="stock.id"
              @click="goDetail(stock.stockCode)"
              class="cursor-row"
            >
              <td class="font-mono text-primary">{{ stock.stockCode }}</td>
              <td>
                <a @click.stop="goDetail(stock.stockCode)">{{ stock.name }}</a>
              </td>
              <td>
                <span class="market-tag" :class="`market-${stock.market?.toLowerCase()}`">
                  {{ stock.market || '-' }}
                </span>
              </td>
              <td>{{ stock.exchange || '-' }}</td>
              <td>{{ stock.industry || '-' }}</td>
              <td>{{ stock.area || '-' }}</td>
              <td class="text-right font-mono">{{ stock.totalShares ? stock.totalShares.toLocaleString() : '-' }}</td>
              <td class="text-right font-mono">{{ stock.floatShares ? stock.floatShares.toLocaleString() : '-' }}</td>
            </tr>
            <tr v-if="!stockStore.stocks.length">
              <td colspan="8" class="empty-state">暂无数据</td>
            </tr>
          </tbody>
        </table>

        <div class="pagination" v-if="stockStore.stocks.length">
          <button :disabled="page === 1" @click="prevPage">上一页</button>
          <span>第 {{ page }} 页</span>
          <button :disabled="stockStore.stocks.length < size" @click="nextPage">下一页</button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.cursor-row {
  cursor: pointer;
}

.text-right {
  text-align: right;
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
