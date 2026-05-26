import api from './api'

export const reviewService = {
  getStudentEligible() {
    return api.get('/reviews/eligible/student')
  },
  getTeacherEligible() {
    return api.get('/reviews/eligible/teacher')
  },
  createStudentClassReview(payload) {
    return api.post('/reviews', { ...payload, targetType: 'CLASS' })
  },
  createTeacherVenueReview(payload) {
    return api.post('/reviews', { ...payload, targetType: 'VENUE' })
  },
  createTeacherStudentReview(payload) {
    return api.post('/reviews', { ...payload, targetType: 'STUDENT' })
  },
}
