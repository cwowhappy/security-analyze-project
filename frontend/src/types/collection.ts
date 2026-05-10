/**
 * 采集任务领域类型定义
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
  scheduledAt: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
}

export interface CreateCollectionTaskRequest {
  taskType: string
  taskParams?: Record<string, unknown>
  dataSource?: string
}

export interface CollectionTaskSchedule {
  id: string
  name: string
  taskType: string
  taskParams: Record<string, unknown> | null
  dataSource: string | null
  cronExpression: string
  isEnabled: boolean
  lastTriggeredAt: string | null
  createdAt: string
}

export interface CreateCollectionTaskScheduleRequest {
  name: string
  taskType: string
  cronExpression: string
  dataSource?: string
  taskParams?: Record<string, unknown>
  isEnabled?: boolean
}
