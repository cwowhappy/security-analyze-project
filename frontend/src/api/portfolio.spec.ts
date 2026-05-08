import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  getPortfolios,
  createPortfolio,
  updatePortfolio,
  deletePortfolio,
  getTransactions,
  createTransaction,
  updateTransaction,
  deleteTransaction,
  getPositions,
  getPortfolioSummary,
} from './portfolio'

vi.mock('./axios', () => ({
  client: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

import { client } from './axios'

const mockedGet = vi.mocked(client.get)
const mockedPost = vi.mocked(client.post)
const mockedPut = vi.mocked(client.put)
const mockedDelete = vi.mocked(client.delete)

describe('Portfolio API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('getPortfolios', () => {
    it('should fetch portfolio list', async () => {
      mockedGet.mockResolvedValue({
        data: [{ id: 1, name: '主账户', type: 'REAL', createdAt: '2026-01-01' }],
      })

      const result = await getPortfolios()

      expect(mockedGet).toHaveBeenCalledWith('/portfolios')
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('主账户')
    })
  })

  describe('createPortfolio', () => {
    it('should create portfolio with correct payload', async () => {
      mockedPost.mockResolvedValue({
        data: { id: 1, name: '新组合', type: 'SIMULATION' },
      })

      const result = await createPortfolio({ name: '新组合', type: 'SIMULATION' })

      expect(mockedPost).toHaveBeenCalledWith('/portfolios', { name: '新组合', type: 'SIMULATION' })
      expect(result.name).toBe('新组合')
    })
  })

  describe('updatePortfolio', () => {
    it('should update portfolio by id', async () => {
      mockedPut.mockResolvedValue({ data: { id: 1, name: '改名' } })

      const result = await updatePortfolio(1, { name: '改名', type: 'REAL' })

      expect(mockedPut).toHaveBeenCalledWith('/portfolios/1', { name: '改名', type: 'REAL' })
      expect(result.name).toBe('改名')
    })
  })

  describe('deletePortfolio', () => {
    it('should delete portfolio by id', async () => {
      mockedDelete.mockResolvedValue({})

      await deletePortfolio(1)

      expect(mockedDelete).toHaveBeenCalledWith('/portfolios/1')
    })
  })

  describe('getTransactions', () => {
    it('should fetch transactions with pagination', async () => {
      mockedGet.mockResolvedValue({
        data: { items: [], total: 0, page: 0, size: 20 },
      })

      const result = await getTransactions(1, { page: 0, size: 20 })

      expect(mockedGet).toHaveBeenCalledWith('/portfolios/1/transactions', {
        params: { page: 0, size: 20 },
      })
      expect(result.items).toHaveLength(0)
    })

    it('should fetch transactions with filters', async () => {
      mockedGet.mockResolvedValue({
        data: { items: [], total: 0, page: 0, size: 20 },
      })

      await getTransactions(1, { stockCode: '600519', tradeType: 'BUY', startDate: '2026-01-01' })

      expect(mockedGet).toHaveBeenCalledWith('/portfolios/1/transactions', {
        params: { stockCode: '600519', tradeType: 'BUY', startDate: '2026-01-01' },
      })
    })
  })

  describe('createTransaction', () => {
    it('should create transaction', async () => {
      mockedPost.mockResolvedValue({
        data: { id: 1, stockCode: '600519', tradeType: 'BUY' },
      })

      const result = await createTransaction(1, {
        stockCode: '600519',
        tradeDate: '2026-05-05',
        tradeType: 'BUY',
        quantity: 100,
      })

      expect(mockedPost).toHaveBeenCalledWith('/portfolios/1/transactions', expect.any(Object))
      expect(result.stockCode).toBe('600519')
    })
  })

  describe('updateTransaction', () => {
    it('should update transaction by id', async () => {
      mockedPut.mockResolvedValue({ data: { id: 1, quantity: 200 } })

      const result = await updateTransaction(1, {
        stockCode: '600519',
        tradeDate: '2026-05-05',
        tradeType: 'BUY',
        quantity: 200,
      })

      expect(mockedPut).toHaveBeenCalledWith('/transactions/1', expect.any(Object))
      expect(result.quantity).toBe(200)
    })
  })

  describe('deleteTransaction', () => {
    it('should delete transaction by id', async () => {
      mockedDelete.mockResolvedValue({})

      await deleteTransaction(1)

      expect(mockedDelete).toHaveBeenCalledWith('/transactions/1')
    })
  })

  describe('getPositions', () => {
    it('should fetch positions by portfolio id', async () => {
      mockedGet.mockResolvedValue({
        data: [{ stockCode: '600519', currentQuantity: 100, avgCost: 1688 }],
      })

      const result = await getPositions(1)

      expect(mockedGet).toHaveBeenCalledWith('/portfolios/1/positions')
      expect(result[0].stockCode).toBe('600519')
    })
  })

  describe('getPortfolioSummary', () => {
    it('should fetch portfolio summary', async () => {
      mockedGet.mockResolvedValue({
        data: { portfolioId: 1, totalCost: 10000, holdingCount: 2 },
      })

      const result = await getPortfolioSummary(1)

      expect(mockedGet).toHaveBeenCalledWith('/portfolios/1/summary')
      expect(result.holdingCount).toBe(2)
    })
  })
})
