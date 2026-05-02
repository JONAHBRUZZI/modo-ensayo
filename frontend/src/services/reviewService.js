import api from './api'

export const reviewService = {
  getStudentEligible() {
    return api.get('/reviews/eligible/student')
  },
  getTeacherEligible() {
    return api.get('/reviews/eligible/teacher')
  },
  createStudentClassReview(payload) {
    return api.post('/reviews/student/class', payload)
  },
  createTeacherVenueReview(payload) {
    return api.post('/reviews/teacher/venue', payload)
  },
  createTeacherStudentReview(payload) {
    return api.post('/reviews/teacher/student', payload)
  },
}
