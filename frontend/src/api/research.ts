import { client } from './axios'
import type {
  FundamentalOverview,
  ScreenParams,
  ScreenResponse,
  IndustryPeersResponse,
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
