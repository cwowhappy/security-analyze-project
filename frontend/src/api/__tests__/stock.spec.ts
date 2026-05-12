import { describe, it, expect, vi, beforeEach } from 'vitest'
import { stockApi } from '../modules/stock'

const mockGet = vi.fn()
const mockPost = vi.fn()

vi.mock('@/utils/request', () => ({
  http: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
  },
}))

describe('stockApi', () => {
  beforeEach(() => {
    mockGet.mockClear()
    mockPost.mockClear()
  })

  it('page should build query with filters', async () => {
    mockGet.mockResolvedValue({ list: [], total: 0 })
    await stockApi.page({ page: 1, size: 20 }, 'SH', '银行', '北京', '招行')
    const url = mockGet.mock.calls[0][0] as string
    expect(url).toContain('page=1')
    expect(url).toContain('size=20')
    expect(url).toContain('market=SH')
    expect(url).toContain('industry=%E9%93%B6%E8%A1%8C')
    expect(url).toContain('area=%E5%8C%97%E4%BA%AC')
    expect(url).toContain('keyword=%E6%8B%9B%E8%A1%8C')
  })

  it('getByStockCode should call correct endpoint', async () => {
    mockGet.mockResolvedValue({ stockCode: '600001' })
    await stockApi.getByStockCode('600001')
    expect(mockGet).toHaveBeenCalledWith('/api/v1/stocks/600001')
  })

  it('create should post data', async () => {
    mockPost.mockResolvedValue('stock-id')
    const data = { stockCode: '600001', name: 'Test' }
    await stockApi.create(data as any)
    expect(mockPost).toHaveBeenCalledWith('/api/v1/stocks', data)
  })
})
