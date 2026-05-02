import axios from 'axios'
import type { FinanceReportList, FinanceReportDetail, IndicatorResponse } from '@/types/finance'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const client = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
})

export async function getFinanceReports(stockCode: string): Promise<FinanceReportList> {
  const response = await client.get(`/finance/${stockCode}/reports`)
  return response.data
}

export async function getFinanceReportDetail(reportId: number): Promise<FinanceReportDetail> {
  const response = await client.get(`/finance/reports/${reportId}`)
  return response.data
}

export async function getFinanceIndicators(
  stockCode: string,
  metrics?: string[],
  startDate?: string,
  endDate?: string
): Promise<IndicatorResponse> {
  const params: Record<string, string> = {}
  if (metrics && metrics.length > 0) {
    params.metrics = metrics.join(',')
  }
  if (startDate) {
    params.startDate = startDate
  }
  if (endDate) {
    params.endDate = endDate
  }
  const response = await client.get(`/finance/${stockCode}/indicators`, { params })
  return response.data
}
