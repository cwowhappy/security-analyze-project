import { client } from './axios'
import type {
  CollectorOverviewResponse,
  CollectorTaskListResponse,
  CollectorTaskParams,
} from '@/types/collector'

export async function getCollectorOverview(): Promise<CollectorOverviewResponse> {
  const response = await client.get('/collector/dashboard/overview')
  return response.data
}

export async function getCollectorTasks(
  params: CollectorTaskParams = {},
): Promise<CollectorTaskListResponse> {
  const response = await client.get('/collector/dashboard/tasks', { params })
  return response.data
}
