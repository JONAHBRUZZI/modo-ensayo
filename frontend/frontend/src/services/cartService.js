import api from './api'

export const cartService = {
  async addToCart(item) {
    const { data } = await api.post('/payments/cart', item)
    return data
  },

  async getCart() {
    const { data } = await api.get('/payments/cart')
    return data
  },

  async removeFromCart(itemId) {
    await api.delete(`/payments/cart/${itemId}`)
  },

  async checkout(checkoutData) {
    const { data } = await api.post('/payments/checkout', checkoutData)
    return data
  },

  async createMercadoPagoPreference() {
    const { data } = await api.post('/payments/mercadopago/create-preference')
    return data
  },
}
