import { client } from './axios'
import type {
  FundamentalOverview,
  ScreenParams,
  ScreenResponse,
  IndustryPeersResponse,
  IndustryRankResponse,
  ValuationOverview,
  ValuationHistoryResponse,
  DcfRequest,
  DcfResponse,
} from '@/types/research'

export async function getFundamentalOverview(
  stockCode: string
): Promise<FundamentalOverview> {
  const response = await client.get(`/research/fundamental/overview/${stockCode}`)
  return response.data
}

export async function screenCompanies(
  params: ScreenParams
): Promise<ScreenResponse> {
  const response = await client.get('/research/fundamental/screen', { params })
  return response.data
}

export async function getIndustryPeers(
  stockCode: string
): Promise<IndustryPeersResponse> {
  const response = await client.get(`/research/fundamental/industry-peers/${stockCode}`)
  return response.data
}

export async function getIndustryRank(
  stockCode: string,
  sortBy: string = 'roe',
  order: string = 'desc'
): Promise<IndustryRankResponse> {
  const response = await client.get(`/research/fundamental/industry-rank/${stockCode}`, {
    params: { sortBy, order },
  })
  return response.data
}

export async function getValuationOverview(stockCode: string): Promise<ValuationOverview> {
  const response = await client.get(`/research/fundamental/valuation/${stockCode}`)
  return response.data
}

export async function getValuationHistory(stockCode: string): Promise<ValuationHistoryResponse> {
  const response = await client.get(`/research/fundamental/valuation-history/${stockCode}`)
  return response.data
}

export async function calculateDcf(stockCode: string, request: DcfRequest): Promise<DcfResponse> {
  const response = await client.post(`/research/fundamental/dcf/${stockCode}`, request)
  return response.data
}
