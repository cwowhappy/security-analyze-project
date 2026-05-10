import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BaseButton from '../base/BaseButton.vue'

describe('BaseButton', () => {
  it('should render slot content', () => {
    const wrapper = mount(BaseButton, {
      slots: { default: '点击我' },
    })
    expect(wrapper.text()).toBe('点击我')
  })

  it('should emit click event when clicked', async () => {
    const wrapper = mount(BaseButton, {
      slots: { default: '点击我' },
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })

  it('should not emit click when disabled', async () => {
    const wrapper = mount(BaseButton, {
      props: { disabled: true },
      slots: { default: '点击我' },
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })
})
