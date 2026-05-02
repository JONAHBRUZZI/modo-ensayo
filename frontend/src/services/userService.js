import api from './api'

export const userService = {
  async getProfile() {
    const { data } = await api.get('/users/me')
    return data
  },

  async updateProfile(profileData) {
    const { data } = await api.put('/users/me', profileData)
    return data
  },

  async getRefundMethods() {
    const { data } = await api.get('/users/me/refund-methods')
    return data
  },

  async createRefundMethod(methodData) {
    const { data } = await api.post('/users/me/refund-methods', methodData)
    return data
  },

  async deleteRefundMethod(id) {
    await api.delete(`/users/me/refund-methods/${id}`)
  },

  async getIdentityVerification() {
    const { data } = await api.get('/users/me/identity-verification')
    return data
  },

  async uploadIdentity(documentUrl) {
    const { data } = await api.post('/users/me/identity-verification', { documentUrl })
    return data
  },
}
