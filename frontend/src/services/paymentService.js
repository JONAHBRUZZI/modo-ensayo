import { supabase, currentUserId, invokeFunction, camelize } from './supabase'
import { attachTeacherNames } from './classService'
import associateService from './associateService'

export default {
  async addToCart(classId, beneficiaryType = 'SELF', beneficiaryId = null) {
    // Get the authenticated user's ID directly from Supabase session
    // (guarantees consistency with the JWT that PostgREST validates for RLS)
    const { data: { session } } = await supabase.auth.getSession()
    if (!session?.user?.id) throw { message: 'No autenticado', response: { status: 401 } }
    const uid = session.user.id

    const { data: cls, error: clsErr } = await supabase
      .from('classes').select('title, discipline, level, price').eq('id', classId).single()
    if (clsErr) throw clsErr

    const row = {
      owner_id: uid,
      class_id: classId,
      class_title: cls.title,
      discipline: cls.discipline,
      level: cls.level,
      price: cls.price,
      // beneficiary_type es NOT NULL en la BD (default 'SELF'); nunca enviar null
      beneficiary_type: beneficiaryType || 'SELF'
    }
    // Only include beneficiary_id if it has a value (avoid sending null for uuid columns)
    if (beneficiaryId) row.beneficiary_id = beneficiaryId

    console.log('[addToCart] insertando en cart_items →', row)
    const { data, error } = await supabase
      .from('cart_items')
      .insert(row)
      .select('*').single()
    if (error) {
      console.error('[addToCart] error de Supabase →', {
        message: error.message,
        details: error.details,
        hint: error.hint,
        code: error.code
      })
      throw error
    }
    console.log('[addToCart] insertado OK →', data)
    return camelize(data)
  },

  async getCart() {
    const { data: { session } } = await supabase.auth.getSession()
    if (!session?.user?.id) {
      console.log('[getCart] sin sesión → carrito vacío')
      return { items: [] }
    }
    const uid = session.user.id
    const { data, error } = await supabase
      .from('cart_items').select('*').eq('owner_id', uid)
      .order('created_at', { ascending: false })
    if (error) {
      console.error('[getCart] error de Supabase →', { message: error.message, code: error.code })
      throw error
    }
    console.log('[getCart] items cargados →', data?.length ?? 0)
    return { items: camelize(data) }
  },

  async removeFromCart(id) {
    const { error } = await supabase.from('cart_items').delete().eq('id', id)
    if (error) throw error
  },

  // --- MercadoPago Connect (marketplace) ---

  // Estado de vinculación de la cuenta MercadoPago del gestor de sede.
  // Lee la vista segura mp_seller_status (sin exponer tokens).
  async getMpAccountStatus() {
    const uid = await currentUserId()
    if (!uid) return null
    const { data, error } = await supabase
      .from('mp_seller_status').select('*').eq('user_id', uid).maybeSingle()
    if (error) throw error
    return camelize(data)
  },

  // Inicia el OAuth de MercadoPago Connect: devuelve la URL de autorización a la
  // que se debe redirigir al gestor para vincular su cuenta.
  async startMpConnect() {
    return invokeFunction('mp-connect-start', { method: 'POST' })
  },

  // Crea la preferencia de pago (con split) para arrendar una sala. Devuelve
  // { initPoint } al que se redirige al profesor. La reserva se materializa en
  // el webhook al aprobarse el pago.
  async reserveRoomPreference(roomId, blockIds, borradorId = null, rescheduleReason = null) {
    return invokeFunction('reserve-room-preference', { body: { roomId, blockIds, borradorId, rescheduleReason } })
  },

  async createMercadoPagoPreference() {
    const { items: cart } = await this.getCart()
    const items = cart.map((c) => ({
      classId: c.classId,
      classTitle: c.classTitle,
      discipline: c.discipline || '',
      level: c.level || '',
      price: Number(c.price),
      beneficiaryType: c.beneficiaryType || 'SELF',
      beneficiaryId: c.beneficiaryId || null
    }))
    console.log('[createPreference] invocando mercadopago-create-preference con items →', items)
    try {
      const res = await invokeFunction('mercadopago-create-preference', { body: { items } })
      console.log('[createPreference] respuesta de la Edge Function →', res)
      return res
    } catch (err) {
      console.error('[createPreference] la Edge Function falló →', {
        status: err?.response?.status,
        data: err?.response?.data,
        message: err?.message
      })
      throw err
    }
  },

  async getMyEnrollments() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('enrollments')
      .select('*, class:classes(*, room:rooms(*, venue:venues(*)))')
      .eq('student_id', uid)
      .order('created_at', { ascending: false })
    if (error) throw error
    const rows = camelize(data) || []
    // El nombre del profesor se resuelve por RPC (RLS de profiles) y se anexa a
    // la clase anidada de cada inscripción.
    await attachTeacherNames(rows.map(r => r.class).filter(Boolean))
    // Mapa id → nombre de los asociados, para etiquetar a cada beneficiario.
    let asociados = {}
    try {
      const list = await associateService.getAssociates()
      for (const a of (Array.isArray(list) ? list : [])) asociados[a.id] = a.name
    } catch { /* sin asociados: los beneficiarios propios se agrupan en "Yo" */ }
    // Aplano la inscripción + su clase a la forma que consumen las vistas
    // (título, fecha, estado de la clase, sede, dirección y profesor).
    return rows.map((e) => {
      const cl = e.class || {}
      const venue = cl.room?.venue || {}
      const esPropio = !e.beneficiaryId || e.beneficiaryType === 'SELF'
      return {
        enrollmentId: e.id,
        classId: e.classId,
        enrollmentStatus: e.status,
        beneficiaryType: e.beneficiaryType,
        beneficiaryId: e.beneficiaryId,
        beneficiaryName: esPropio ? null : (asociados[e.beneficiaryId] || 'Asociado'),
        title: cl.title,
        discipline: cl.discipline,
        level: cl.level,
        description: cl.description,
        startTime: cl.startTime,
        endTime: cl.endTime,
        duration: cl.duration,
        price: cl.price,
        status: cl.status,
        minAge: cl.minAge,
        maxAge: cl.maxAge,
        roomName: cl.room?.name || null,
        venueName: venue.name || null,
        venueAddress: venue.address || null,
        venueComuna: venue.comuna || null,
        teacherName: cl.teacherName || null
      }
    })
  },

  async getMyPaymentHistory() {
    const { data, error } = await supabase
      .from('payments')
      .select('*, enrollment:enrollments(*, class:classes(title, start_time))')
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  }
}
