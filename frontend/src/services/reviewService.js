import api from './api'

export const reviewService = {
  getStudentEligible() {
    return api.get('/reviews/eligible/student')
  },
  getMine() {
    return api.get('/reviews/mine')
  },
  getRecent() {
    return api.get('/reviews/recent')
  },
  getAboutMe() {
    return api.get('/reviews/about-me')
  },
  getEligibleTargets() {
    return api.get('/reviews/eligible/targets')
  },
  getMySystemReview() {
    return api.get('/reviews/system/mine')
  },
  getSystemReviews() {
    return api.get('/reviews/system')
  },
  create(payload) {
    return api.post('/reviews', payload)
  },
  getTeacherEligible() {
    return api.get('/reviews/eligible/teacher')
  },
  getByClass(classId) {
    return api.get(`/reviews/class/${classId}`)
  },
  getByTeacher(teacherId) {
    return api.get(`/reviews/teacher/${teacherId}`)
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
