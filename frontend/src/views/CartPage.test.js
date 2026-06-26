import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import CartPage from './CartPage.vue'

// Mock paymentService
vi.mock('@/services/paymentService', () => ({
  default: {
    getCart: vi.fn().mockResolvedValue({ items: [
      { id: '1', classTitle: 'Cueca Básica', discipline: 'Cueca', price: 15000 },
      { id: '2', classTitle: 'Ballet', discipline: 'Ballet', price: 20000 },
    ]}),
    removeFromCart: vi.fn().mockResolvedValue({}),
    checkout: vi.fn().mockResolvedValue({}),
  }
}))

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', component: { template: '<div/>' } },
      { path: '/payment/checkout', component: { template: '<div/>' } },
      { path: '/payment/success', component: { template: '<div/>' } },
      { path: '/payment/failure', component: { template: '<div/>' } },
      { path: '/payment/pending', component: { template: '<div/>' } },
    ],
  })
}

describe('CartPage', () => {
  let router

  beforeEach(() => {
    router = createTestRouter()
    vi.clearAllMocks()
  })

  it('renders cart items and total correctly', async () => {
    const wrapper = mount(CartPage, { global: { plugins: [router] } })
    await new Promise(r => setTimeout(r, 50)) // wait for onMounted
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Cueca Básica')
    expect(wrapper.text()).toContain('Ballet')
    expect(wrapper.text()).toContain('35.000') // total
  })

  it('shows confirm modal when clicking Pagar button', async () => {
    const wrapper = mount(CartPage, { global: { plugins: [router] } })
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBeGreaterThan(0)
    const payButton = buttons[buttons.length - 1]
    payButton.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.showConfirm).toBe(true)
  })
})
