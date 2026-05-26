import api from './api'

export default {
  async getClasses(params) {
    const res = await api.get('/classes', { params })
    return res.data
  },

  async getClassById(id) {
    const res = await api.get(`/classes/${id}`)
    return res.data
  },

  async createClass(data) {
    const res = await api.post('/classes', data)
    return res.data
  },

  async getTeacherClasses() {
    const res = await api.get('/teacher/classes')
    return res.data
  },

  async getTeacherPropias() {
    const res = await api.get('/teacher/classes/propias')
    return res.data
  },

  async getTeacherAsignadas() {
    const res = await api.get('/teacher/classes/asignadas')
    return res.data
  },

  async getMyClasses() {
    const res = await api.get('/teacher/classes')
    return res.data
  },

  async getVenues() {
    const res = await api.get('/venues')
    return res.data
  },

  async getVenueRooms(venueId) {
    const res = await api.get(`/venues/${venueId}/rooms`)
    return res.data
  },

  async getRoomAvailability(roomId, afterTime) {
    const res = await api.get(`/venues/rooms/${roomId}/availability`, {
      params: { afterTime }
    })
    return res.data
  },

  async markAttendance(classId, data) {
    const res = await api.post('/attendance', { classId, ...data })
    return res.data
  },

  async getClassAttendance(classId) {
    const res = await api.get(`/attendance/class/${classId}`)
    return res.data
  },

  async getTeacherMetrics() {
    const res = await api.get('/teacher/metrics')
    return res.data
  },

  async getTeacherEarnings() {
    const res = await api.get('/teacher/earnings')
    return res.data
  }
}
