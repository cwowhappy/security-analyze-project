export type PortfolioType = 'REAL' | 'SIMULATION'

export type TradeType = 'BUY' | 'SELL' | 'DIVIDEND' | 'BONUS' | 'RIGHTS' | 'SPLIT' | 'MERGER' | 'OTHER'

export const TRADE_TYPE_LABELS: Record<TradeType, string> = {
  BUY: '买入',
  SELL: '卖出',
  DIVIDEND: '现金分红',
  BONUS: '送股',
  RIGHTS: '配股',
  SPLIT: '股份拆分',
  MERGER: '吸收合并',
  OTHER: '其他',
}

export interface Portfolio {
  id: number
  name: string
  type: PortfolioType
  broker?: string
  description?: string
  createdAt: string
}

export interface PortfolioRequest {
  name: string
  type: PortfolioType
  broker?: string
  description?: string
}

export interface Transaction {
  id: number
  portfolioId: number
  stockCode: string
  stockName?: string
  tradeDate: string
  tradeType: TradeType
  tradeTypeLabel: string
  price?: number
  quantity: number
  fee: number
  tax: number
  amount?: number
  realizedPnl: number
  remark?: string
  createdAt: string
}

export interface TransactionRequest {
  stockCode: string
  tradeDate: string
  tradeType: TradeType
  price?: number
  quantity: number
  fee?: number
  tax?: number
  remark?: string
}

export interface TransactionListResponse {
  items: Transaction[]
  total: number
  page: number
  size: number
}

export interface Position {
  stockCode: string
  stockName?: string
  industry?: string
  market?: string
  currentQuantity: number
  avgCost: number
  closePrice?: number
  marketValue?: number
  totalCost: number
  floatingPnl?: number
  floatingPnlRate?: number
  realizedPnl: number
  firstBuyDate?: string
  lastTradeDate?: string
  weight?: number
}

export interface PortfolioSummary {
  portfolioId: number
  portfolioName: string
  totalMarketValue: number
  totalCost: number
  totalFloatingPnl: number
  totalFloatingPnlRate: number
  totalRealizedPnl: number
  totalAssetReturn: number
  totalAssetReturnRate: number
  holdingCount: number
  latestTradeDate?: string
}
