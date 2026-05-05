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

export interface CompanyIndustryDto {
  standardName: string
  standardCode: string
  level1Code?: string
  level1Name?: string
  level2Code?: string
  level2Name?: string
  primary: boolean
}

export interface CompanyDetail extends Company {
  establishDate?: string
  registeredCapital?: number
  securities?: SecurityItem[]
  industries?: CompanyIndustryDto[]
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
