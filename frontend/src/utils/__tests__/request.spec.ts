import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { http } from '../request'

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

describe('request', () => {
  let fetchMock: ReturnType<typeof vi.fn>
  let mockSessionStorage: Storage
  let mockLocalStorage: Storage

  beforeEach(() => {
    fetchMock = vi.fn()
    mockSessionStorage = createMockStorage()
    mockLocalStorage = createMockStorage()
    vi.stubGlobal('fetch', fetchMock)
    vi.stubGlobal('window', { location: { href: '' } })
    vi.stubGlobal('sessionStorage', mockSessionStorage)
    vi.stubGlobal('localStorage', mockLocalStorage)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('should attach Authorization header when token exists in sessionStorage', async () => {
    mockSessionStorage.setItem('sai_token', 'session-token')
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: true, code: 0, data: 'ok' }),
    })

    await http.get('/test')

    expect(fetchMock).toHaveBeenCalledOnce()
    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.headers).toMatchObject({ Authorization: 'Bearer session-token' })
  })

  it('should attach Authorization header when token exists in localStorage', async () => {
    mockLocalStorage.setItem('sai_token', 'local-token')
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: true, code: 0, data: 'ok' }),
    })

    await http.get('/test')

    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.headers).toMatchObject({ Authorization: 'Bearer local-token' })
  })

  it('should not attach Authorization header when no token', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: true, code: 0, data: 'ok' }),
    })

    await http.get('/test')

    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect((init.headers as Record<string, string>)['Authorization']).toBeUndefined()
  })

  it('should throw and redirect on 401', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: false,
      status: 401,
      json: async () => null,
    })

    await expect(http.get('/test')).rejects.toThrow('登录已过期，请重新登录')
    expect(mockLocalStorage.getItem('sai_token')).toBeNull()
    expect(mockSessionStorage.getItem('sai_token')).toBeNull()
  })

  it('should throw with server error message when response not ok', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: false,
      status: 500,
      json: async () => ({ success: false, message: '服务器内部错误' }),
    })

    await expect(http.get('/test')).rejects.toThrow('服务器内部错误')
  })

  it('should throw with HTTP status when error body is empty', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: false,
      status: 502,
      json: async () => null,
    })

    await expect(http.get('/test')).rejects.toThrow('HTTP error! status: 502')
  })

  it('should throw when api returns success=false and code != 0', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: false, code: 40001, message: '参数错误' }),
    })

    await expect(http.get('/test')).rejects.toThrow('参数错误')
  })

  it('should return data on successful response', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: true, code: 0, data: { id: '1' } }),
    })

    const result = await http.get<{ id: string }>('/test')
    expect(result).toEqual({ id: '1' })
  })

  it('should build query string for GET with params', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: true, code: 0, data: null }),
    })

    await http.get('/test', { page: 1, keyword: 'foo', empty: undefined })

    const url = fetchMock.mock.calls[0][0] as string
    expect(url).toContain('page=1')
    expect(url).toContain('keyword=foo')
    expect(url).not.toContain('empty')
  })

  it('should append to existing query string', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: true, code: 0, data: null }),
    })

    await http.get('/test?existing=1', { page: 2 })

    const url = fetchMock.mock.calls[0][0] as string
    expect(url).toContain('existing=1')
    expect(url).toContain('&page=2')
  })

  it('should send POST with JSON body', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: true, code: 0, data: { id: '1' } }),
    })

    await http.post('/test', { name: 'foo' })

    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.method).toBe('POST')
    expect(init.body).toBe(JSON.stringify({ name: 'foo' }))
  })

  it('should send PUT with JSON body', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: true, code: 0, data: null }),
    })

    await http.put('/test/1', { name: 'bar' })

    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.method).toBe('PUT')
  })

  it('should send DELETE request', async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => ({ success: true, code: 0, data: null }),
    })

    await http.delete('/test/1')

    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.method).toBe('DELETE')
  })
})
