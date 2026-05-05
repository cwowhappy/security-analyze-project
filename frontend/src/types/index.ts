export interface IndexListItem {
  indexCode: string
  indexName: string
  indexType: string
  market: string
  publishDate: string
}

export interface IndexListResponse {
  items: IndexListItem[]
  total: number
  page: number
  size: number
}

export interface IndexDetailResponse {
  indexCode: string
  indexName: string
  indexType: string
  market: string
  baseDate: string
  basePoint: number
  componentCount: number
  publishDate: string
}

export interface IndexTrendItem {
  tradeDate: string
  openPrice: number
  highPrice: number
  lowPrice: number
  closePrice: number
  volume: number
  amount: number
  amplitude: number
  changePct: number
  changeAmount: number
  turnoverRate: number
}

export interface IndexTrendResponse {
  indexCode: string
  granularity: string
  items: IndexTrendItem[]
}

export interface IndexCategoryGroup {
  indexType: string
  indexTypeLabel: string
  items: IndexListItem[]
}

export interface EtfListItem {
  etfCode: string
  etfName: string
  trackingIndexCode: string
  managementFee: number
  fundSize: number
  establishDate: string
  market: string
}
