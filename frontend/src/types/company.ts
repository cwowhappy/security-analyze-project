export interface SecurityItem {
  stockCode: string
  stockName: string
  market?: string
  securityType?: string
  listingDate?: string
  listingStatus?: string
}

export interface Company {
  stockCode: string
  stockName: string
  industry?: string
  region?: string
  listingDate?: string
  market?: string
}

export interface CompanyDetail extends Company {
  establishDate?: string
  registeredCapital?: number
  securities?: SecurityItem[]
}

export interface CompanyListParams {
  keyword?: string
  page?: number
  size?: number
}

export interface CompanyListResponse {
  items: Company[]
  total: number
  page: number
  size: number
}
