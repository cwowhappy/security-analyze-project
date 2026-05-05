import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref, nextTick } from 'vue'
import IndexDetailView from './IndexDetailView.vue'

// Mock API
vi.mock('@/api/index', () => ({
  getIndexDetail: vi.fn(),
  getIndexTrend: vi.fn(),
  getIndexEtfs: vi.fn(),
}))

import { getIndexDetail, getIndexTrend, getIndexEtfs } from '@/api/index'

const mockedGetIndexDetail = vi.mocked(getIndexDetail)
const mockedGetIndexTrend = vi.mocked(getIndexTrend)
const mockedGetIndexEtfs = vi.mocked(getIndexEtfs)

// Mock vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { indexCode: '000001' },
    path: '/indexes/000001',
  }),
  useRouter: () => ({
    push: mockPush,
  }),
}))

// Mock vue-echarts
vi.mock('vue-echarts', () => ({
  default: {
    name: 'VChart',
    render: () => null,
  },
}))

describe('IndexDetailView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetIndexDetail.mockResolvedValue({
      indexCode: '000001',
      indexName: '上证指数',
      indexType: '宽基',
      market: 'SH',
      baseDate: '1991-07-15',
      basePoint: 100.0,
      componentCount: 1800,
      publishDate: '1991-07-15',
    })
    mockedGetIndexTrend.mockResolvedValue({
      indexCode: '000001',
      granularity: 'day',
      items: [
        { tradeDate: '2024-01-01', openPrice: 2900, highPrice: 3000, lowPrice: 2880, closePrice: 2950, volume: 1000000 },
        { tradeDate: '2024-01-02', openPrice: 2950, highPrice: 2980, lowPrice: 2920, closePrice: 2960, volume: 1200000 },
      ],
    })
    mockedGetIndexEtfs.mockResolvedValue([
      { etfCode: '510050', etfName: '华夏上证50ETF', trackingIndexCode: '000016', fundSize: 1000000000, market: 'SH' },
    ])
  })

  it('should fetch detail on mount', async () => {
    const wrapper = mount(IndexDetailView)
    await flushPromises()

    expect(mockedGetIndexDetail).toHaveBeenCalledWith('000001')
    expect(wrapper.text()).toContain('上证指数')
    expect(wrapper.text()).toContain('000001')
  })

  it('should display basic info correctly', async () => {
    const wrapper = mount(IndexDetailView)
    await flushPromises()

    expect(wrapper.text()).toContain('宽基')
    expect(wrapper.text()).toContain('SH')
  })

  it('should fetch trend when trend tab is activated', async () => {
    const wrapper = mount(IndexDetailView)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.activeTab = 'trend'
    await flushPromises()

    expect(mockedGetIndexTrend).toHaveBeenCalledWith('000001', 'day')
    expect(vm.trendData).toHaveLength(2)
  })

  it('should fetch ETFs when etf tab is activated', async () => {
    const wrapper = mount(IndexDetailView)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.activeTab = 'etfs'
    await flushPromises()

    expect(mockedGetIndexEtfs).toHaveBeenCalledWith('000001')
    expect(vm.etfData).toHaveLength(1)
    expect(vm.etfData[0].etfCode).toBe('510050')
  })

  it('should switch trend granularity', async () => {
    const wrapper = mount(IndexDetailView)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.activeTab = 'trend'
    await flushPromises()

    vi.clearAllMocks()
    vm.trendGranularity = 'week'
    await flushPromises()

    expect(mockedGetIndexTrend).toHaveBeenCalledWith('000001', 'week')
  })

  it('should compute chart option from trend data', async () => {
    const wrapper = mount(IndexDetailView)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.activeTab = 'trend'
    await flushPromises()

    const option = vm.chartOption
    expect(option.series).toHaveLength(2)
    expect(option.series[0].name).toBe('收盘价')
    expect(option.series[1].name).toBe('成交量')
    expect(option.xAxis[0].data).toEqual(['2024-01-01', '2024-01-02'])
  })

  it('should handle detail fetch error gracefully', async () => {
    mockedGetIndexDetail.mockRejectedValue(new Error('Network error'))

    const wrapper = mount(IndexDetailView)
    await flushPromises()

    expect(wrapper.vm.indexInfo).toBeNull()
  })

  it('should not refetch trend if already loaded', async () => {
    const wrapper = mount(IndexDetailView)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.activeTab = 'trend'
    await flushPromises()

    vi.clearAllMocks()
    // Switch away and back
    vm.activeTab = 'basic'
    await flushPromises()
    vm.activeTab = 'trend'
    await flushPromises()

    expect(mockedGetIndexTrend).not.toHaveBeenCalled()
  })
})
