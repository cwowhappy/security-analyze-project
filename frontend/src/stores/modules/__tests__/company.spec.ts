import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useCompanyStore } from '../company'

const mockList = vi.fn()
const mockGetByUscCode = vi.fn()

vi.mock('@/api/modules/company', () => ({
  companyApi: {
    list: (...args: unknown[]) => mockList(...args),
    getByUscCode: (...args: unknown[]) => mockGetByUscCode(...args),
  },
}))

describe('company store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockList.mockClear()
    mockGetByUscCode.mockClear()
  })

  it('should have initial state', () => {
    const store = useCompanyStore()
    expect(store.companies).toEqual([])
    expect(store.companyTotal).toBe(0)
    expect(store.currentCompany).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.companyCount).toBe(0)
  })

  it('fetchCompanies should update state', async () => {
    const store = useCompanyStore()
    mockList.mockResolvedValue({ list: [{ name: 'Test Co' }], total: 1 })

    await store.fetchCompanies({ page: 1, size: 20 }, '科技')

    expect(store.companies).toHaveLength(1)
    expect(store.companyTotal).toBe(1)
    expect(mockList).toHaveBeenCalledWith({ page: 1, size: 20 }, '科技', undefined, undefined, undefined)
  })

  it('fetchCompanyDetail should update currentCompany', async () => {
    const store = useCompanyStore()
    mockGetByUscCode.mockResolvedValue({ name: 'Detail Co' })

    await store.fetchCompanyDetail('usc-123')

    expect(store.currentCompany).toEqual({ name: 'Detail Co' })
    expect(store.loading).toBe(false)
  })
})
