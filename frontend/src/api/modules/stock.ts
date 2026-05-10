import { http } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'
import type { Stock, StockDetail, CreateStockRequest } from '@/types/stock'

const PREFIX = '/stocks'

export const stockApi = {
  /** 获取股票列表（不分页，支持筛选） */
  list: (industry?: string, market?: string) => {
    const params = new URLSearchParams()
    if (industry) params.append('industry', industry)
    if (market) params.append('market', market)
    const query = params.toString()
    return http.get<Stock[]>(`${PREFIX}${query ? '?' + query : ''}`)
  },

  /** 分页获取股票列表 */
  page: (query: PageQuery, industry?: string, market?: string) => {
    const params = new URLSearchParams()
    params.append('page', String(query.page))
    params.append('size', String(query.size))
    if (industry) params.append('industry', industry)
    if (market) params.append('market', market)
    return http.get<PageResult<Stock>>(`${PREFIX}/page?${params.toString()}`)
  },

  /** 获取股票详情 */
  getByStockCode: (stockCode: string) =>
    http.get<StockDetail>(`${PREFIX}/${stockCode}`),

  /** 创建股票 */
  create: (data: CreateStockRequest) => http.post<string>(PREFIX, data),
}
