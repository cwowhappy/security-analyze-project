import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useStockStore } from '../stock'

const mockPage = vi.fn()
const mockGetByStockCode = vi.fn()

vi.mock('@/api/modules/stock', () => ({
  stockApi: {
    page: (...args: unknown[]) => mockPage(...args),
    getByStockCode: (...args: unknown[]) => mockGetByStockCode(...args),
  },
}))

describe('stock store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockPage.mockClear()
    mockGetByStockCode.mockClear()
  })

  it('should have initial state', () => {
    const store = useStockStore()
    expect(store.stocks).toEqual([])
    expect(store.stockTotal).toBe(0)
    expect(store.currentStock).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.stockCount).toBe(0)
  })

  it('fetchStockPage should update stocks and total', async () => {
    const store = useStockStore()
    mockPage.mockResolvedValue({ list: [{ stockCode: '600001', name: 'Test' }], total: 1 })

    await store.fetchStockPage({ page: 1, size: 20 })

    expect(store.stocks).toHaveLength(1)
    expect(store.stockTotal).toBe(1)
    expect(store.loading).toBe(false)
  })

  it('fetchStockPage should set loading during request', async () => {
    const store = useStockStore()
    let resolveFn: (value: unknown) => void
    mockPage.mockImplementation(() => new Promise((resolve) => { resolveFn = resolve }))

    const promise = store.fetchStockPage({ page: 1, size: 20 })
    expect(store.loading).toBe(true)

    resolveFn!({ list: [], total: 0 })
    await promise
    expect(store.loading).toBe(false)
  })

  it('fetchStockDetail should update currentStock', async () => {
    const store = useStockStore()
    mockGetByStockCode.mockResolvedValue({ stockCode: '600001', name: 'Test Stock' })

    await store.fetchStockDetail('600001')

    expect(store.currentStock).toEqual({ stockCode: '600001', name: 'Test Stock' })
    expect(store.loading).toBe(false)
  })

  it('getStockByCode should find stock in list', () => {
    const store = useStockStore()
    store.stocks = [
      { stockCode: '600001', name: 'A' },
      { stockCode: '000001', name: 'B' },
    ] as any

    expect(store.getStockByCode('600001')?.name).toBe('A')
    expect(store.getStockByCode('999999')).toBeUndefined()
  })
})
