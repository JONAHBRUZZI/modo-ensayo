import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import PaymentSuccessPage from './PaymentSuccessPage.vue'

function createTestRouter(query = {}) {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/payment/success', name: 'PaymentSuccess', component: PaymentSuccessPage },
      { path: '/alumno/mis-clases', component: { template: '<div/>' } },
      { path: '/alumno/pagos', component: { template: '<div/>' } },
    ],
  })
  return router
}

describe('PaymentSuccessPage', () => {
  it('renders success message', async () => {
    const router = createTestRouter()
    await router.push('/payment/success')
    const wrapper = mount(PaymentSuccessPage, { global: { plugins: [router] } })

    expect(wrapper.text()).toContain('Pago Exitoso')
    expect(wrapper.text()).toContain('procesado correctamente')
  })

  it('shows payment_id from query params', async () => {
    const router = createTestRouter()
    await router.push('/payment/success?payment_id=MP-12345&status=approved&external_reference=ref-abc')
    const wrapper = mount(PaymentSuccessPage, { global: { plugins: [router] } })

    expect(wrapper.text()).toContain('MP-12345')
    expect(wrapper.text()).toContain('ref-abc')
  })

  it('does NOT show payment info section when no payment_id', async () => {
    const router = createTestRouter()
    await router.push('/payment/success')
    const wrapper = mount(PaymentSuccessPage, { global: { plugins: [router] } })

    // The info card with N° operación only shows when paymentId is present
    expect(wrapper.text()).not.toContain('N° de operación')
  })

  it('shows links to mis-clases and pagos', async () => {
    const router = createTestRouter()
    await router.push('/payment/success')
    const wrapper = mount(PaymentSuccessPage, { global: { plugins: [router] } })

    expect(wrapper.html()).toContain('/alumno/mis-clases')
    expect(wrapper.html()).toContain('/alumno/pagos')
  })
})
