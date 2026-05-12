import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useCollectionStore } from '../collection'

const mockList = vi.fn()
const mockGetById = vi.fn()
const mockCreate = vi.fn()

vi.mock('@/api/modules/collection', () => ({
  collectionTaskApi: {
    list: (...args: unknown[]) => mockList(...args),
    getById: (...args: unknown[]) => mockGetById(...args),
    create: (...args: unknown[]) => mockCreate(...args),
  },
}))

describe('collection store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockList.mockClear()
    mockGetById.mockClear()
    mockCreate.mockClear()
  })

  it('should have initial state', () => {
    const store = useCollectionStore()
    expect(store.tasks).toEqual([])
    expect(store.taskTotal).toBe(0)
    expect(store.currentTask).toBeNull()
    expect(store.loading).toBe(false)
  })

  it('fetchTasks should update state', async () => {
    const store = useCollectionStore()
    mockList.mockResolvedValue({ list: [{ id: 't1' }], total: 1 })

    await store.fetchTasks({ page: 1, size: 20 }, 'running')

    expect(store.tasks).toHaveLength(1)
    expect(store.taskTotal).toBe(1)
    expect(mockList).toHaveBeenCalledWith({ page: 1, size: 20 }, 'running', undefined)
  })

  it('fetchTaskDetail should update currentTask', async () => {
    const store = useCollectionStore()
    mockGetById.mockResolvedValue({ id: 't1', status: 'completed' })

    await store.fetchTaskDetail('t1')

    expect(store.currentTask).toEqual({ id: 't1', status: 'completed' })
  })

  it('createTask should call api with correct params', async () => {
    const store = useCollectionStore()
    mockCreate.mockResolvedValue('new-task-id')

    const result = await store.createTask('stock_daily', 'eastmoney', { startDate: '2026-01-01' })

    expect(result).toBe('new-task-id')
    expect(mockCreate).toHaveBeenCalledWith({
      taskType: 'stock_daily',
      dataSource: 'eastmoney',
      taskParams: { startDate: '2026-01-01' },
    })
  })
})
