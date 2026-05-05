import axios from 'axios'
import router from '@/router'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const client = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
})

client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')

      const isAdminPath = window.location.pathname.startsWith('/admin')
      const loginPath = isAdminPath ? '/admin/login' : '/login'
      router.replace(loginPath)
    }
    return Promise.reject(error)
  }
)
