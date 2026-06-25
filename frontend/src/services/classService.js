import { supabase, currentUserId, invokeFunction, camelize } from './supabase'

const CLASS_WITH_RELATIONS = '*, room:rooms(*, venue:venues(*))'

function mapClassBody(data) {
  return {
    title: data.title,
    discipline: data.discipline ?? null,
    disciplineCategory: data.disciplineCategory ?? data.category ?? data.discipline_category ?? null,
    level: data.level || undefined,
    description: data.description ?? undefined,
    capacity: data.capacity,
    duration: data.duration,
    price: data.price,
    minAge: data.minAge ?? undefined,
    maxAge: data.maxAge ?? undefined,
    // El input datetime-local entrega 'YYYY-MM-DDTHH:mm' (sin segundos ni zona);
    // se normaliza a ISO completo porque la Edge Function valida con z.datetime().
    startTime: data.startTime ? new Date(data.startTime).toISOString() : undefined,
    roomId: data.roomId ?? undefined
  }
}

export default {
  async getClasses(params = {}) {
    let q = supabase.from('classes').select(CLASS_WITH_RELATIONS).eq('status', 'PUBLISHED')
    if (params.discipline) q = q.eq('discipline', params.discipline)
    if (params.level) q = q.eq('level', params.level)
    const { data, error } = await q.order('start_time', { ascending: true })
    if (error) throw error
    return camelize(data)
  },

  async getClassById(id) {
    const { data, error } = await supabase
      .from('classes').select(CLASS_WITH_RELATIONS).eq('id', id).maybeSingle()
    if (error) throw error
    return camelize(data)
  },

  async createClass(data) {
    return invokeFunction('create-class', { body: { ...mapClassBody(data), draft: false } })
  },

  async createBorrador(data) {
    return invokeFunction('create-class', { body: { ...mapClassBody(data), draft: true } })
  },

  async getTeacherClasses() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('classes').select(CLASS_WITH_RELATIONS).eq('teacher_id', uid)
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  },

  async getTeacherPropias() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('classes').select(CLASS_WITH_RELATIONS).eq('teacher_id', uid).eq('tipo_clase', 'PROPIA')
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  },

  async getTeacherAsignadas() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('classes').select(CLASS_WITH_RELATIONS).eq('teacher_id', uid).eq('tipo_clase', 'ASIGNADA')
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  },

  async getTeacherDrafts() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('classes').select(CLASS_WITH_RELATIONS).eq('teacher_id', uid).eq('status', 'DRAFT')
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  },

  getMyClasses() {
    return this.getTeacherClasses()
  },

  async getVenues() {
    const { data, error } = await supabase
      .from('venues').select('*').eq('status', 'APROBADA').order('name', { ascending: true })
    if (error) throw error
    return camelize(data)
  },

  async getVenueRooms(venueId) {
    const { data, error } = await supabase.from('rooms').select('*').eq('venue_id', venueId)
    if (error) throw error
    return camelize(data)
  },

  async markAttendance(classId, data) {
    const { data: row, error } = await supabase
      .from('attendances')
      .insert({
        class_id: classId,
        beneficiary_id: data.beneficiaryId,
        beneficiary_type: data.beneficiaryType || 'SELF',
        present: data.present
      })
      .select('*').single()
    if (error) throw error
    return camelize(row)
  },

  async getClassAttendance(classId) {
    const { data, error } = await supabase.from('attendances').select('*').eq('class_id', classId)
    if (error) throw error
    return camelize(data)
  },

  async getTeacherEarnings() {
    const { data, error } = await supabase
      .from('payments')
      .select('amount, status, created_at, enrollment:enrollments(class:classes(title, teacher_id))')
      .order('created_at', { ascending: false })
    if (error) throw error
    const pagos = camelize(data || [])
    const sum = (pred) => pagos.filter(pred).reduce((s, p) => s + Number(p.amount || 0), 0)
    const startMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString()
    return {
      resumen: {
        totalRetenido: sum((p) => p.status === 'RETAINED'),
        totalLiberadoMes: sum((p) => p.status === 'RELEASED' && (p.createdAt || '') >= startMonth),
        totalLiberadoAcumulado: sum((p) => p.status === 'RELEASED')
      },
      pagos
    }
  },

  async getProfesorClase(classId) {
    const { data, error } = await supabase
      .from('classes').select(CLASS_WITH_RELATIONS).eq('id', classId).maybeSingle()
    if (error) throw error
    return camelize(data)
  },

  // Catálogo de disciplinas agrupado: [{ category, label, items: [nombre,...] }]
  async getDisciplines() {
    const { data, error } = await supabase
      .from('discipline_catalog').select('name, category, sort_order')
      .eq('active', true)
      .order('category', { ascending: true }).order('sort_order', { ascending: true })
    if (error) throw error
    const groups = new Map()
    for (const row of data || []) {
      const key = row.category || 'Otras'
      if (!groups.has(key)) groups.set(key, { category: row.category, label: key, items: [] })
      groups.get(key).items.push(row.name)
    }
    return Array.from(groups.values())
  },

  async deleteDraft(id) {
    const { error } = await supabase.from('classes').delete().eq('id', id)
    if (error) throw error
  },

  async assignReserva(classId, { roomId, startTime, duration }) {
    return invokeFunction('assign-reserva', { body: { classId, roomId, startTime, duration } })
  },

  // Publicar un borrador: conserva sala/horario del DRAFT, actualiza datos y pasa a PUBLISHED.
  async publishClass(id, fields) {
    const patch = {
      title: fields.title,
      discipline: fields.discipline ?? null,
      level: fields.level,
      description: fields.description ?? null,
      capacity: fields.capacity,
      duration: fields.duration,
      price: fields.price,
      min_age: fields.minAge ?? null,
      max_age: fields.maxAge ?? null,
      status: 'PUBLISHED'
    }
    const { data, error } = await supabase
      .from('classes').update(patch).eq('id', id).select(CLASS_WITH_RELATIONS).single()
    if (error) throw error
    return camelize(data)
  },

  // Reservas (bloques ocupados) que aún no tienen borrador asociado.
  // Pendiente de lógica server-side fiel; se devuelve vacío para no romper la vista.
  async getReservasSinBorrador() {
    return []
  },

  getTeacherMetrics() {
    throw { response: { status: 501, data: { message: 'getTeacherMetrics requiere una Edge Function/RPC aún no migrada' } } }
  }
}
