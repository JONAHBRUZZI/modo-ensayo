import api from './api'

export const classService = {
  async listPublished() {
    const { data } = await api.get('/classes')
    return data
  },
  async createClass(request) {
    const { data } = await api.post('/classes', request)
    return data
  },
  async getMyClasses() {
    const { data } = await api.get('/teacher/classes')
    return data
  },
  async getVenues() {
    const { data } = await api.get('/venues')
    return data
  },
  async getVenueRooms(venueId) {
    const { data } = await api.get(`/venues/${venueId}/rooms`)
    return data
  },
  async getRoomAvailability(roomId, afterTime) {
    const params = afterTime ? { afterTime } : {}
    const { data } = await api.get(`/venues/rooms/${roomId}/availability`, { params })
    return data
  },
  async markAttendance(request) {
    const { data } = await api.post('/attendance', request)
    return data
  },
  async getClassAttendance(classId) {
    const { data } = await api.get(`/attendance/class/${classId}`)
    return data
  },
}
