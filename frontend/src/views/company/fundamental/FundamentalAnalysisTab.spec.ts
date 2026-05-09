import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import FundamentalAnalysisTab from './FundamentalAnalysisTab.vue'

// Mock API
vi.mock('@/api/research', () => ({
  getFundamentalOverview: vi.fn(),
}))

import { getFundamentalOverview } from '@/api/research'

const mockedGetFundamentalOverview = vi.mocked(getFundamentalOverview)

describe('FundamentalAnalysisTab', () => {
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
      {
        reportDate: '2022-12-31',
        reportYear: 2022,
        totalRevenue: 14000000000,
        parentNetProfit: 6500000000,
        totalAssets: 23000000000,
        totalEquity: 17000000000,
        operatingCashFlow: 5500000000,
        grossMargin: 91,
        netMargin: 51,
        roe: 34,
        debtRatio: 26,
        operateIncome: 11000000000,
        operateCost: 900000000,
        totalLiabilities: 6000000000,
        totalCurrentAssets: 18000000000,
        totalNoncurrentAssets: 5000000000,
        investingCashFlow: -800000000,
        financingCashFlow: -3800000000,
        endCce: 4500000000,
        saleExpense: 280000000,
        manageExpense: 750000000,
        researchExpense: 90000000,
        financeExpense: 40000000,
      },
    ],
  }

  it('should render loading skeleton on mount', async () => {
    mockedGetFundamentalOverview.mockImplementation(() => new Promise(() => {}))

    const wrapper = mount(FundamentalAnalysisTab, {
      props: { stockCode: '600519' },
    })
    await nextTick()

    expect(wrapper.find('.metric-cards').exists()).toBe(true)
  })

  it('should fetch and display data on mount', async () => {
    mockedGetFundamentalOverview.mockResolvedValue(mockOverview)

    const wrapper = mount(FundamentalAnalysisTab, {
      props: { stockCode: '600519' },
    })
    await flushPromises()

    expect(mockedGetFundamentalOverview).toHaveBeenCalledWith('600519')
    expect(wrapper.text()).toContain('营业总收入')
    expect(wrapper.text()).toContain('归母净利润')
  })

  it('should display metric cards with formatted values', async () => {
    mockedGetFundamentalOverview.mockResolvedValue(mockOverview)

    const wrapper = mount(FundamentalAnalysisTab, {
      props: { stockCode: '600519' },
    })
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.metricCards.length).toBe(8)
    expect(vm.metricCards[0].value).toBe('140.00 亿') // totalRevenue (latest = 2022)
    expect(vm.metricCards[5].value).toBe('91.00%') // grossMargin (latest = 2022, value=91)
  })

  it('should render data table with metrics', async () => {
    mockedGetFundamentalOverview.mockResolvedValue(mockOverview)

    const wrapper = mount(FundamentalAnalysisTab, {
      props: { stockCode: '600519' },
    })
    await flushPromises()

    expect(wrapper.find('.data-table').exists()).toBe(true)
    const rows = wrapper.findAll('.data-table tbody tr')
    expect(rows.length).toBe(2)
  })

  it('should show empty state when no metrics', async () => {
    mockedGetFundamentalOverview.mockResolvedValue({
      stockCode: '600519',
      stockName: '贵州茅台',
      industry: '白酒',
      market: 'SH',
      metrics: [],
    })

    const wrapper = mount(FundamentalAnalysisTab, {
      props: { stockCode: '600519' },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('暂无年报数据')
  })

  it('should refetch when stockCode changes', async () => {
    mockedGetFundamentalOverview.mockResolvedValue(mockOverview)

    const wrapper = mount(FundamentalAnalysisTab, {
      props: { stockCode: '600519' },
    })
    await flushPromises()

    expect(mockedGetFundamentalOverview).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ stockCode: '000001' })
    await flushPromises()

    expect(mockedGetFundamentalOverview).toHaveBeenCalledTimes(2)
    expect(mockedGetFundamentalOverview).toHaveBeenLastCalledWith('000001')
  })

  it('should handle API error gracefully', async () => {
    mockedGetFundamentalOverview.mockRejectedValue(new Error('Network error'))

    const wrapper = mount(FundamentalAnalysisTab, {
      props: { stockCode: '600519' },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('暂无年报数据')
  })

  it('should format money correctly', async () => {
    mockedGetFundamentalOverview.mockResolvedValue(mockOverview)

    const wrapper = mount(FundamentalAnalysisTab, {
      props: { stockCode: '600519' },
    })
    await flushPromises()

    const vm = wrapper.vm as any
    // 使用组件暴露的 formatMoney 方法（通过直接访问组件方法）
    expect(vm.metricCards[0].value).toContain('亿') // >= 1e8
  })
})
