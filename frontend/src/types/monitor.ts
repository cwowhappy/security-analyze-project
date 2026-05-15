export interface CollectionMonitorOverview {
  taskType: string
  totalCount: number
  recentSuccessCount: number
  recentExpiredCount: number
}

export interface CollectionMonitorBaseline {
  totalStocks: number
}
