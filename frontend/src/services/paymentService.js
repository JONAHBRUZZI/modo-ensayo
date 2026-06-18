import api from './api'

export default {
  async addToCart(classId, beneficiaryType, beneficiaryId) {
    const res = await api.post('/payments/cart', { classId, beneficiaryType, beneficiaryId })
    return res.data
  },

  async getCart() {
    const res = await api.get('/payments/cart')
    return res.data
  },

  async removeFromCart(id) {
    await api.delete(`/payments/cart/${id}`)
  },

  async createMercadoPagoPreference() {
    const res = await api.post('/payments/mercadopago/create-preference')
    return res.data
  },

  async getMyEnrollments() {
    const res = await api.get('/payments/my-enrollments')
    return res.data
  },

  async getMyPaymentHistory() {
    const res = await api.get('/payments/my-history')
    return res.data
  }
}
