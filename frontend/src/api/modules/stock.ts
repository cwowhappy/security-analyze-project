import { http } from '@/utils/request'
import type { Stock, CreateStockRequest } from '@/types/stock'

const PREFIX = '/stocks'

export const stockApi = {
  list: () => http.get<Stock[]>(PREFIX),
  getBySymbol: (symbol: string) => http.get<Stock>(`${PREFIX}/${symbol}`),
  create: (data: CreateStockRequest) => http.post<string>(PREFIX, data),
}
