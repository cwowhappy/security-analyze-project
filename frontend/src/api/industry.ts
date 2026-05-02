import axios from 'axios'
import type {
  IndustryListResponse,
  IndustryTrendResponse,
} from '@/types/industry'
import type { CompanyListResponse } from '@/types/company'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const client = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
})

export async function getIndustryList(): Promise<IndustryListResponse> {
  const response = await client.get('/industries')
  return response.data
}

export async function getIndustryCompanies(
  industryName: string,
  params: { page?: number; size?: number } = {},
): Promise<CompanyListResponse> {
  const response = await client.get(`/industries/${encodeURIComponent(industryName)}/companies`, { params })
  return response.data
}

export async function getIndustryTrend(
  industryName: string,
  period: string = '3m',
): Promise<IndustryTrendResponse> {
  const response = await client.get(`/industries/${encodeURIComponent(industryName)}/trend`, {
    params: { period },
  })
  return response.data
}
