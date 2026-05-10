import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { stockApi } from '@/api/modules/stock'
import type { Stock } from '@/types/stock'

/**
 * 股票状态管理 Store（Composition API 风格）
 */
export const useStockStore = defineStore('stock', () => {
  // State
  const stocks = ref<Stock[]>([])
  const loading = ref(false)

  // Getters
  const stockCount = computed(() => stocks.value.length)

  // Actions
  const fetchStocks = async () => {
    loading.value = true
    try {
      const data = await stockApi.list()
      stocks.value = data
    } finally {
      loading.value = false
    }
  }

  const getStockBySymbol = (symbol: string) => {
    return stocks.value.find(s => s.symbol === symbol)
  }

  return {
    stocks,
    loading,
    stockCount,
    fetchStocks,
    getStockBySymbol,
  }
})
