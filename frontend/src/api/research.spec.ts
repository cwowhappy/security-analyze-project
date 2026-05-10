import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  getFundamentalOverview,
  screenCompanies,
  getIndustryPeers,
} from './research'

vi.mock('./axios', () => ({
  client: {
    get: vi.fn(),
  },
}))

import { client } from './axios'

const mockedGet = vi.mocked(client.get)

describe('Research API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('getFundamentalOverview', () => {
    it('should fetch overview by stock code', async () => {
      mockedGet.mockResolvedValue({
        data: {
          stockCode: '600519',
          stockName: '贵州茅台',
          industry: '白酒',
          market: 'SH',
          metrics: [
            {
              reportDate: '2023-12-31',
              reportYear: 2023,
              totalRevenue: 150545774400,
              grossMargin: 87.92,
            },
          ],
        },
      })

      const result = await getFundamentalOverview('600519')

      expect(mockedGet).toHaveBeenCalledWith('/research/fundamental/overview/600519')
      expect(result.stockCode).toBe('600519')
      expect(result.metrics).toHaveLength(1)
      expect(result.metrics[0].reportYear).toBe(2023)
    })
  })

  describe('screenCompanies', () => {
    it('should screen companies with keyword', async () => {
      mockedGet.mockResolvedValue({
        data: {
          items: [
            { stockCode: '600519', stockName: '贵州茅台', industry: '白酒', market: 'SH' },
          ],
          total: 1,
          page: 0,
          size: 20,
        },
      })

      const result = await screenCompanies({ keyword: '茅台', page: 0, size: 20 })

      expect(mockedGet).toHaveBeenCalledWith('/research/fundamental/screen', {
        params: { keyword: '茅台', page: 0, size: 20 },
      })
      expect(result.items).toHaveLength(1)
      expect(result.items[0].stockCode).toBe('600519')
    })

    it('should screen companies with industry and market filters', async () => {
      mockedGet.mockResolvedValue({
        data: { items: [], total: 0, page: 0, size: 20 },
      })

      await screenCompanies({ industry: '白酒', market: 'SH' })

      expect(mockedGet).toHaveBeenCalledWith('/research/fundamental/screen', {
        params: { industry: '白酒', market: 'SH' },
      })
    })
  })

  describe('getIndustryPeers', () => {
    it('should fetch industry peers by stock code', async () => {
      mockedGet.mockResolvedValue({
        data: {
          peers: [
            { stockCode: '000001', stockName: '平安银行', industry: '银行', roe: 10.5 },
          ],
        },
      })

      const result = await getIndustryPeers('600000')

      expect(mockedGet).toHaveBeenCalledWith('/research/fundamental/industry-peers/600000')
      expect(result.peers).toHaveLength(1)
      expect(result.peers[0].stockCode).toBe('000001')
    })
  })
})
