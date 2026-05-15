import { http } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'
import type { Company, CompanyDetail, CreateCompanyRequest } from '@/types/company'

const PREFIX = '/api/v1/companies'

export const companyApi = {
  /** 分页获取公司列表（支持筛选） */
  list: (query: PageQuery, industry?: string, province?: string, keyword?: string, controllerType?: string) => {
    const params = new URLSearchParams()
    params.append('page', String(query.page))
    params.append('size', String(query.size))
    if (industry) params.append('industry', industry)
    if (province) params.append('province', province)
    if (keyword) params.append('keyword', keyword)
    if (controllerType) params.append('controllerType', controllerType)
    return http.get<PageResult<Company>>(`${PREFIX}?${params.toString()}`)
  },

  /** 获取公司详情 */
  getById: (id: string) =>
    http.get<CompanyDetail>(`${PREFIX}/${id}`),

  /** 创建公司 */
  create: (data: CreateCompanyRequest) => http.post<string>(PREFIX, data),
}
