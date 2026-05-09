import { client } from './axios'
import type {
  FundamentalOverview,
  ScreenParams,
  ScreenResponse,
  IndustryPeersResponse,
  IndustryRankResponse,
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
