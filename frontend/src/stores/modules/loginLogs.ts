import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/utils/request'

export interface LoginLog {
  id: number
  userId: string
  username: string
  action: string
  ip: string
  userAgent: string
  details?: string
  timestamp: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export const useLoginLogsStore = defineStore('loginLogs', () => {
  const logs = ref<LoginLog[]>([])
  const total = ref(0)
  const loading = ref(false)

  async function fetchLogs(params: {
    page?: number
    size?: number
    userId?: string
    action?: string
    startDate?: string
    endDate?: string
  }) {
    loading.value = true
    try {
      const data = await http.get<PageResult<LoginLog>>('/api/v1/admin/login-logs', params)
      logs.value = data.list
      total.value = data.total
      return data
    } finally {
      loading.value = false
    }
  }

  async function exportCSV(params: {
    userId?: string
    action?: string
    startDate?: string
    endDate?: string
  }) {
    const searchParams = new URLSearchParams()
    for (const [key, value] of Object.entries(params)) {
      if (value) searchParams.append(key, value)
    }
    const url = `/api/v1/admin/login-logs/export?${searchParams.toString()}`

    const token = localStorage.getItem('sai_token') || sessionStorage.getItem('sai_token')
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
    })

    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = `login-logs-${new Date().toISOString().split('T')[0]}.csv`
    link.click()
    URL.revokeObjectURL(downloadUrl)
  }

  return {
    logs,
    total,
    loading,
    fetchLogs,
    exportCSV,
  }
})
