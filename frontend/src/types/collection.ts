/**
 * 采集任务领域类型定义 v2.0
 */

export interface CollectionTask {
  id: string
  taskType: string
  taskParams: Record<string, unknown> | null
  status: 'pending' | 'running' | 'success' | 'failed'
  dataSource: string | null
  totalCount: number
  successCount: number
  failCount: number
  errorMessage: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
}

export interface CreateCollectionTaskRequest {
  taskType: string
  taskParams?: Record<string, unknown>
  dataSource?: string
}
