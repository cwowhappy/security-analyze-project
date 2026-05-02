export interface CollectorOverviewItem {
  dataType: string
  dataTypeLabel: string
  totalRows: number
  lastUpdatedAt: string
  lastTaskStatus: string
  lastTaskDurationSeconds: number
}

export interface CollectorOverviewResponse {
  data: CollectorOverviewItem[]
}

export interface CollectorTaskItem {
  id: number
  taskName: string
  taskType: string
  startedAt: string
  endedAt: string
  status: string
  rowsAffected: number
  durationSeconds: number
}

export interface CollectorTaskListResponse {
  data: CollectorTaskItem[]
  total: number
  page: number
  size: number
}

export interface CollectorTaskParams {
  dataType?: string
  status?: string
  page?: number
  size?: number
}

export const DATA_TYPE_LABELS: Record<string, string> = {
  company: '公司基本信息',
  security: '上市证券信息',
  finance_report: '财务报告',
}

export const STATUS_LABELS: Record<string, string> = {
  success: '成功',
  failed: '失败',
  running: '运行中',
}
