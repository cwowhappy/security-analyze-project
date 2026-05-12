import { describe, it, expect, vi, beforeEach } from 'vitest'
import { collectionTaskApi } from '../modules/collection'

const mockGet = vi.fn()
const mockPost = vi.fn()

vi.mock('@/utils/request', () => ({
  http: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
  },
}))

describe('collectionTaskApi', () => {
  beforeEach(() => {
    mockGet.mockClear()
    mockPost.mockClear()
  })

  it('list should build query with page and size', async () => {
    mockGet.mockResolvedValue({ list: [], total: 0 })
    await collectionTaskApi.list({ page: 1, size: 20 })
    expect(mockGet).toHaveBeenCalledWith('/api/v1/collection/tasks?page=1&size=20')
  })

  it('list should append status and taskType when provided', async () => {
    mockGet.mockResolvedValue({ list: [], total: 0 })
    await collectionTaskApi.list({ page: 2, size: 10 }, 'running', 'stock_daily')
    const url = mockGet.mock.calls[0][0] as string
    expect(url).toContain('status=running')
    expect(url).toContain('taskType=stock_daily')
  })

  it('getById should call correct endpoint', async () => {
    mockGet.mockResolvedValue({ id: 'task1' })
    await collectionTaskApi.getById('task1')
    expect(mockGet).toHaveBeenCalledWith('/api/v1/collection/tasks/task1')
  })

  it('create should post data', async () => {
    mockPost.mockResolvedValue('task-id')
    const data = { taskType: 'stock_daily', taskParams: {} }
    await collectionTaskApi.create(data as any)
    expect(mockPost).toHaveBeenCalledWith('/api/v1/collection/tasks', data)
  })
})
