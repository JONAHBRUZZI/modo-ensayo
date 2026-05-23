import api from './api'

export default {
  async getProfile() {
    const res = await api.get('/users/me')
    return res.data
  },

  async updateProfile(data) {
    const res = await api.put('/users/me', data)
    return res.data
  },

  async getRefundMethods() {
    const res = await api.get('/users/me/refund-methods')
    return res.data
  },

  async createRefundMethod(data) {
    const res = await api.post('/users/me/refund-methods', data)
    return res.data
  },

  async deleteRefundMethod(id) {
    await api.delete(`/users/me/refund-methods/${id}`)
  },

  async getIdentityVerification() {
    const res = await api.get('/users/me/identity-verification')
    return res.data
  },

  async uploadIdentityDocument(documentUrl) {
    const res = await api.post('/users/me/identity-verification', { documentUrl })
    return res.data
  }
}
