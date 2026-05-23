import api from './api'

export default {
  async getStats() {
    const res = await api.get('/admin/stats')
    return res.data
  },

  async getIdentityVerifications() {
    const res = await api.get('/admin/identity-verifications')
    return res.data
  },

  async reviewIdentity(id, action) {
    const res = await api.patch(`/admin/identity-verifications/${id}?action=${action}`)
    return res.data
  },

  async getPendingVenues() {
    const res = await api.get('/admin/venues/pending')
    return res.data
  },

  async approveVenue(id) {
    const res = await api.patch(`/admin/venues/${id}/approve`)
    return res.data
  },

  async rejectVenue(id) {
    const res = await api.patch(`/admin/venues/${id}/reject`)
    return res.data
  },

  async getUsers() {
    const res = await api.get('/admin/users')
    return res.data
  },

  async assignRole(userId, roleName) {
    const res = await api.post(`/admin/users/${userId}/roles`, { roleName })
    return res.data
  },

  async revokeRole(userId, roleName) {
    const res = await api.delete(`/admin/users/${userId}/roles/${roleName}`)
    return res.data
  }
}
