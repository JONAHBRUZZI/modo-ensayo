import api from './api'

export default {
  async addToCart(classId) {
    const res = await api.post('/payments/cart', { classId })
    return res.data
  },

  async getCart() {
    const res = await api.get('/payments/cart')
    return res.data
  },

  async removeFromCart(id) {
    await api.delete(`/payments/cart/${id}`)
  },

  async checkout() {
    const res = await api.post('/payments/checkout')
    return res.data
  },

  async createMercadoPagoPreference() {
    const res = await api.post('/payments/mercadopago/create-preference')
    return res.data
  }
}
