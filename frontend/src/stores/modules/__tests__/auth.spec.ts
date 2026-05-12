import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../auth'

function createMockStorage() {
  const store = new Map<string, string>()
  return {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => store.set(key, value),
    removeItem: (key: string) => store.delete(key),
    clear: () => store.clear(),
    length: 0,
    key: () => null,
  } as Storage
}

describe('auth store', () => {
  let mockSessionStorage: Storage
  let mockLocalStorage: Storage

  beforeEach(() => {
    setActivePinia(createPinia())
    mockSessionStorage = createMockStorage()
    mockLocalStorage = createMockStorage()
    vi.stubGlobal('window', { location: { href: '' } })
    vi.stubGlobal('sessionStorage', mockSessionStorage)
    vi.stubGlobal('localStorage', mockLocalStorage)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('should have initial state', () => {
    const store = useAuthStore()
    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(store.avatarInitial).toBe('?')
    expect(store.displayName).toBe('访客')
  })

  it('should compute roleLabel correctly', () => {
    const store = useAuthStore()
    store.user = { id: '1', username: 'test', email: '', role: 'portfolio_manager', displayName: '', avatarInitial: '' }
    expect(store.roleLabel).toBe('投资组合经理')

    store.user = { ...store.user!, role: 'analyst' }
    expect(store.roleLabel).toBe('分析师')

    store.user = { ...store.user!, role: 'viewer' }
    expect(store.roleLabel).toBe('观察者')

    store.user = { ...store.user!, role: 'unknown' }
    expect(store.roleLabel).toBe('unknown')
  })

  it('should compute displayName from displayName or username', () => {
    const store = useAuthStore()
    expect(store.displayName).toBe('访客')

    store.user = { id: '1', username: 'testuser', email: '', role: '', displayName: '', avatarInitial: '' }
    expect(store.displayName).toBe('testuser')

    store.user = { ...store.user!, displayName: 'Test User' }
    expect(store.displayName).toBe('Test User')
  })

  it('should compute isLoggedIn when both token and user exist', () => {
    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(false)

    store.token = 'token-123'
    expect(store.isLoggedIn).toBe(false)

    store.user = { id: '1', username: 'test', email: '', role: '', displayName: '', avatarInitial: '' }
    expect(store.isLoggedIn).toBe(true)
  })

  it('clearAuth should reset token, user and storage', () => {
    const store = useAuthStore()
    store.token = 'token-123'
    store.user = { id: '1', username: 'test', email: '', role: 'viewer', displayName: '', avatarInitial: '' }
    mockLocalStorage.setItem('sai_token', 'token-123')
    mockSessionStorage.setItem('sai_token', 'token-123')

    store.clearAuth()

    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
    expect(mockLocalStorage.getItem('sai_token')).toBeNull()
    expect(mockSessionStorage.getItem('sai_token')).toBeNull()
  })

  it('initAuth should restore token from sessionStorage', () => {
    mockSessionStorage.setItem('sai_token', 'saved-token')
    const store = useAuthStore()
    store.initAuth()
    expect(store.token).toBe('saved-token')
  })

  it('initAuth should restore token from localStorage', () => {
    mockLocalStorage.setItem('sai_token', 'local-token')
    const store = useAuthStore()
    store.initAuth()
    expect(store.token).toBe('local-token')
  })

  it('initAuth should keep state unchanged when no token found', () => {
    const store = useAuthStore()
    store.token = 'old-token'
    store.initAuth()
    // initAuth does not clear auth when token is missing; it simply does nothing
    expect(store.token).toBe('old-token')
  })
})
