import { client } from './axios'
import type { FinanceReportList, FinanceReportDetail, IndicatorResponse } from '@/types/finance'

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
  endDate?: string,
  reportType?: string
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
  if (reportType && reportType !== 'all') {
    params.reportType = reportType
  }
  const response = await client.get(`/finance/${stockCode}/indicators`, { params })
  return response.data
}

export async function getYearlyIndicators(
  stockCode: string,
  year: number
): Promise<IndicatorResponse> {
  const response = await client.get(`/finance/${stockCode}/indicators/yearly`, {
    params: { year: String(year) },
  })
  return response.data
}
