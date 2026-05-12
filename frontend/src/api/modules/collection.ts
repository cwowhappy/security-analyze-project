import { http } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'
import type { CollectionTask, CreateCollectionTaskRequest } from '@/types/collection'

const PREFIX = '/api/v1/collection/tasks'

export const collectionTaskApi = {
  /** 分页获取任务列表 */
  list: (query: PageQuery, status?: string, taskType?: string) => {
    const params = new URLSearchParams()
    params.append('page', String(query.page))
    params.append('size', String(query.size))
    if (status) params.append('status', status)
    if (taskType) params.append('taskType', taskType)
    return http.get<PageResult<CollectionTask>>(`${PREFIX}?${params.toString()}`)
  },

  /** 获取任务详情 */
  getById: (id: string) => http.get<CollectionTask>(`${PREFIX}/${id}`),

  /** 创建即时采集任务 */
  create: (data: CreateCollectionTaskRequest) =>
    http.post<string>(PREFIX, data),
}
