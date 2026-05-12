import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { stockApi } from '@/api/modules/stock'
import type { Stock, StockDetail } from '@/types/stock'
import type { PageQuery } from '@/types/api'

/**
 * 股票状态管理 Store（Composition API 风格）
 */
export const useStockStore = defineStore('stock', () => {
  // State
  const stocks = ref<Stock[]>([])
  const stockTotal = ref(0)
  const currentStock = ref<StockDetail | null>(null)
  const loading = ref(false)

  // Getters
  const stockCount = computed(() => stocks.value.length)

  // Actions
  const fetchStockPage = async (
    query: PageQuery,
    market?: string,
    industry?: string,
    area?: string,
    keyword?: string,
  ) => {
    loading.value = true
    try {
      const result = await stockApi.page(query, market, industry, area, keyword)
      stocks.value = result.list
      stockTotal.value = result.total
    } finally {
      loading.value = false
    }
  }

  const fetchStockDetail = async (stockCode: string) => {
    loading.value = true
    try {
      const data = await stockApi.getByStockCode(stockCode)
      currentStock.value = data
    } finally {
      loading.value = false
    }
  }

  const getStockByCode = (stockCode: string) => {
    return stocks.value.find(s => s.stockCode === stockCode)
  }

  return {
    stocks,
    stockTotal,
    currentStock,
    loading,
    stockCount,
    fetchStockPage,
    fetchStockDetail,
    getStockByCode,
  }
})
