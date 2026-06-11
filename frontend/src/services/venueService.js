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

  async registrarVenueConDocumentos(formData) {
    const res = await api.post('/venue-admin/venues/registrar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
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

  async getVenueClasses() {
    const res = await api.get('/venue-admin/classes')
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

  async getPublicRoomAvailability(roomId) {
    const res = await api.get(`/venues/rooms/${roomId}/availability`)
    return res.data
  },

  async deleteRoomAvailability(roomId, slotId) {
    await api.delete(`/venue-admin/rooms/${roomId}/availability/${slotId}`)
  },

  async updateVenueSocial(id, data) {
    const res = await api.patch(`/venue-admin/venues/${id}/social`, data)
    return res.data
  },

  async updateRoom(roomId, data) {
    const res = await api.patch(`/venue-admin/rooms/${roomId}`, data)
    return res.data
  },

  // ── Documentos de sede ──────────────────────────────────────────────────
  async getVenueDocuments(venueId) {
    const res = await api.get(`/venue-admin/venues/${venueId}/documentos`)
    return res.data
  },

  async addVenueDocument(venueId, data) {
    const res = await api.post(`/venue-admin/venues/${venueId}/documentos`, data)
    return res.data
  },

  async deleteVenueDocument(docId) {
    await api.delete(`/venue-admin/documentos/${docId}`)
  },

  // ── Fotos de sede y sala ────────────────────────────────────────────────
  async getVenuePhotos(venueId) {
    const res = await api.get(`/venues/${venueId}/fotos`)
    return res.data
  },

  async addVenuePhoto(venueId, data) {
    const res = await api.post(`/venue-admin/venues/${venueId}/fotos`, data)
    return res.data
  },

  async getRoomPhotos(roomId) {
    const res = await api.get(`/venues/rooms/${roomId}/fotos`)
    return res.data
  },

  async addRoomPhoto(roomId, data) {
    const res = await api.post(`/venue-admin/rooms/${roomId}/fotos`, data)
    return res.data
  },

  async deletePhoto(photoId) {
    await api.delete(`/venue-admin/fotos/${photoId}`)
  }
}
