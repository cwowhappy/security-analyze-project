<script setup lang="ts">
import { onMounted } from 'vue'
import { useStockStore } from '@/stores/modules/stock'
import BaseButton from '@/components/base/BaseButton.vue'

const stockStore = useStockStore()

onMounted(() => {
  stockStore.fetchStocks()
})
</script>

<template>
  <div class="stock-list">
    <h1>股票列表</h1>
    <BaseButton @click="stockStore.fetchStocks">刷新</BaseButton>
    <div v-if="stockStore.loading">加载中...</div>
    <ul v-else>
      <li v-for="stock in stockStore.stocks" :key="stock.id">
        {{ stock.symbol }} - {{ stock.name }} ({{ stock.currentPrice }})
      </li>
    </ul>
  </div>
</template>

<style scoped>
.stock-list {
  padding: 20px;
}
</style>
