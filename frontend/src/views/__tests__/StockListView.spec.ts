import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import StockListView from '../stock/StockListView.vue'

const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
}))

const mockPage = vi.fn()

vi.mock('@/api/modules/stock', () => ({
  stockApi: {
    page: (...args: unknown[]) => mockPage(...args),
  },
}))

describe('StockListView', () => {
  beforeEach(() => {
    mockPush.mockClear()
    mockPage.mockClear()
  })

  it('should render stock list page title', () => {
    mockPage.mockResolvedValue({ list: [], total: 0 })
    const wrapper = mount(StockListView)
    expect(wrapper.text()).toContain('股票列表')
  })

  it('should fetch data on mount', async () => {
    mockPage.mockResolvedValue({
      list: [
        { stockCode: '600001', name: 'Test Stock', exchange: 'SSE', market: '主板', industry: '银行', area: '北京', totalShares: 100000000, floatShares: 50000000 },
      ],
      total: 1,
    })
    const wrapper = mount(StockListView)
    await new Promise((r) => setTimeout(r, 10))
    expect(wrapper.text()).toContain('600001')
    expect(wrapper.text()).toContain('Test Stock')
  })

  it('should show empty state when no data', async () => {
    mockPage.mockResolvedValue({ list: [], total: 0 })
    const wrapper = mount(StockListView)
    await new Promise((r) => setTimeout(r, 10))
    expect(wrapper.text()).toContain('无匹配结果')
  })

  it('should call router push when clicking row', async () => {
    mockPage.mockResolvedValue({
      list: [{ stockCode: '600001', name: 'Test Stock' }],
      total: 1,
    })
    const wrapper = mount(StockListView)
    await new Promise((r) => setTimeout(r, 10))
    await wrapper.find('.cursor-row').trigger('click')
    expect(mockPush).toHaveBeenCalledWith('/stocks/600001')
  })

  it('should render exchange badge class correctly', async () => {
    mockPage.mockResolvedValue({
      list: [
        { stockCode: '600001', name: 'A', exchange: 'SSE' },
        { stockCode: '000001', name: 'B', exchange: 'SZSE' },
        { stockCode: '430001', name: 'C', exchange: 'BSE' },
      ],
      total: 3,
    })
    const wrapper = mount(StockListView)
    await new Promise((r) => setTimeout(r, 10))
    expect(wrapper.find('.b-sh').exists()).toBe(true)
    expect(wrapper.find('.b-sz').exists()).toBe(true)
    expect(wrapper.find('.b-bj').exists()).toBe(true)
  })
})
