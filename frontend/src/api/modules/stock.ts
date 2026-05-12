import { http } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'
import type { Stock, StockDetail, CreateStockRequest } from '@/types/stock'

const PREFIX = '/api/v1/stocks'

export const stockApi = {
  /** 分页获取股票列表（支持筛选） */
  page: (query: PageQuery, market?: string, industry?: string, area?: string, keyword?: string) => {
    const params = new URLSearchParams()
    params.append('page', String(query.page))
    params.append('size', String(query.size))
    if (market) params.append('market', market)
    if (industry) params.append('industry', industry)
    if (area) params.append('area', area)
    if (keyword) params.append('keyword', keyword)
    return http.get<PageResult<Stock>>(`${PREFIX}?${params.toString()}`)
  },

  /** 获取股票详情 */
  getByStockCode: (stockCode: string) =>
    http.get<StockDetail>(`${PREFIX}/${stockCode}`),

  /** 创建股票 */
  create: (data: CreateStockRequest) => http.post<string>(PREFIX, data),
}
