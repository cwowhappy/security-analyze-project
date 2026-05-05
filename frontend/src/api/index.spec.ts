import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  getIndexList,
  getIndexDetail,
  getIndexTrend,
  getIndexEtfs,
  getIndexCategories,
} from './index'

// Mock the axios client
vi.mock('./axios', () => ({
  client: {
    get: vi.fn(),
  },
}))

import { client } from './axios'

const mockedGet = vi.mocked(client.get)

describe('Index API', () => {
  beforeEach(() => {
    mockedGet.mockClear()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('getIndexList', () => {
    it('should fetch index list with default pagination', async () => {
      const mockResponse = {
        data: {
          items: [{ indexCode: '000001', indexName: '上证指数' }],
          total: 1,
          page: 0,
          size: 20,
        },
      }
      mockedGet.mockResolvedValue(mockResponse)

      const result = await getIndexList()

      expect(mockedGet).toHaveBeenCalledWith('/indexes', { params: { page: 0, size: 20 } })
      expect(result.items).toHaveLength(1)
      expect(result.items[0].indexCode).toBe('000001')
    })

    it('should fetch index list with keyword', async () => {
      const mockResponse = {
        data: {
          items: [{ indexCode: '000300', indexName: '沪深300' }],
          total: 1,
          page: 0,
          size: 20,
        },
      }
      mockedGet.mockResolvedValue(mockResponse)

      const result = await getIndexList('沪深', 0, 20)

      expect(mockedGet).toHaveBeenCalledWith('/indexes', {
        params: { page: 0, size: 20, keyword: '沪深' },
      })
      expect(result.items[0].indexCode).toBe('000300')
    })
  })

  describe('getIndexDetail', () => {
    it('should fetch index detail by code', async () => {
      const mockResponse = {
        data: {
          indexCode: '000001',
          indexName: '上证指数',
          indexType: '宽基',
        },
      }
      mockedGet.mockResolvedValue(mockResponse)

      const result = await getIndexDetail('000001')

      expect(mockedGet).toHaveBeenCalledWith('/indexes/000001')
      expect(result.indexCode).toBe('000001')
    })

    it('should encode special characters in index code', async () => {
      mockedGet.mockResolvedValue({ data: {} })

      await getIndexDetail('H30035')

      expect(mockedGet).toHaveBeenCalledWith('/indexes/H30035')
    })
  })

  describe('getIndexTrend', () => {
    it('should fetch trend with default granularity', async () => {
      const mockResponse = {
        data: {
          indexCode: '000001',
          granularity: 'day',
          items: [{ tradeDate: '2024-01-01', closePrice: 3000 }],
        },
      }
      mockedGet.mockResolvedValue(mockResponse)

      const result = await getIndexTrend('000001')

      expect(mockedGet).toHaveBeenCalledWith('/indexes/000001/trend', {
        params: { granularity: 'day' },
      })
      expect(result.granularity).toBe('day')
    })

    it('should fetch trend with date range', async () => {
      const mockResponse = {
        data: {
          indexCode: '000001',
          granularity: 'week',
          items: [],
        },
      }
      mockedGet.mockResolvedValue(mockResponse)

      const result = await getIndexTrend('000001', 'week', '2024-01-01', '2024-01-31')

      expect(mockedGet).toHaveBeenCalledWith('/indexes/000001/trend', {
        params: { granularity: 'week', startDate: '2024-01-01', endDate: '2024-01-31' },
      })
    })
  })

  describe('getIndexEtfs', () => {
    it('should fetch ETF list by index code', async () => {
      const mockResponse = {
        data: [
          { etfCode: '510300', etfName: '华泰柏瑞沪深300ETF' },
        ],
      }
      mockedGet.mockResolvedValue(mockResponse)

      const result = await getIndexEtfs('000300')

      expect(mockedGet).toHaveBeenCalledWith('/indexes/000300/etfs')
      expect(result).toHaveLength(1)
      expect(result[0].etfCode).toBe('510300')
    })
  })

  describe('getIndexCategories', () => {
    it('should fetch index categories', async () => {
      const mockResponse = {
        data: [
          {
            indexType: '宽基',
            indexTypeLabel: '宽基指数',
            items: [{ indexCode: '000001', indexName: '上证指数' }],
          },
        ],
      }
      mockedGet.mockResolvedValue(mockResponse)

      const result = await getIndexCategories()

      expect(mockedGet).toHaveBeenCalledWith('/indexes/categories')
      expect(result).toHaveLength(1)
      expect(result[0].indexType).toBe('宽基')
    })
  })
})
