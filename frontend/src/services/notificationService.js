import api from './api'

export const notificationService = {
  getAll()         { return api.get('/reschedules/notifications') },
  getUnreadCount() { return api.get('/reschedules/notifications/count') },
  markRead(id)     { return api.patch(`/reschedules/notifications/${id}/read`) },
  markAllRead()    { return api.patch('/reschedules/notifications/read-all') },
}
