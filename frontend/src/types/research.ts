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
  totalCurrentAssets?: number
  totalNoncurrentAssets?: number
  totalCurrentLiabilities?: number
  totalNoncurrentLiabilities?: number
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

  // 阶段B衍生指标
  revenueYoy?: number
  profitYoy?: number
  roa?: number
  assetTurnover?: number
  equityMultiplier?: number
  currentRatio?: number
  quickRatio?: number
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

export interface IndustryRankItem {
  stockCode: string
  stockName: string
  industry: string
  totalRevenue?: number
  parentNetProfit?: number
  grossMargin?: number
  roe?: number
  debtRatio?: number
}

export interface IndustryRankResponse {
  rank: number
  total: number
  sortBy: string
  order: string
  items: IndustryRankItem[]
}

// ==================== 阶段C：估值分析 ====================

export interface CompositeScore {
  financialHealthScore: number
  valuationAppealScore: number
  overallScore: number
}

export interface ValuationWarning {
  metric: string
  level: 'high' | 'medium' | 'low'
  message: string
}

export interface ValuationOverview {
  stockCode: string
  stockName: string
  currentPrice: number
  marketCap: number
  peTtm?: number
  peTtmPercentile?: number
  peLyr?: number
  pb?: number
  pbPercentile?: number
  psTtm?: number
  psTtmPercentile?: number
  compositeScore: CompositeScore
  warnings: ValuationWarning[]
}

export interface ValuationHistoryItem {
  tradeDate: string
  closePrice: number
  peTtm?: number
  peLyr?: number
  pb?: number
  psTtm?: number
}

export interface ValuationHistoryResponse {
  stockCode: string
  stockName: string
  items: ValuationHistoryItem[]
}

export interface DcfRequest {
  growthRate?: number
  discountRate?: number
  terminalGrowthRate?: number
  projectionYears?: number
  baseCashFlow?: number
}

export interface DcfResponse {
  fairPrice: number
  fairPriceRangeLow?: number
  fairPriceRangeHigh?: number
  upsidePercent?: number
  appliedAssumptions: DcfRequest
}
