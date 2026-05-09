export interface AnnualMetric {
  reportDate: string
  reportYear: number
  // 盈利能力
  totalRevenue: number
  operateIncome: number
  operateCost: number
  parentNetProfit: number
  grossMargin: number
  netMargin: number
  roe: number
  // 资产负债
  totalAssets: number
  totalLiabilities: number
  totalEquity: number
  debtRatio: number
  // 现金流
  operatingCashFlow: number
  investingCashFlow: number
  financingCashFlow: number
  endCce: number
  cashflowProfitRatio: number
  // 成本费用
  saleExpense: number
  manageExpense: number
  researchExpense: number
  financeExpense: number
  periodExpenseRate: number
}

export interface FundamentalOverview {
  stockCode: string
  stockName: string
  industry: string
  market: string
  metrics: AnnualMetric[]
}

export interface ScreenParams {
  keyword?: string
  industry?: string
  market?: string
  page?: number
  size?: number
}

export interface ScreenCompanyItem {
  stockCode: string
  stockName: string
  industry: string
  market: string
  latestRevenue?: number
  latestProfit?: number
}

export interface ScreenResponse {
  items: ScreenCompanyItem[]
  total: number
  page: number
  size: number
}

export interface PeerMetric {
  stockCode: string
  stockName: string
  industry: string
  totalRevenue?: number
  parentNetProfit?: number
  roe?: number
  debtRatio?: number
}

export interface IndustryPeersResponse {
  peers: PeerMetric[]
}
