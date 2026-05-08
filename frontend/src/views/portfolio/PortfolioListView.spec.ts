import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElCard, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElRadio, ElTag, ElEmpty } from 'element-plus'
import PortfolioListView from './PortfolioListView.vue'

vi.mock('@/api/portfolio', () => ({
  getPortfolios: vi.fn(() => Promise.resolve([
    { id: 1, name: '测试组合', type: 'REAL', broker: '华泰', createdAt: '2026-01-01' },
  ])),
  createPortfolio: vi.fn(),
  updatePortfolio: vi.fn(),
  deletePortfolio: vi.fn(),
}))

describe('PortfolioListView', () => {
  const globalComponents = {
    ElCard, ElButton, ElDialog, ElForm, ElFormItem, ElInput, ElRadio, ElTag, ElEmpty,
  }

  it('should render portfolio list', async () => {
    const wrapper = mount(PortfolioListView, {
      global: { components: globalComponents },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('测试组合')
    expect(wrapper.text()).toContain('华泰')
  })

  it('should open create dialog when clicking add button', async () => {
    const wrapper = mount(PortfolioListView, {
      global: { components: globalComponents },
    })

    await flushPromises()

    const addBtn = wrapper.findAllComponents(ElButton).find(b => b.text().includes('新增'))
    await addBtn?.trigger('click')

    expect(wrapper.findComponent(ElDialog).isVisible()).toBe(true)
  })
})
