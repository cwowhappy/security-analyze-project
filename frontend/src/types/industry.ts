export interface IndustryListItem {
  industryName: string
  companyCount: number
}

export interface IndustryListResponse {
  data: IndustryListItem[]
  total: number
}

export interface TrendDataPoint {
  date: string
  close: number
  changePercent: number
}

export interface IndustryTrendResponse {
  industryName: string
  period: string
  data: TrendDataPoint[]
  fallback: boolean
}
