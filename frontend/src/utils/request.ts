/**
 * HTTP 请求工具封装
 * 基于 fetch，统一处理请求/响应格式
 */

import type { ApiResponse } from '@/types/api'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

const TOKEN_KEY = 'sai_token'

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options?.headers as Record<string, string>) || {}),
  }

  const token = sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(fullUrl, {
    ...options,
    headers,
  })

  if (response.status === 401) {
    localStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(TOKEN_KEY)
    window.location.href = '/login'
    throw new Error('登录已过期，请重新登录')
  }

  if (!response.ok) {
    const errorResult: ApiResponse<unknown> | null = await response.json().catch(() => null)
    const errorMessage = errorResult?.message || `HTTP error! status: ${response.status}`
    throw new Error(errorMessage)
  }

  const result: ApiResponse<T> = await response.json()

  if (!result.success && result.code !== 0) {
    throw new Error(result.message || '请求失败')
  }

  return result.data
}

export const http = {
  get: <T>(url: string, params?: Record<string, string | number | boolean | undefined>) => {
    let fullUrl = url
    if (params) {
      const searchParams = new URLSearchParams()
      for (const [key, value] of Object.entries(params)) {
        if (value !== undefined && value !== null) {
          searchParams.append(key, String(value))
        }
      }
      const queryString = searchParams.toString()
      if (queryString) {
        fullUrl = `${url}${url.includes('?') ? '&' : '?'}${queryString}`
      }
    }
    return request<T>(fullUrl, { method: 'GET' })
  },
  post: <T>(url: string, body: unknown) =>
    request<T>(url, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(url: string, body: unknown) =>
    request<T>(url, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T>(url: string) => request<T>(url, { method: 'DELETE' }),
}
