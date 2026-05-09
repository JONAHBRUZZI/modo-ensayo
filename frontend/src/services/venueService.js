import api from './api'

export const venueService = {
  async create(request) {
    const { data } = await api.post('/venues', request)
    return data
  },
  async listApproved() {
    const { data } = await api.get('/venues')
    return data
  },
  async getMyVenues() {
    const { data } = await api.get('/venue-admin/my-venues')
    return data
  },
  async createVenue(request) {
    const { data } = await api.post('/venue-admin/venues', request)
    return data
  },
  async updateVenue(id, request) {
    const { data } = await api.patch(`/venue-admin/venues/${id}`, request)
    return data
  },
  async getRooms(venueId) {
    const { data } = await api.get(`/venue-admin/venues/${venueId}/rooms`)
    return data
  },
  async createRoom(venueId, request) {
    const { data } = await api.post(`/venue-admin/venues/${venueId}/rooms`, request)
    return data
  },
  async getClassesPendingConfirmation() {
    const { data } = await api.get('/venue-admin/classes/pending-confirmation')
    return data
  },
  async confirmClassRealized(classId) {
    const { data } = await api.patch(`/venue-admin/classes/${classId}/confirm-realized`)
    return data
  },
  async confirmClassNotRealized(classId) {
    const { data } = await api.patch(`/venue-admin/classes/${classId}/confirm-not-realized`)
    return data
  },
  async createRoomAvailability(roomId, request) {
    const { data } = await api.post(`/venue-admin/rooms/${roomId}/availability`, request)
    return data
  },
  async getRoomAvailability(roomId) {
    const { data } = await api.get(`/venue-admin/rooms/${roomId}/availability`)
    return data
  },
  async deleteRoomAvailability(roomId, availabilityId) {
    await api.post(`/venue-admin/rooms/${roomId}/availability/delete/${availabilityId}`)
  },
}