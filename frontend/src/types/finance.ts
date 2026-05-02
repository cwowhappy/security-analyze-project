export type ReportType = '年报' | '中报' | '一季报' | '三季报'

export interface FinanceReportItem {
  id: number
  reportDate: string
  reportType: ReportType
  reportYear: number
  noticeDate?: string
  totalRevenue?: number
  netProfit?: number
  parentNetProfit?: number
  totalAssets?: number
  totalEquity?: number
}

export interface FinanceReportList {
  stockCode: string
  stockName: string
  items: FinanceReportItem[]
}

export interface FinanceSummary {
  totalAssets?: number
  totalLiabilities?: number
  totalEquity?: number
  totalRevenue?: number
  operateCost?: number
  operateProfit?: number
  netProfit?: number
  parentNetProfit?: number
  operatingCashFlow?: number
}

export interface FinanceReportDetail {
  id: number
  stockCode: string
  reportDate: string
  reportType: ReportType
  reportYear: number
  noticeDate?: string
  currency: string
  summary: FinanceSummary
  balanceSheet?: Record<string, any>
  profitSheet?: Record<string, any>
  cashFlowSheet?: Record<string, any>
}

export interface IndicatorDataPoint {
  reportDate: string
  value: number
}

export interface IndicatorMetric {
  metric: string
  label: string
  unit: string
  data: IndicatorDataPoint[]
}

export interface IndicatorResponse {
  stockCode: string
  metrics: IndicatorMetric[]
}
