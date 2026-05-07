import { client } from './axios'
import type { Portfolio, PortfolioRequest, Transaction, TransactionRequest, TransactionListResponse, Position, PortfolioSummary } from '@/types/portfolio'

export async function getPortfolios(): Promise<Portfolio[]> {
  const response = await client.get('/portfolios')
  return response.data
}

export async function getPortfolio(id: number): Promise<Portfolio> {
  // 当前后端没有单独的 GET /api/portfolios/:id 接口，从列表中查找
  const list = await getPortfolios()
  const found = list.find(p => p.id === id)
  if (!found) throw new Error('组合不存在')
  return found
}

export async function createPortfolio(data: PortfolioRequest): Promise<Portfolio> {
  const response = await client.post('/portfolios', data)
  return response.data
}

export async function updatePortfolio(id: number, data: PortfolioRequest): Promise<Portfolio> {
  const response = await client.put(`/portfolios/${id}`, data)
  return response.data
}

export async function deletePortfolio(id: number): Promise<void> {
  await client.delete(`/portfolios/${id}`)
}

export async function getTransactions(
  portfolioId: number,
  params?: {
    stockCode?: string
    tradeType?: string
    startDate?: string
    endDate?: string
    page?: number
    size?: number
  }
): Promise<TransactionListResponse> {
  const response = await client.get(`/portfolios/${portfolioId}/transactions`, { params })
  return response.data
}

export async function createTransaction(portfolioId: number, data: TransactionRequest): Promise<Transaction> {
  const response = await client.post(`/portfolios/${portfolioId}/transactions`, data)
  return response.data
}

export async function updateTransaction(id: number, data: TransactionRequest): Promise<Transaction> {
  const response = await client.put(`/transactions/${id}`, data)
  return response.data
}

export async function deleteTransaction(id: number): Promise<void> {
  await client.delete(`/transactions/${id}`)
}

export async function getPositions(portfolioId: number): Promise<Position[]> {
  const response = await client.get(`/portfolios/${portfolioId}/positions`)
  return response.data
}

export async function getPortfolioSummary(portfolioId: number): Promise<PortfolioSummary> {
  const response = await client.get(`/portfolios/${portfolioId}/summary`)
  return response.data
}

export interface ImportResult {
  total: number
  success: number
  errors: { line: number; content: string; message: string }[]
}

export async function importTransactions(portfolioId: number, file: File): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await client.post(`/portfolios/${portfolioId}/transactions/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}
