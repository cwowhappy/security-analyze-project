/**
 * 公司领域类型定义
 */

export interface Company {
  id: string
  unifiedSocialCreditCode: string
  name: string
  shortName: string | null
  englishName: string | null
  formerName: string | null
  legalRepresentative: string | null
  chairman: string | null
  manager: string | null
  secretary: string | null
  regCapital: number | null
  setupDate: string | null
  province: string | null
  city: string | null
  regAddress: string | null
  officeAddress: string | null
  website: string | null
  industry: string | null
  mainBusiness: string | null
  businessScope: string | null
  introduction: string | null
  employees: number | null
  controllerName: string | null
  controllerType: string | null
  updatedAt: string
  createdAt: string
}

export interface CompanyDetail extends Company {
  stocks: StockBrief[]
}

export interface StockBrief {
  stockCode: string
  name: string
  market: string | null
  exchange: string | null
  listDate: string | null
}

export interface CreateCompanyRequest {
  unifiedSocialCreditCode: string
  name: string
  shortName?: string
  englishName?: string
  formerName?: string
  legalRepresentative?: string
  chairman?: string
  manager?: string
  secretary?: string
  regCapital?: number
  setupDate?: string
  province?: string
  city?: string
  regAddress?: string
  officeAddress?: string
  website?: string
  industry?: string
  mainBusiness?: string
  businessScope?: string
  introduction?: string
  employees?: number
  controllerName?: string
  controllerType?: string
}
