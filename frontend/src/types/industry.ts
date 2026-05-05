export interface IndustryCategoryDto {
  code: string
  name: string
  level: number
  parentCode?: string
  companyCount?: number
}

export interface IndustryListResponse {
  standard: string
  level?: number
  data: IndustryCategoryDto[]
  total: number
}

export interface TrendDataPoint {
  date: string
  close: number
  changePercent: number
}

export interface IndustryTrendResponse {
  standard: string
  industryCode: string
  industryName: string
  period: string
  data: TrendDataPoint[]
  fallback: boolean
}
