/**
 * 股票领域类型定义（Phase 3 更新版，与后端 12 字段对齐）
 */

export interface Stock {
  id: string
  stockCode: string
  tsCode: string | null
  name: string
  fullName: string | null
  market: string | null
  exchange: string | null
  listDate: string | null
  industry: string | null
  area: string | null
  totalShares: number | null
  floatShares: number | null
  updatedAt: string
  createdAt: string
}

export interface StockDetail extends Stock {
  company?: CompanyBrief
}

export interface CompanyBrief {
  id: string
  unifiedSocialCreditCode: string
  name: string
  legalRepresentative: string | null
  regCapital: number | null
  setupDate: string | null
  mainBusiness: string | null
}

export interface CreateStockRequest {
  stockCode: string
  name: string
  tsCode?: string
  fullName?: string
  market?: string
  exchange?: string
  listDate?: string
  industry?: string
  area?: string
  totalShares?: number
  floatShares?: number
}
