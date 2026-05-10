/**
 * HTTP 请求工具封装
 * 基于 fetch，统一处理请求/响应格式
 */

import type { ApiResponse } from '@/types/api'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options?.headers as Record<string, string>) || {}),
  }

  const token = localStorage.getItem('token')
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(fullUrl, {
    ...options,
    headers,
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  const result: ApiResponse<T> = await response.json()

  if (!result.success) {
    throw new Error(result.message || '请求失败')
  }

  return result.data
}

export const http = {
  get: <T>(url: string) => request<T>(url, { method: 'GET' }),
  post: <T>(url: string, body: unknown) =>
    request<T>(url, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(url: string, body: unknown) =>
    request<T>(url, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T>(url: string) => request<T>(url, { method: 'DELETE' }),
}
