import api from './api'

export default {
  async createVenue(data) {
    const res = await api.post('/venues', data)
    return res.data
  },

  async getVenues() {
    const res = await api.get('/venues')
    return res.data
  },

  async getMyVenues() {
    const res = await api.get('/venue-admin/my-venues')
    return res.data
  },

  async createVenueAdmin(data) {
    const res = await api.post('/venue-admin/venues', data)
    return res.data
  },

  async updateVenue(id, data) {
    const res = await api.patch(`/venue-admin/venues/${id}`, data)
    return res.data
  },

  async getVenueMetrics() {
    const res = await api.get('/venue-admin/metrics')
    return res.data
  },

  async getVenueProfessors() {
    const res = await api.get('/venue-admin/professors')
    return res.data
  },

  async getVenueRooms(venueId) {
    const res = await api.get(`/venue-admin/venues/${venueId}/rooms`)
    return res.data
  },

  async getRooms(venueId) {
    const res = await api.get(`/venues/${venueId}/rooms`)
    return res.data
  },

  async createRoom(venueId, data) {
    const res = await api.post(`/venue-admin/venues/${venueId}/rooms`, data)
    return res.data
  },

  async getPendingClasses() {
    const res = await api.get('/venue-admin/classes/pending-confirmation')
    return res.data
  },

  async confirmClassRealized(classId) {
    const res = await api.post(`/venue-admin/classes/${classId}/confirm-realized`, { confirmacion: true })
    return res.data
  },

  async confirmClassNotRealized(classId) {
    const res = await api.post(`/venue-admin/classes/${classId}/confirm-not-realized`, { confirmacion: true })
    return res.data
  },

  async createRoomAvailability(roomId, data) {
    const res = await api.post(`/venue-admin/rooms/${roomId}/availability`, data)
    return res.data
  },

  async getRoomAvailability(roomId) {
    const res = await api.get(`/venue-admin/rooms/${roomId}/availability`)
    return res.data
  },

  async deleteRoomAvailability(roomId, slotId) {
    await api.post(`/venue-admin/rooms/${roomId}/availability/delete/${slotId}`)
  }
}
