import axios from 'axios'
import type {
  CollectorOverviewResponse,
  CollectorTaskListResponse,
  CollectorTaskParams,
} from '@/types/collector'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const client = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
})

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
