import { supabase, currentUserId, camelize } from './supabase'

// Devuelve el shape { data } que esperan los consumidores (antes axios response).
export const notificationService = {
  async getAll() {
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
  async markRead(id) {
    const { error } = await supabase.from('notifications').update({ read: true }).eq('id', id)
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

export default notificationService
