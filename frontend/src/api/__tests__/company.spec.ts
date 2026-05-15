import { describe, it, expect, vi, beforeEach } from 'vitest'
import { companyApi } from '../modules/company'

const mockGet = vi.fn()
const mockPost = vi.fn()

vi.mock('@/utils/request', () => ({
  http: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
  },
}))

describe('companyApi', () => {
  beforeEach(() => {
    mockGet.mockClear()
    mockPost.mockClear()
  })

  it('list should build query with filters', async () => {
    mockGet.mockResolvedValue({ list: [], total: 0 })
    await companyApi.list({ page: 1, size: 20 }, '科技', '广东', '腾讯', 'personal')
    const url = mockGet.mock.calls[0][0] as string
    expect(url).toContain('industry=%E7%A7%91%E6%8A%80')
    expect(url).toContain('province=%E5%B9%BF%E4%B8%9C')
    expect(url).toContain('keyword=%E8%85%BE%E8%AE%AF')
    expect(url).toContain('controllerType=personal')
  })

  it('getById should call correct endpoint', async () => {
    mockGet.mockResolvedValue({ id: 'code1' })
    await companyApi.getById('code1')
    expect(mockGet).toHaveBeenCalledWith('/api/v1/companies/code1')
  })

  it('create should post data', async () => {
    mockPost.mockResolvedValue('company-id')
    const data = { name: 'Test Company' }
    await companyApi.create(data as any)
    expect(mockPost).toHaveBeenCalledWith('/api/v1/companies', data)
  })
})
