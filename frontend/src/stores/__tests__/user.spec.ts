import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../user'

describe('User Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('should set token and persist to localStorage', () => {
    const store = useUserStore()
    store.setToken('test-token')

    expect(store.token).toBe('test-token')
    expect(localStorage.getItem('token')).toBe('test-token')
  })

  it('should clear token and remove from localStorage', () => {
    const store = useUserStore()
    store.setToken('test-token')
    store.clearToken()

    expect(store.token).toBe('')
    expect(localStorage.getItem('token')).toBeNull()
  })
})
