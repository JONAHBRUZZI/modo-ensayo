import { supabase, invokeFunction, camelize } from './supabase'

export default {
  // Estadísticas del panel admin. Se calculan con consultas directas: el admin
  // tiene acceso a estas tablas vía las políticas RLS (has_role('ADMIN')), así que
  // no dependemos de la Edge Function admin-stats (que requiere despliegue manual).
  async getStats() {
    // Cuenta filas de una tabla aplicando un filtro opcional. Devuelve 0 si falla.
    const contar = async (tabla, filtro) => {
      let q = supabase.from(tabla).select('*', { count: 'exact', head: true })
      if (filtro) q = filtro(q)
      const { count, error } = await q
      if (error) { console.error(`getStats: error contando ${tabla}`, error.message); return 0 }
      return count ?? 0
    }

    const [usuarios, usuariosPendientes, sedes, sedesPendientes, pendientes, totalClases, clasesRealizadas] =
      await Promise.all([
        contar('profiles'),
        contar('profiles', (q) => q.eq('identidad_validada', false)),
        contar('venues'),
        contar('venues', (q) => q.eq('status', 'PENDIENTE_APROBACION')),
        contar('identity_verifications', (q) => q.eq('status', 'PENDING')),
        contar('classes'),
        contar('classes', (q) => q.eq('status', 'COMPLETED'))
      ])

    // Sedes agrupadas por estado (gráfico de barras).
    const sedesPorEstado = {}
    try {
      const { data } = await supabase.from('venues').select('status')
      for (const v of data || []) sedesPorEstado[v.status] = (sedesPorEstado[v.status] || 0) + 1
    } catch { /* sin datos */ }

    // Ingresos mensuales liberados (gráfico de línea; degrada si no hay datos).
    const ingresosMensuales = []
    try {
      const { data } = await supabase.from('payments').select('amount, created_at').eq('status', 'RELEASED')
      const porMes = {}
      for (const p of data || []) {
        const mes = (p.created_at || '').substring(0, 7) // YYYY-MM
        if (mes) porMes[mes] = (porMes[mes] || 0) + Number(p.amount || 0)
      }
      for (const mes of Object.keys(porMes).sort()) ingresosMensuales.push({ mes, ingresos: porMes[mes] })
    } catch { /* sin datos */ }

    // Usuarios por rol (gráfico circular). Los roles viven en auth.users, así que
    // se obtienen vía la Edge Function admin-users (que usa la Admin API).
    const usuariosPorRol = {}
    try {
      const lista = await this.getUsers()
      const arr = Array.isArray(lista) ? lista : (lista?.users || lista?.data || [])
      for (const u of arr) {
        const roles = u.roles || u.appMetadata?.roles || ['USER']
        for (const r of roles) usuariosPorRol[r] = (usuariosPorRol[r] || 0) + 1
      }
    } catch { /* sin datos */ }

    return {
      usuarios,
      usuariosPendientes,
      sedes,
      sedesPendientes,
      pendientes,
      totalClases,
      clasesRealizadas,
      sedesPorEstado,
      ingresosMensuales,
      usuariosPorRol
    }
  },

  async getIdentityVerifications() {
    const { data, error } = await supabase
      .from('identity_verifications').select('*')
      .eq('status', 'PENDING')
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
