import axios from 'axios'
import type { CompanyListParams, CompanyListResponse, CompanyDetail } from '@/types/company'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const client = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
})

export async function getCompanyList(params: CompanyListParams = {}): Promise<CompanyListResponse> {
  const response = await client.get('/companies', { params })
  return response.data
}

export async function getCompanyDetail(stockCode: string): Promise<CompanyDetail> {
  const response = await client.get(`/companies/${stockCode}`)
  return response.data
}
