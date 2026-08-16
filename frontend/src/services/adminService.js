import { supabase, invokeFunction, camelize } from './supabase'

export default {
  // Estadísticas del panel admin. Se calculan con consultas directas: el admin
  // tiene acceso a estas tablas vía las políticas RLS (has_role('ADMIN')), así que
  // no dependemos de la Edge Function admin-stats (que requiere despliegue manual).
  async getStats(granularidad = 'month') {
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

    // Ingresos por fuente y período (clases = payments RELEASED; arriendos =
    // payment_sessions ROOM_RESERVATION aprobadas). granularidad: 'month' | 'year'.
    const ingresosPorPeriodo = []
    const periodoDe = (fecha) => (fecha || '').substring(0, granularidad === 'year' ? 4 : 7)
    const porPeriodo = {}
    const acumular = (per, campo, monto) => {
      if (!per) return
      porPeriodo[per] = porPeriodo[per] || { clases: 0, arriendos: 0 }
      porPeriodo[per][campo] += Number(monto || 0)
    }
    try {
      const { data } = await supabase.from('payments').select('amount, created_at').eq('status', 'RELEASED')
      for (const p of data || []) acumular(periodoDe(p.created_at), 'clases', p.amount)
    } catch { /* sin datos */ }
    try {
      const { data } = await supabase.from('payment_sessions')
        .select('cart_snapshot, processed_at, created_at').eq('status', 'APPROVED')
      for (const ps of data || []) {
        const snap = ps.cart_snapshot || {}
        if (snap.type !== 'ROOM_RESERVATION') continue
        acumular(periodoDe(ps.processed_at || ps.created_at), 'arriendos', snap.amount)
      }
    } catch { /* sin datos / sin permiso */ }
    for (const per of Object.keys(porPeriodo).sort()) {
      const { clases, arriendos } = porPeriodo[per]
      ingresosPorPeriodo.push({ periodo: per, clases, arriendos, total: clases + arriendos })
    }

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
      ingresosPorPeriodo,
      usuariosPorRol
    }
  },

  // Comisiones configurables (app_settings). El admin las lee y edita vía las
  // policies has_role('ADMIN'). value es jsonb: guardamos/leemos un número.
  async getSettings() {
    const keys = ['room_reservation_commission_pct', 'marketplace_commission_pct', 'payout_cutoff_day']
    const { data, error } = await supabase.from('app_settings').select('key, value').in('key', keys)
    if (error) throw error
    const out = {}
    for (const row of data || []) out[row.key] = Number(row.value)
    return out
  },

  async updateSetting(key, value) {
    const { error } = await supabase.from('app_settings').update({ value }).eq('key', key)
    if (error) throw error
    return { status: 'ok' }
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
    if (error) {
      // 23505 = violación del índice único identity_verifications_document_approved_unique (R05)
      if (error.code === '23505') {
        const message = 'Este documento de identidad ya está aprobado en otra cuenta.'
        throw { response: { status: 409, data: { message } }, message }
      }
      throw error
    }
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

  async getVenueDocuments(venueId) {
    const { data, error } = await supabase
      .from('venue_documents').select('*').eq('venue_id', venueId)
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  },

  async reviewDocument(docId, action, motivo = null) {
    const estado = action === 'approve' ? 'APROBADO' : 'RECHAZADO'
    const patch = { estado }
    if (estado === 'RECHAZADO' && motivo) patch.motivo_rechazo = motivo
    const { data, error } = await supabase
      .from('venue_documents').update(patch).eq('id', docId).select('*').single()
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
  },

  // ── Panel de pagos (Edge Function admin-payments, requiere service_role) ──
  // Giros pendientes a profesores + reembolsos fallidos.
  async getPayments() {
    return invokeFunction('admin-payments', { body: { action: 'list' } })
  },

  // Finanzas del ciclo de corte: costo real MP vs. comisión cobrada (margen).
  // month opcional 'YYYY-MM'; por defecto el ciclo actual.
  async getFinance(month) {
    return invokeFunction('admin-payments', { body: { action: 'finance', ...(month ? { month } : {}) } })
  },

  async markPayoutPaid(payoutId, reference) {
    return invokeFunction('admin-payments', { body: { action: 'markPayoutPaid', payoutId, ...(reference ? { reference } : {}) } })
  },

  async retryRefund(paymentId) {
    return invokeFunction('admin-payments', { body: { action: 'retryRefund', paymentId } })
  },

  async markRefundResolved(paymentId) {
    return invokeFunction('admin-payments', { body: { action: 'markRefundResolved', paymentId } })
  },

  // Métricas de rendimiento (M1/M2/M3/M5) globales y por sede. EF admin-metrics.
  async getMetrics() {
    return invokeFunction('admin-metrics', { body: {} })
  },

  // Métricas de comportamiento desde Google Analytics 4. EF ga-metrics.
  // Devuelve { configured, activeNow, usuarios7d, sesiones7d, vistas7d, topPages }.
  async getAnalytics() {
    return invokeFunction('ga-metrics', { body: {} })
  }
}
