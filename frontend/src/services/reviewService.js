import { supabase, currentUserId, invokeFunction, camelize } from './supabase'

// Las vistas consumen estos métodos como respuestas axios (usan `res.data`),
// por eso devolvemos siempre { data }.
const NOT_MIGRATED = (name) => {
  throw { response: { status: 501, data: { message: `"${name}" requiere una Edge Function/RPC aún no migrada` } } }
}

export const reviewService = {
  async getMine() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('reviews').select('*').eq('reviewer_id', uid)
      .order('created_at', { ascending: false })
    if (error) throw error
    return { data: camelize(data) }
  },

  async getRecent() {
    const { data, error } = await supabase
      .from('reviews').select('*')
      .order('created_at', { ascending: false }).limit(20)
    if (error) throw error
    return { data: camelize(data) }
  },

  async getAboutMe() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('reviews').select('*').eq('target_id', uid)
      .order('created_at', { ascending: false })
    if (error) throw error
    return { data: camelize(data) }
  },

  async getByClass(classId) {
    const { data, error } = await supabase
      .from('reviews').select('*').eq('class_id', classId)
      .order('created_at', { ascending: false })
    if (error) throw error
    return { data: camelize(data) }
  },

  async getByTarget(targetType, targetId) {
    const { data, error } = await supabase
      .from('reviews').select('*').eq('target_type', targetType).eq('target_id', targetId)
      .order('created_at', { ascending: false })
    if (error) throw error
    return { data: camelize(data) }
  },

  async getByTeacher(teacherId) {
    const { data, error } = await supabase
      .from('reviews').select('*').eq('target_id', teacherId)
      .order('created_at', { ascending: false })
    if (error) throw error
    return { data: camelize(data) }
  },

  async create(payload) {
    const data = await invokeFunction('create-review', { body: payload })
    return { data }
  },

  createStudentClassReview(payload) {
    return this.create({ ...payload, targetType: 'CLASS' })
  },
  createTeacherVenueReview(payload) {
    return this.create({ ...payload, targetType: 'VENUE' })
  },
  createTeacherStudentReview(payload) {
    return this.create({ ...payload, targetType: 'STUDENT' })
  },

  // Pendientes: requieren lógica de elegibilidad / reseñas de sistema (nueva EF/RPC).
  getStudentEligible() { return NOT_MIGRATED('getStudentEligible') },
  getTeacherEligible() { return NOT_MIGRATED('getTeacherEligible') },
  getEligibleTargets() { return NOT_MIGRATED('getEligibleTargets') },
  getMySystemReview() { return NOT_MIGRATED('getMySystemReview') },
  getSystemReviews() { return NOT_MIGRATED('getSystemReviews') },
  getSystemStats() { return NOT_MIGRATED('getSystemStats') }
}

export default reviewService
