import api from './api'

export default {
  propose(classId, proposedTime, reason) {
    return api.post('/reschedules', { classId, proposedTime, reason })
  },

  teacherDecision(rescheduleId, accepted) {
    return api.post('/reschedules/teacher-decision', { rescheduleId, accepted })
  },

  studentDecision(rescheduleId, accepted) {
    return api.post('/reschedules/student-decision', { rescheduleId, accepted })
  },

  getReschedule(rescheduleId) {
    return api.get(`/reschedules/${rescheduleId}`)
  },

  getByClass(classId) {
    return api.get(`/reschedules/class/${classId}`)
  },

  getNotifications() {
    return api.get('/reschedules/notifications')
  },

  getUnreadCount() {
    return api.get('/reschedules/notifications/count')
  },

  markRead(notificationId) {
    return api.patch(`/reschedules/notifications/${notificationId}/read`)
  },

  markAllRead() {
    return api.patch('/reschedules/notifications/read-all')
  },
}
