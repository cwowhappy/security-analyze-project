import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')

  const setToken = (value: string) => {
    token.value = value
    localStorage.setItem('token', value)
  }

  const clearToken = () => {
    token.value = ''
    localStorage.removeItem('token')
  }

  return { token, setToken, clearToken }
})
