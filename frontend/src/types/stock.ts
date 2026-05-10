/**
 * 股票领域类型定义
 */

export interface Stock {
  id: string
  symbol: string
  name: string
  market: string
  currentPrice: number
  changePercent: number
  updatedAt: string
}

export interface CreateStockRequest {
  symbol: string
  name: string
  market?: string
  currentPrice: number
  changePercent?: number
}
