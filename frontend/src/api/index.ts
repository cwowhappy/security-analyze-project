import { client } from './axios'
import type {
  IndexListResponse,
  IndexDetailResponse,
  IndexTrendResponse,
  EtfListItem,
  IndexCategoryGroup,
} from '@/types/index'

export async function getIndexList(
  keyword?: string,
  page: number = 0,
  size: number = 20,
): Promise<IndexListResponse> {
  const params: Record<string, any> = { page, size }
  if (keyword) params.keyword = keyword
  const response = await client.get('/indexes', { params })
  return response.data
}

export async function getIndexDetail(indexCode: string): Promise<IndexDetailResponse> {
  const response = await client.get(`/indexes/${encodeURIComponent(indexCode)}`)
  return response.data
}

export async function getIndexTrend(
  indexCode: string,
  granularity: string = 'day',
  startDate?: string,
  endDate?: string,
): Promise<IndexTrendResponse> {
  const params: Record<string, any> = { granularity }
  if (startDate) params.startDate = startDate
  if (endDate) params.endDate = endDate
  const response = await client.get(`/indexes/${encodeURIComponent(indexCode)}/trend`, { params })
  return response.data
}

export async function getIndexEtfs(indexCode: string): Promise<EtfListItem[]> {
  const response = await client.get(`/indexes/${encodeURIComponent(indexCode)}/etfs`)
  return response.data
}

export async function getIndexCategories(): Promise<IndexCategoryGroup[]> {
  const response = await client.get('/indexes/categories')
  return response.data
}
