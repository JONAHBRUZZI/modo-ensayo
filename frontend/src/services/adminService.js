import { supabase, invokeFunction, camelize } from './supabase'

export default {
  async getStats() {
    return invokeFunction('admin-stats')
  },

  async getIdentityVerifications() {
    const { data, error } = await supabase
      .from('identity_verifications').select('*')
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  },

  async reviewIdentity(id, action) {
    const approved = action === 'approve' || action === 'APPROVED' || action === 'APROBADO'
    const status = approved ? 'APPROVED' : 'REJECTED'
    const { data: idver, error } = await supabase
      .from('identity_verifications').update({ status }).eq('id', id).select('user_id').single()
    if (error) throw error
    const { error: profErr } = await supabase
      .from('profiles')
      .update({
        identidad_validada: approved,
        identidad_estado: approved ? 'APROBADO' : 'RECHAZADO'
      })
      .eq('id', idver.user_id)
    if (profErr) throw profErr
    return { status }
  },

  async getPendingVenues() {
    const { data, error } = await supabase
      .from('venues').select('*').eq('status', 'PENDIENTE_APROBACION')
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  },

  async getAllVenues() {
    const { data, error } = await supabase
      .from('venues').select('*').order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  },

  approveVenue(id) {
    return invokeFunction('admin-approve-venue', { body: { venueId: id, action: 'approve' } })
  },

  rejectVenue(id, motivo) {
    return invokeFunction('admin-approve-venue', { body: { venueId: id, action: 'reject', reason: motivo } })
  },

  // Alterna APROBADA <-> SUSPENDIDA (update directo permitido por RLS al ADMIN).
  async toggleVenue(id, motivo) {
    const { data: venue, error: getErr } = await supabase
      .from('venues').select('status').eq('id', id).single()
    if (getErr) throw getErr
    const next = venue.status === 'SUSPENDIDA' ? 'APROBADA' : 'SUSPENDIDA'
    const patch = { status: next }
    if (next === 'SUSPENDIDA') patch.rejection_reason = motivo || ''
    const { data, error } = await supabase
      .from('venues').update(patch).eq('id', id).select('*').single()
    if (error) throw error
    return camelize(data)
  },

  // ── Gestión de usuarios (Edge Function admin-users, requiere Admin API) ──
  async getUsers() {
    return invokeFunction('admin-users', { body: { action: 'list' } })
  },

  async assignRole(userId, roleName) {
    return invokeFunction('admin-users', { body: { action: 'assignRole', userId, role: roleName } })
  },

  async revokeRole(userId, roleName) {
    return invokeFunction('admin-users', { body: { action: 'revokeRole', userId, role: roleName } })
  },

  async toggleUser(userId) {
    return invokeFunction('admin-users', { body: { action: 'toggleUser', userId } })
  },

  async deleteUser(userId) {
    return invokeFunction('admin-users', { body: { action: 'deleteUser', userId } })
  }
}
