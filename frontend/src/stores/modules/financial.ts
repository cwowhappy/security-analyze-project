import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { financialApi } from '@/api/modules/financial'
import type {
  FinancialIncome,
  FinancialBalance,
  FinancialCashflow,
  FinancialIndicator,
  TrendData,
  DupontAnalysis,
  PeerComparison,
} from '@/types/financial'

export type FinancialTab = 'analysis' | 'statements' | 'ai-report'

export const useFinancialStore = defineStore('financial', () => {
  // State
  const stockCode = ref<string | null>(null)
  const activeTab = ref<FinancialTab>('statements')
  const incomes = ref<FinancialIncome[]>([])
  const balances = ref<FinancialBalance[]>([])
  const cashflows = ref<FinancialCashflow[]>([])
  const indicators = ref<FinancialIndicator[]>([])
  const trendData = ref<TrendData[]>([])
  const dupontData = ref<DupontAnalysis | null>(null)
  const peerComparison = ref<PeerComparison | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const latestIndicator = computed<FinancialIndicator | null>(() =>
    indicators.value.length > 0 ? indicators.value[0] : null
  )

  const latestIncome = computed<FinancialIncome | null>(() =>
    incomes.value.length > 0 ? incomes.value[0] : null
  )

  const latestBalance = computed<FinancialBalance | null>(() =>
    balances.value.length > 0 ? balances.value[0] : null
  )

  const latestCashflow = computed<FinancialCashflow | null>(() =>
    cashflows.value.length > 0 ? cashflows.value[0] : null
  )

  // Actions
  const fetchIncome = async (code: string, limit: number = 20) => {
    loading.value = true
    error.value = null
    try {
      incomes.value = await financialApi.getIncome(code, undefined, limit)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载利润表失败'
    } finally {
      loading.value = false
    }
  }

  const fetchBalance = async (code: string, limit: number = 20) => {
    loading.value = true
    error.value = null
    try {
      balances.value = await financialApi.getBalance(code, undefined, limit)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载资产负债表失败'
    } finally {
      loading.value = false
    }
  }

  const fetchCashflow = async (code: string, limit: number = 20) => {
    loading.value = true
    error.value = null
    try {
      cashflows.value = await financialApi.getCashflow(code, undefined, limit)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载现金流量表失败'
    } finally {
      loading.value = false
    }
  }

  const fetchIndicators = async (code: string, limit: number = 20) => {
    loading.value = true
    error.value = null
    try {
      indicators.value = await financialApi.getIndicators(code, undefined, limit)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载财务指标失败'
    } finally {
      loading.value = false
    }
  }

  const fetchTrend = async (code: string, metrics: string[], periods: number = 8) => {
    loading.value = true
    try {
      trendData.value = await financialApi.getTrend(code, metrics, undefined, periods)
    } finally {
      loading.value = false
    }
  }

  const fetchDupont = async (code: string, reportDate: string, reportType: string) => {
    loading.value = true
    try {
      dupontData.value = await financialApi.getDupont(code, reportDate, reportType)
    } finally {
      loading.value = false
    }
  }

  const fetchPeerComparison = async (code: string, metric: string, reportType: string) => {
    loading.value = true
    error.value = null
    try {
      peerComparison.value = await financialApi.getPeerComparison(code, metric, reportType)
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载同业对比失败'
    } finally {
      loading.value = false
    }
  }

  const fetchAll = async (code: string) => {
    stockCode.value = code
    loading.value = true
    error.value = null
    try {
      const [i, b, c, ind] = await Promise.all([
        financialApi.getIncome(code, undefined, 12),
        financialApi.getBalance(code, undefined, 12),
        financialApi.getCashflow(code, undefined, 12),
        financialApi.getIndicators(code, undefined, 12),
      ])
      incomes.value = i
      balances.value = b
      cashflows.value = c
      indicators.value = ind
    } catch (e) {
      error.value = e instanceof Error ? e.message : '加载财务数据失败'
    } finally {
      loading.value = false
    }
  }

  const switchTab = (tab: FinancialTab) => {
    activeTab.value = tab
  }

  return {
    stockCode,
    activeTab,
    incomes,
    balances,
    cashflows,
    indicators,
    trendData,
    dupontData,
    peerComparison,
    loading,
    error,
    latestIndicator,
    latestIncome,
    latestBalance,
    latestCashflow,
    fetchIncome,
    fetchBalance,
    fetchCashflow,
    fetchIndicators,
    fetchTrend,
    fetchDupont,
    fetchPeerComparison,
    fetchAll,
    switchTab,
  }
})
