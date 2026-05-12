import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import HomeView from '../HomeView.vue'

describe('HomeView', () => {
  it('should render welcome text', () => {
    const wrapper = mount(HomeView)
    expect(wrapper.text()).toContain('A 股市场正常运行')
  })

  it('should render description', () => {
    const wrapper = mount(HomeView)
    expect(wrapper.text()).toContain('数据采集任务')
  })
})
