import { supabase, currentUserId, invokeFunction, camelize } from './supabase'

export default {
  async propose(classId, proposedTime, reason) {
    const data = await invokeFunction('propose-reschedule', { body: { classId, proposedTime, reason } })
    return { data }
  },

  async teacherDecision(rescheduleId, accepted) {
    const data = await invokeFunction('teacher-decision', { body: { rescheduleId, accepted } })
    return { data }
  },

  async studentDecision(rescheduleId, accepted) {
    const data = await invokeFunction('student-decision', { body: { rescheduleId, accepted } })
    return { data }
  },

  async getReschedule(rescheduleId) {
    const { data, error } = await supabase
      .from('reschedules').select('*, class:classes(*)').eq('id', rescheduleId).maybeSingle()
    if (error) throw error
    return { data: camelize(data) }
  },

  async getStudentResponses(rescheduleId) {
    const { data, error } = await supabase
      .from('reschedule_responses').select('*').eq('reschedule_id', rescheduleId)
    if (error) throw error
    return { data: camelize(data) }
  },

  async getByClass(classId) {
    const { data, error } = await supabase
      .from('reschedules').select('*').eq('class_id', classId)
      .order('created_at', { ascending: false })
    if (error) throw error
    return { data: camelize(data) }
  },

  getAvailableSlots() {
    throw { response: { status: 501, data: { message: 'getAvailableSlots requiere una Edge Function/RPC aún no migrada' } } }
  },

  // Notificaciones (misma tabla que notificationService)
  async getNotifications() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('notifications').select('*').eq('user_id', uid)
      .order('created_at', { ascending: false })
    if (error) throw error
    return { data: camelize(data) }
  },
  async getUnreadCount() {
    const uid = await currentUserId()
    const { count, error } = await supabase
      .from('notifications').select('*', { count: 'exact', head: true })
      .eq('user_id', uid).eq('read', false)
    if (error) throw error
    return { data: count ?? 0 }
  },
  async markRead(notificationId) {
    const { error } = await supabase.from('notifications').update({ read: true }).eq('id', notificationId)
    if (error) throw error
    return { data: {} }
  },
  async markAllRead() {
    const uid = await currentUserId()
    const { error } = await supabase
      .from('notifications').update({ read: true }).eq('user_id', uid).eq('read', false)
    if (error) throw error
    return { data: {} }
  }
}
