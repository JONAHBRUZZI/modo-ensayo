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
    createMercadoPagoPreference: vi.fn().mockResolvedValue({
      preferenceId: 'pref-123',
      initPoint: 'https://www.mercadopago.cl/checkout/v1/redirect?pref_id=pref-123',
      sandboxInitPoint: 'https://sandbox.mercadopago.cl/checkout/v1/redirect?pref_id=pref-123',
    }),
  }
}))

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', component: { template: '<div/>' } },
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
  })

  it('renders cart items and total correctly', async () => {
    const wrapper = mount(CartPage, { global: { plugins: [router] } })
    await new Promise(r => setTimeout(r, 50)) // wait for onMounted
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Cueca Básica')
    expect(wrapper.text()).toContain('Ballet')
    expect(wrapper.text()).toContain('35.000') // total
  })

  it('shows ConfirmModal when clicking Pagar button', async () => {
    const wrapper = mount(CartPage, { global: { plugins: [router] } })
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const payButton = wrapper.find('button')
    expect(payButton.exists()).toBe(true)
    await payButton.trigger('click')

    // showConfirm should be true after clicking
    expect(wrapper.vm.showConfirm).toBe(true)
  })

  it('does NOT call createMercadoPagoPreference before confirming', async () => {
    const paymentService = (await import('@/services/paymentService')).default
    const wrapper = mount(CartPage, { global: { plugins: [router] } })
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const payButton = wrapper.find('button')
    await payButton.trigger('click') // opens confirm modal

    expect(paymentService.createMercadoPagoPreference).not.toHaveBeenCalled()
  })

  it('calls createMercadoPagoPreference after handleConfirmedCheckout', async () => {
    const paymentService = (await import('@/services/paymentService')).default
    const wrapper = mount(CartPage, { global: { plugins: [router] } })
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    // Simulate confirmed checkout directly
    await wrapper.vm.handleConfirmedCheckout()

    expect(paymentService.createMercadoPagoPreference).toHaveBeenCalledOnce()
  })
})
