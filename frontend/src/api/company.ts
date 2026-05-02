import { client } from './axios'
import type { CompanyListParams, CompanyListResponse, CompanyDetail } from '@/types/company'

export async function getCompanyList(params: CompanyListParams = {}): Promise<CompanyListResponse> {
  const response = await client.get('/companies', { params })
  return response.data
}

export async function getCompanyDetail(stockCode: string): Promise<CompanyDetail> {
  const response = await client.get(`/companies/${stockCode}`)
  return response.data
}
