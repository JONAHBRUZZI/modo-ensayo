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

  async getTeacherDrafts() {
    // Usa /profesor/clases/borradores (accessible sin rol TEACHER, para usuarios que aun no publicaron)
    const res = await api.get('/profesor/clases/borradores')
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
    const res = await api.get('/profesor/pagos')
    return res.data  // { resumen: { totalRetenido, totalLiberadoMes, totalLiberadoAcumulado }, pagos: [...] }
  },

  async createBorrador(data) {
    const res = await api.post('/profesor/clases/borrador', data)
    return res.data
  },

  async getProfesorClase(classId) {
    const res = await api.get(`/classes/${classId}/profesor`)
    return res.data
  }
}
