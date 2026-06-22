import { supabase, currentUserId, invokeFunction, camelize } from './supabase'

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
      beneficiary_type: beneficiaryType
    }
    // Only include beneficiary_id if it has a value (avoid sending null for uuid columns)
    if (beneficiaryId) row.beneficiary_id = beneficiaryId

    const { data, error } = await supabase
      .from('cart_items')
      .insert(row)
      .select('*').single()
    if (error) throw error
    return camelize(data)
  },

  async getCart() {
    const { data: { session } } = await supabase.auth.getSession()
    if (!session?.user?.id) return { items: [] }
    const uid = session.user.id
    const { data, error } = await supabase
      .from('cart_items').select('*').eq('owner_id', uid)
      .order('created_at', { ascending: false })
    if (error) throw error
    return { items: camelize(data) }
  },

  async removeFromCart(id) {
    const { error } = await supabase.from('cart_items').delete().eq('id', id)
    if (error) throw error
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
    return invokeFunction('mercadopago-create-preference', { body: { items } })
  },

  async getMyEnrollments() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('enrollments')
      .select('*, class:classes(*, room:rooms(*, venue:venues(*)))')
      .eq('student_id', uid)
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
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
