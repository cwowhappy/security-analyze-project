import { http } from '@/utils/request'
import type {
  FinancialIncome,
  FinancialBalance,
  FinancialCashflow,
  FinancialIndicator,
  TrendData,
  DupontAnalysis,
  PeerComparison,
} from '@/types/financial'

const PREFIX = (stockCode: string) => `/api/v1/stocks/${stockCode}/financial`

function buildUrl(path: string, params: Record<string, string | number | undefined>): string {
  const searchParams = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      searchParams.set(key, String(value))
    }
  })
  const query = searchParams.toString()
  return query ? `${path}?${query}` : path
}

export const financialApi = {
  /** 获取利润表 */
  getIncome: (stockCode: string, reportType?: string, limit: number = 20) =>
    http.get<FinancialIncome[]>(
      buildUrl(`${PREFIX(stockCode)}/income`, { reportType, limit })
    ),

  /** 获取资产负债表 */
  getBalance: (stockCode: string, reportType?: string, limit: number = 20) =>
    http.get<FinancialBalance[]>(
      buildUrl(`${PREFIX(stockCode)}/balance`, { reportType, limit })
    ),

  /** 获取现金流量表 */
  getCashflow: (stockCode: string, reportType?: string, limit: number = 20) =>
    http.get<FinancialCashflow[]>(
      buildUrl(`${PREFIX(stockCode)}/cashflow`, { reportType, limit })
    ),

  /** 获取财务指标 */
  getIndicators: (stockCode: string, reportType?: string, limit: number = 20) =>
    http.get<FinancialIndicator[]>(
      buildUrl(`${PREFIX(stockCode)}/indicator`, { reportType, limit })
    ),

  /** 获取趋势数据 */
  getTrend: (stockCode: string, metrics: string[], reportType?: string, periods: number = 8) =>
    http.get<TrendData[]>(
      buildUrl(`${PREFIX(stockCode)}/trend`, { metrics: metrics.join(','), reportType, periods })
    ),

  /** 获取杜邦分析 */
  getDupont: (stockCode: string, reportDate: string, reportType: string) =>
    http.get<DupontAnalysis>(
      buildUrl(`${PREFIX(stockCode)}/dupont`, { reportDate, reportType })
    ),

  /** 获取同业对比 */
  getPeerComparison: (stockCode: string, metric: string, reportType: string) =>
    http.get<PeerComparison>(
      buildUrl(`${PREFIX(stockCode)}/peer-comparison`, { metric, reportType })
    ),
}
