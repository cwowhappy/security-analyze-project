import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TransactionFormDialog from './TransactionFormDialog.vue'

describe('TransactionFormDialog', () => {
  it('should receive portfolioId prop', () => {
    const wrapper = mount(TransactionFormDialog, {
      props: { modelValue: true, portfolioId: 42 },
    })
    expect(wrapper.props('portfolioId')).toBe(42)
  })

  it('should initialize form with editData when provided', () => {
    const editData = {
      stockCode: '600519',
      tradeDate: '2026-05-05',
      tradeType: 'BUY' as const,
      price: 100,
      quantity: 10,
      fee: 5,
      tax: 1,
      remark: '测试',
      id: 3,
    }

    const wrapper = mount(TransactionFormDialog, {
      props: { modelValue: true, portfolioId: 1, editData },
    })

    expect(wrapper.props('editData').stockCode).toBe('600519')
    expect(wrapper.props('editData').id).toBe(3)
  })

  it('should compute isEdit correctly', () => {
    const wrapperCreate = mount(TransactionFormDialog, {
      props: { modelValue: true, portfolioId: 1 },
    })
    expect((wrapperCreate.vm as any).isEdit).toBe(false)

    const wrapperEdit = mount(TransactionFormDialog, {
      props: { modelValue: true, portfolioId: 1, editData: { id: 2, stockCode: '000001', tradeDate: '2026-01-01', tradeType: 'BUY', quantity: 100 } },
    })
    expect((wrapperEdit.vm as any).isEdit).toBe(true)
  })
})
