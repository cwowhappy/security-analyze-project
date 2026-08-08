import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import LoginView from '../LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/', name: 'Home', component: { template: '<div>Home</div>' } }]
})

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render login form', () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [ElementPlus, router]
      }
    })

    expect(wrapper.find('.title').text()).toBe('安全分析平台')
    expect(wrapper.find('input[type="password"]').exists()).toBe(true)
  })

  it('should update username on input', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [ElementPlus, router]
      }
    })

    const input = wrapper.find('input')
    await input.setValue('admin')

    expect((wrapper.vm as any).form.username).toBe('admin')
  })
})
