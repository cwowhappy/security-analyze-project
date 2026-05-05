import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ref } from 'vue'
import IndexListView from './IndexListView.vue'

// Mock API
vi.mock('@/api/index', () => ({
  getIndexList: vi.fn(),
  getIndexCategories: vi.fn(),
}))

import { getIndexList, getIndexCategories } from '@/api/index'

const mockedGetIndexList = vi.mocked(getIndexList)
const mockedGetIndexCategories = vi.mocked(getIndexCategories)

// Mock vue-router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
}))

describe('IndexListView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetIndexCategories.mockResolvedValue([
      {
        indexType: '宽基',
        indexTypeLabel: '宽基指数',
        items: [
          { indexCode: '000001', indexName: '上证指数', market: 'SH', indexType: '宽基' },
          { indexCode: '000300', indexName: '沪深300', market: 'SH', indexType: '宽基' },
        ],
      },
      {
        indexType: '行业',
        indexTypeLabel: '行业指数',
        items: [
          { indexCode: '399989', indexName: '中证医疗', market: 'SZ', indexType: '行业' },
        ],
      },
    ])
  })

  it('should render category tabs on mount', async () => {
    const wrapper = mount(IndexListView)
    await flushPromises()

    expect(mockedGetIndexCategories).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.category-section').exists()).toBe(true)
    expect(wrapper.text()).toContain('核心指数')
  })

  it('should display index cards in category tabs', async () => {
    const wrapper = mount(IndexListView)
    await flushPromises()

    expect(wrapper.text()).toContain('上证指数')
    expect(wrapper.text()).toContain('沪深300')
    expect(wrapper.text()).toContain('中证医疗')
  })

  it('should navigate to detail when card clicked', async () => {
    const wrapper = mount(IndexListView)
    await flushPromises()

    const cards = wrapper.findAll('.index-card')
    expect(cards.length).toBeGreaterThan(0)

    await cards[0].trigger('click')
    expect(mockPush).toHaveBeenCalledWith('/indexes/000001')
  })

  it('should call getIndexList when searching', async () => {
    mockedGetIndexList.mockResolvedValue({
      items: [{ indexCode: '000300', indexName: '沪深300', indexType: '宽基', market: 'SH' }],
      total: 1,
      page: 0,
      size: 20,
    })

    const wrapper = mount(IndexListView)
    await flushPromises()

    // Simulate setting keyword and triggering search
    const vm = wrapper.vm as any
    vm.keyword = '沪深300'
    await vm.handleSearch()
    await flushPromises()

    expect(mockedGetIndexList).toHaveBeenCalledWith('沪深300', 0, 20)
  })

  it('should handle empty search results', async () => {
    mockedGetIndexList.mockResolvedValue({
      items: [],
      total: 0,
      page: 0,
      size: 20,
    })

    const wrapper = mount(IndexListView)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.keyword = 'notexist'
    await vm.handleSearch()
    await flushPromises()

    expect(vm.tableData).toHaveLength(0)
    expect(vm.hasSearched).toBe(true)
  })

  it('should reset search when keyword is cleared', async () => {
    const wrapper = mount(IndexListView)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.keyword = ''
    await vm.handleSearch()

    expect(vm.hasSearched).toBe(false)
    expect(vm.tableData).toHaveLength(0)
  })

  it('should handle page change', async () => {
    mockedGetIndexList.mockResolvedValue({
      items: [],
      total: 50,
      page: 0,
      size: 20,
    })

    const wrapper = mount(IndexListView)
    await flushPromises()

    const vm = wrapper.vm as any
    vm.keyword = 'test'
    vm.handlePageChange(2)
    await flushPromises()

    expect(vm.page).toBe(1)
    expect(mockedGetIndexList).toHaveBeenCalledWith('test', 1, 20)
  })

  it('should handle category loading error gracefully', async () => {
    mockedGetIndexCategories.mockRejectedValue(new Error('Network error'))

    const wrapper = mount(IndexListView)
    await flushPromises()

    // Should not throw; component handles error internally
    expect(wrapper.find('.category-section').exists()).toBe(false)
  })
})
