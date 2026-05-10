/**
 * API 统一响应类型
 */
export interface ApiResponse<T> {
  success: boolean
  code: number
  message: string
  data: T
  timestamp: string
}

/**
 * 分页请求参数
 */
export interface PageQuery {
  page: number
  size: number
}

/**
 * 分页响应数据
 */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}
