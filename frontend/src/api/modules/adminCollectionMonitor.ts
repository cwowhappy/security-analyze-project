import { http } from '@/utils/request'
import type { CollectionMonitorOverview, CollectionMonitorBaseline } from '@/types/monitor'

const PREFIX = '/api/v1/admin/collection/monitor'

export const adminCollectionMonitorApi = {
  /** 查询采集覆盖度概览 */
  getOverview: () => http.get<CollectionMonitorOverview[]>(`${PREFIX}/overview`),

  /** 查询数据基线 */
  getBaseline: () => http.get<CollectionMonitorBaseline>(`${PREFIX}/baseline`),
}
