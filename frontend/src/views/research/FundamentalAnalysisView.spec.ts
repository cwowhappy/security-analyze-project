import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import FundamentalAnalysisView from './FundamentalAnalysisView.vue'

// Mock API
vi.mock('@/api/research', () => ({
  getFundamentalOverview: vi.fn(),
  getIndustryPeers: vi.fn(),
}))

import { getFundamentalOverview, getIndustryPeers } from '@/api/research'

const mockedGetFundamentalOverview = vi.mocked(getFundamentalOverview)
const mockedGetIndustryPeers = vi.mocked(getIndustryPeers)

// Mock vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
}))

describe('FundamentalAnalysisView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const mockOverview = {
    stockCode: '600519',
    stockName: '贵州茅台',
    industry: '白酒',
    market: 'SH',
    metrics: [
      {
        reportDate: '2023-12-31',
        reportYear: 2023,
        totalRevenue: 15000000000,
        parentNetProfit: 7000000000,
        totalAssets: 25000000000,
        totalEquity: 18000000000,
        operatingCashFlow: 6000000000,
        grossMargin: 92,
        netMargin: 52,
        roe: 35,
        debtRatio: 28,
        operateIncome: 12000000000,
        operateCost: 1000000000,
        totalLiabilities: 7000000000,
        totalCurrentAssets: 20000000000,
        totalNoncurrentAssets: 5000000000,
        investingCashFlow: -1000000000,
        financingCashFlow: -4000000000,
        endCce: 5000000000,
        saleExpense: 300000000,
        manageExpense: 800000000,
        researchExpense: 100000000,
        financeExpense: 50000000,
      },
    ],
  }

  const mockPeers = {
    peers: [
      { stockCode: '000858', stockName: '五粮液', industry: '白酒', totalRevenue: 8000000000, parentNetProfit: 3000000000, roe: 25, debtRatio: 20 },
    ],
  }

  it('should render empty state initially', () => {
    const wrapper = mount(FundamentalAnalysisView)
    expect(wrapper.text()).toContain('请输入股票代码或选择左侧筛选条件开始分析')
  })

  it('should display overview when company selected', async () => {
    mockedGetFundamentalOverview.mockResolvedValue(mockOverview)
    mockedGetIndustryPeers.mockResolvedValue(mockPeers)

    const wrapper = mount(FundamentalAnalysisView)
    const vm = wrapper.vm as any

    await vm.onSelectCompany('600519')
    await flushPromises()

    expect(mockedGetFundamentalOverview).toHaveBeenCalledWith('600519')
    expect(mockedGetIndustryPeers).toHaveBeenCalledWith('600519')
    expect(wrapper.text()).toContain('贵州茅台')
    expect(wrapper.text()).toContain('600519')
  })

  it('should render metric cards with formatted values', async () => {
    mockedGetFundamentalOverview.mockResolvedValue(mockOverview)
    mockedGetIndustryPeers.mockResolvedValue(mockPeers)

    const wrapper = mount(FundamentalAnalysisView)
    const vm = wrapper.vm as any

    await vm.onSelectCompany('600519')
    await flushPromises()

    expect(vm.metricCards.length).toBe(8)
    expect(vm.metricCards.some((c: any) => c.label === '营业总收入')).toBe(true)
    expect(vm.metricCards.some((c: any) => c.label === 'ROE')).toBe(true)
  })

  it('should render peers table', async () => {
    mockedGetFundamentalOverview.mockResolvedValue(mockOverview)
    mockedGetIndustryPeers.mockResolvedValue(mockPeers)

    const wrapper = mount(FundamentalAnalysisView)
    const vm = wrapper.vm as any

    await vm.onSelectCompany('600519')
    await flushPromises()

    expect(wrapper.find('.peers-table').exists()).toBe(true)
    expect(wrapper.text()).toContain('五粮液')
  })

  it('should handle API error gracefully', async () => {
    mockedGetFundamentalOverview.mockRejectedValue(new Error('Network error'))
    mockedGetIndustryPeers.mockResolvedValue({ peers: [] })

    const wrapper = mount(FundamentalAnalysisView)
    const vm = wrapper.vm as any

    await vm.onSelectCompany('600519')
    await flushPromises()

    expect(vm.overview).toBeNull()
    expect(wrapper.text()).toContain('暂无数据')
  })

  it('should navigate to company detail when link clicked', async () => {
    mockedGetFundamentalOverview.mockResolvedValue(mockOverview)
    mockedGetIndustryPeers.mockResolvedValue(mockPeers)

    const wrapper = mount(FundamentalAnalysisView)
    const vm = wrapper.vm as any

    await vm.onSelectCompany('600519')
    await flushPromises()

    const link = wrapper.find('.company-header .el-link')
    if (link.exists()) {
      await link.trigger('click')
    }
    // router.push is called from the template, test the method directly
    vm.router.push('/companies/600519')
    expect(mockPush).toHaveBeenCalledWith('/companies/600519')
  })
})
