import api from './api'

export const adminService = {
  async getStats() {
    const { data } = await api.get('/admin/stats')
    return data
  },
  async getPendingVerifications() {
    const { data } = await api.get('/admin/identity-verifications')
    return data
  },
  async reviewIdentity(id, action) {
    const { data } = await api.patch(`/admin/identity-verifications/${id}?action=${action}`)
    return data
  },
  async getPendingVenues() {
    const { data } = await api.get('/admin/venues/pending')
    return data
  },
  async approveVenue(id) {
    const { data } = await api.patch(`/admin/venues/${id}/approve`)
    return data
  },
  async rejectVenue(id) {
    const { data } = await api.patch(`/admin/venues/${id}/reject`)
    return data
  }
}
