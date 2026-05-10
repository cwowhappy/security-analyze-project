import { http } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'
import type {
  CollectionTask,
  CollectionTaskSchedule,
  CreateCollectionTaskRequest,
  CreateCollectionTaskScheduleRequest,
} from '@/types/collection'

const TASK_PREFIX = '/collection/tasks'
const SCHEDULE_PREFIX = '/collection/schedules'

export const collectionTaskApi = {
  /** 分页获取任务列表 */
  list: (query: PageQuery, status?: string, taskType?: string) => {
    const params = new URLSearchParams()
    params.append('page', String(query.page))
    params.append('size', String(query.size))
    if (status) params.append('status', status)
    if (taskType) params.append('taskType', taskType)
    return http.get<PageResult<CollectionTask>>(`${TASK_PREFIX}?${params.toString()}`)
  },

  /** 获取任务详情 */
  getById: (id: string) => http.get<CollectionTask>(`${TASK_PREFIX}/${id}`),

  /** 创建即时采集任务 */
  create: (data: CreateCollectionTaskRequest) =>
    http.post<string>(TASK_PREFIX, data),
}

export const collectionScheduleApi = {
  /** 获取所有定时规则 */
  list: () => http.get<CollectionTaskSchedule[]>(SCHEDULE_PREFIX),

  /** 创建定时规则 */
  create: (data: CreateCollectionTaskScheduleRequest) =>
    http.post<string>(SCHEDULE_PREFIX, data),

  /** 更新定时规则 */
  update: (id: string, data: CreateCollectionTaskScheduleRequest) =>
    http.put<void>(`${SCHEDULE_PREFIX}/${id}`, data),

  /** 删除定时规则 */
  delete: (id: string) => http.delete<void>(`${SCHEDULE_PREFIX}/${id}`),
}
