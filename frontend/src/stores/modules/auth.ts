import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { http } from '@/utils/request'

export interface UserInfo {
  id: string
  username: string
  email: string
  role: string
  displayName: string
  avatarInitial: string
}

export interface LoginCredentials {
  username: string
  password: string
  rememberMe: boolean
}

export interface RegisterData {
  username: string
  email: string
  password: string
  confirmPassword: string
  role: string
}

const TOKEN_KEY = 'sai_token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<UserInfo | null>(null)
  const rememberMe = ref(false)

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const avatarInitial = computed(() => user.value?.avatarInitial || '?')
  const displayName = computed(() => user.value?.displayName || user.value?.username || '访客')
  const roleLabel = computed(() => {
    const map: Record<string, string> = {
      portfolio_manager: '投资组合经理',
      analyst: '分析师',
      viewer: '观察者',
    }
    return map[user.value?.role || ''] || user.value?.role || ''
  })

  function initAuth() {
    const savedToken = sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)
    if (savedToken) {
      token.value = savedToken
      fetchCurrentUser().catch(() => {
        clearAuth()
      })
    }
  }

  function setToken(newToken: string, remember: boolean) {
    token.value = newToken
    rememberMe.value = remember
    if (remember) {
      localStorage.setItem(TOKEN_KEY, newToken)
    } else {
      sessionStorage.setItem(TOKEN_KEY, newToken)
    }
  }

  function clearAuth() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(TOKEN_KEY)
  }

  async function login(credentials: LoginCredentials) {
    const data = await http.post<{
      accessToken: string
      tokenType: string
      expiresIn: number
      user: UserInfo
    }>('/api/v1/auth/login', {
      username: credentials.username,
      password: credentials.password,
      rememberMe: credentials.rememberMe,
    })
    setToken(data.accessToken, credentials.rememberMe)
    user.value = data.user
    return data
  }

  async function register(data: RegisterData) {
    const result = await http.post<UserInfo>('/api/v1/auth/register', {
      username: data.username,
      email: data.email,
      password: data.password,
      confirmPassword: data.confirmPassword,
      role: data.role,
    })
    return result
  }

  async function verifyEmail(userId: string, code: string) {
    await http.post<null>('/api/v1/auth/verify-email', { userId, code })
  }

  async function resendVerification(userId: string) {
    await http.post<null>(`/api/v1/auth/resend-verification?userId=${encodeURIComponent(userId)}`, {})
  }

  async function fetchCurrentUser() {
    if (!token.value) return
    const data = await http.get<UserInfo>('/api/v1/auth/me')
    user.value = data
    return data
  }

  async function logout() {
    try {
      await http.post<null>('/api/v1/auth/logout', {})
    } catch {
      // ignore
    }
    clearAuth()
  }

  async function checkUsername(username: string): Promise<boolean> {
    const data = await http.get<{ available: boolean }>('/api/v1/auth/check-username', { username })
    return data.available
  }

  async function checkEmail(email: string): Promise<boolean> {
    const data = await http.get<{ available: boolean }>('/api/v1/auth/check-email', { email })
    return data.available
  }

  async function verifyResetToken(token: string): Promise<{ userId: string; email: string }> {
    return http.get<{ userId: string; email: string }>('/api/v1/auth/verify-reset-token', { token })
  }

  async function resetPassword(token: string, newPassword: string, confirmPassword: string): Promise<void> {
    await http.post<null>('/api/v1/auth/reset-password', {
      token,
      newPassword,
      confirmPassword,
    })
  }

  return {
    token,
    user,
    rememberMe,
    isLoggedIn,
    avatarInitial,
    displayName,
    roleLabel,
    initAuth,
    login,
    register,
    verifyEmail,
    resendVerification,
    fetchCurrentUser,
    logout,
    clearAuth,
    checkUsername,
    checkEmail,
    verifyResetToken,
    resetPassword,
  }
})
