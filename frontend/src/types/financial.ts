/**
 * 财务分析模块类型定义
 */

/** 利润表 */
export interface FinancialIncome {
  stockCode: string
  reportDate: string
  reportType: string
  basicEps: number | null
  dilutedEps: number | null
  totalRevenue: number | null
  revenue: number | null
  operatingCost: number | null
  grossProfit: number | null
  grossMargin: number | null
  sellingExpense: number | null
  adminExpense: number | null
  rdExpense: number | null
  financialExpense: number | null
  operatingProfit: number | null
  totalProfit: number | null
  netProfit: number | null
  npParentCompany: number | null
  npExclNonrecurring: number | null
  netMargin: number | null
}

/** 资产负债表 */
export interface FinancialBalance {
  stockCode: string
  reportDate: string
  reportType: string
  totalAssets: number | null
  totalLiabilities: number | null
  totalEquity: number | null
  equityParentCompany: number | null
  currentAssets: number | null
  nonCurrentAssets: number | null
  cashEquivalents: number | null
  accountsReceivable: number | null
  inventories: number | null
  currentLiabilities: number | null
  nonCurrentLiabilities: number | null
  accountsPayable: number | null
  shortTermBorrowings: number | null
  longTermBorrowings: number | null
  goodwill: number | null
  debtRatio: number | null
}

/** 现金流量表 */
export interface FinancialCashflow {
  stockCode: string
  reportDate: string
  reportType: string
  cfOperating: number | null
  cfInvesting: number | null
  cfFinancing: number | null
  netCashFlow: number | null
  freeCashFlow: number | null
  capex: number | null
  cashReceivedOperating: number | null
  taxPaid: number | null
  cfoToNetProfit: number | null
}

/** 财务指标 */
export interface FinancialIndicator {
  stockCode: string
  reportDate: string
  reportType: string
  roe: number | null
  roa: number | null
  roic: number | null
  grossMargin: number | null
  netMargin: number | null
  netMarginExcl: number | null
  debtRatio: number | null
  currentRatio: number | null
  quickRatio: number | null
  netDebtRatio: number | null
  equityRatio: number | null
  dso: number | null
  dio: number | null
  dpo: number | null
  ccc: number | null
  assetTurnover: number | null
  fixedAssetTurnover: number | null
  revenueGrowth: number | null
  npParentGrowth: number | null
  npExclGrowth: number | null
  cfoGrowth: number | null
  equityGrowth: number | null
  assetGrowth: number | null
  pe: number | null
  pb: number | null
  ps: number | null
  peg: number | null
  evEbitda: number | null
  dividendYield: number | null
  marketCap: number | null
  cfoToNp: number | null
}

/** 趋势数据点 */
export interface TrendPoint {
  reportDate: string
  value: number | null
}

/** 趋势数据 */
export interface TrendData {
  stockCode: string
  metric: string
  data: TrendPoint[]
}

/** 杜邦分析 */
export interface DupontAnalysis {
  stockCode: string
  reportDate: string
  reportType: string
  roe: number | null
  netMargin: number | null
  assetTurnover: number | null
  equityMultiplier: number | null
}

/** 同业对比项 */
export interface PeerItem {
  stockCode: string
  stockName: string
  value: number | null
}

/** 同业对比 */
export interface PeerComparison {
  stockCode: string
  metric: string
  metricName: string
  stockValue: number | null
  industryAvg: number | null
  industryMedian: number | null
  industryMax: number | null
  industryMin: number | null
  peers: PeerItem[]
}

/** AI 财报解读报告 */
export interface AIReportAnalysis {
  stockCode: string
  stockName: string | null
  reportDate: string
  reportType: string
  scoreProfitability: number | null
  scoreGrowth: number | null
  scoreCashflow: number | null
  scoreFinancialHealth: number | null
  scoreOverall: number | null
  reportContent: string | null
  summary: string | null
  riskSignals: string[] | null
  aiModel: string | null
  status: string
}
