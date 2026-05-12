import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import CompanyListView from '../company/CompanyListView.vue'

const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
}))

const mockList = vi.fn()

vi.mock('@/api/modules/company', () => ({
  companyApi: {
    list: (...args: unknown[]) => mockList(...args),
  },
}))

describe('CompanyListView', () => {
  beforeEach(() => {
    mockPush.mockClear()
    mockList.mockClear()
  })

  it('should render company list page title', () => {
    mockList.mockResolvedValue({ list: [], total: 0 })
    const wrapper = mount(CompanyListView)
    expect(wrapper.text()).toContain('公司列表')
  })

  it('should fetch data on mount', async () => {
    mockList.mockResolvedValue({
      list: [
        { id: '1', name: 'Test Company', shortName: 'TC', industry: '科技', province: '广东', city: '深圳', controllerType: '民营', legalRepresentative: '张三', employees: 1000 },
      ],
      total: 1,
    })
    const wrapper = mount(CompanyListView)
    await new Promise((r) => setTimeout(r, 10))
    expect(wrapper.text()).toContain('Test Company')
    expect(wrapper.text()).toContain('张三')
  })

  it('should show empty state when no data', async () => {
    mockList.mockResolvedValue({ list: [], total: 0 })
    const wrapper = mount(CompanyListView)
    await new Promise((r) => setTimeout(r, 10))
    expect(wrapper.text()).toContain('无匹配结果')
  })

  it('should render controller type badge correctly', async () => {
    mockList.mockResolvedValue({
      list: [
        { name: 'A', controllerType: '国企' },
        { name: 'B', controllerType: '民营' },
        { name: 'C', controllerType: '外资' },
      ],
      total: 3,
    })
    const wrapper = mount(CompanyListView)
    await new Promise((r) => setTimeout(r, 10))
    expect(wrapper.find('.bs-gq').exists()).toBe(true)
    expect(wrapper.find('.bs-my').exists()).toBe(true)
    expect(wrapper.find('.bs-wz').exists()).toBe(true)
  })
})
