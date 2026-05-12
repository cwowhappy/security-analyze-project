import { defineStore } from 'pinia'
import { ref } from 'vue'
import { http } from '@/utils/request'

export interface User {
  id: string
  username: string
  email: string
  displayName: string
  role: string
  avatarInitial: string
  emailVerified: boolean
  locked: boolean
  lastLoginAt: string
  createdAt: string
}

export interface UserDetail extends User {
  lockedUntil: string | null
  failedLoginAttempts: number
  updatedAt: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

export const useAdminStore = defineStore('admin', () => {
  const users = ref<User[]>([])
  const currentUser = ref<UserDetail | null>(null)
  const total = ref(0)
  const loading = ref(false)

  async function fetchUsers(params: {
    page?: number
    size?: number
    role?: string
    keyword?: string
    emailVerified?: boolean
    locked?: boolean
  }) {
    loading.value = true
    try {
      const data = await http.get<PageResult<User>>('/api/v1/admin/users', params)
      users.value = data.list
      total.value = data.total
      return data
    } finally {
      loading.value = false
    }
  }

  async function fetchUserDetail(userId: string) {
    loading.value = true
    try {
      const data = await http.get<UserDetail>(`/api/v1/admin/users/${userId}`)
      currentUser.value = data
      return data
    } finally {
      loading.value = false
    }
  }

  async function updateUser(userId: string, data: { displayName?: string; role?: string }) {
    await http.put<void>(`/api/v1/admin/users/${userId}`, data)
    if (currentUser.value?.id === userId) {
      await fetchUserDetail(userId)
    }
  }

  async function unlockUser(userId: string) {
    await http.post<void>(`/api/v1/admin/users/${userId}/unlock`, {})
    if (currentUser.value?.id === userId) {
      await fetchUserDetail(userId)
    }
  }

  async function forcePasswordReset(userId: string, reason?: string) {
    await http.post<void>(`/api/v1/admin/users/${userId}/force-password-reset`, { reason })
  }

  return {
    users,
    currentUser,
    total,
    loading,
    fetchUsers,
    fetchUserDetail,
    updateUser,
    unlockUser,
    forcePasswordReset,
  }
})
