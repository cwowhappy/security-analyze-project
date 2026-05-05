import { client } from './axios'
import type {
  IndustryListResponse,
  IndustryTrendResponse,
} from '@/types/industry'
import type { CompanyListResponse } from '@/types/company'

export async function getIndustryList(
  standard: string = 'EM',
  level?: number,
  parentCode?: string,
): Promise<IndustryListResponse> {
  const params: Record<string, any> = { standard }
  if (level !== undefined) params.level = level
  if (parentCode) params.parentCode = parentCode
  const response = await client.get('/industries', { params })
  return response.data
}

export async function getIndustryCompanies(
  industryCode: string,
  standard: string = 'EM',
  parentCode?: string,
  params: { page?: number; size?: number } = {},
): Promise<CompanyListResponse> {
  const query: Record<string, any> = { standard, ...params }
  if (parentCode) query.parentCode = parentCode
  const response = await client.get(`/industries/${encodeURIComponent(industryCode)}/companies`, { params: query })
  return response.data
}

export async function getIndustryTrend(
  industryCode: string,
  standard: string = 'EM',
  period: string = '3m',
): Promise<IndustryTrendResponse> {
  const response = await client.get(`/industries/${encodeURIComponent(industryCode)}/trend`, {
    params: { standard, period },
  })
  return response.data
}
