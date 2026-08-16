import { supabase, currentUserId, camelize } from './supabase'

// 23514 = check_violation (constraints chk_profiles_rut / chk_profiles_phone /
// chk_refund_methods_rut / chk_identity_verifications_document_number).
// Traduce el error crudo de Postgres a un mensaje entendible para el usuario.
function friendlyCheckViolation(error, message) {
  if (error?.code === '23514') throw { response: { status: 422, data: { message } }, message }
  throw error
}

export default {
  async getProfile() {
    const { data: { session } } = await supabase.auth.getSession()
    if (!session) throw new Error('No autenticado')
    const uid = session.user.id
    const { data: profile, error } = await supabase
      .from('profiles').select('*').eq('id', uid).maybeSingle()
    if (error) throw error
    return {
      id: uid,
      email: session.user.email,
      fullName: profile?.full_name,
      socialName: profile?.social_name,
      phone: profile?.phone,
      roles: session.user.app_metadata?.roles ?? ['USER'],
      identidadValidada: profile?.identidad_validada ?? false,
      identidadEstado: profile?.identidad_estado ?? 'SIN_VALIDAR'
    }
  },

  async updateProfile(data) {
    const uid = await currentUserId()
    const patch = {}
    if (data.fullName !== undefined) patch.full_name = data.fullName
    if (data.socialName !== undefined) patch.social_name = data.socialName
    if (data.phone !== undefined) patch.phone = data.phone
    const { data: row, error } = await supabase
      .from('profiles').update(patch).eq('id', uid).select('*').single()
    if (error) friendlyCheckViolation(error, 'El teléfono o RUT ingresado no tiene un formato válido.')
    return row
  },

  async getRefundMethods() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('refund_methods').select('*').eq('user_id', uid)
    if (error) throw error
    return camelize(data)
  },

  async createRefundMethod(data) {
    const uid = await currentUserId()
    const { data: row, error } = await supabase
      .from('refund_methods')
      .insert({
        user_id: uid,
        bank: data.bank ?? null,
        account_type: data.accountType ?? data.account_type ?? null,
        account_number: data.accountNumber ?? data.account_number ?? null,
        account_holder: data.accountHolder ?? data.account_holder ?? null,
        rut: data.rut ?? null
      })
      .select('*').single()
    if (error) friendlyCheckViolation(error, 'El RUT ingresado no es válido.')
    return camelize(row)
  },

  async deleteRefundMethod(id) {
    const { error } = await supabase.from('refund_methods').delete().eq('id', id)
    if (error) throw error
  },

  async getIdentityVerification() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('identity_verifications').select('*').eq('user_id', uid)
      .order('created_at', { ascending: false }).limit(1).maybeSingle()
    if (error) throw error
    return camelize(data)
  },

  // ¿El RUT ya está en uso por otra cuenta? RPC seguro (SECURITY DEFINER) que
  // solo devuelve booleano; no expone de quién es (RLS impide leer RUT ajenos).
  async rutYaRegistrado(rut) {
    const { data, error } = await supabase.rpc('rut_ya_registrado', { p_rut: rut })
    if (error) throw error
    return !!data
  },

  async uploadIdentityDocument(documentUrl, formData) {
    const uid = await currentUserId()
    const { data: row, error } = await supabase
      .from('identity_verifications')
      .insert({
        user_id: uid,
        document_url: documentUrl,
        document_type: formData?.documentType || null,
        document_number: formData?.documentNumber || null,
        full_name: formData?.fullName || null,
        birth_date: formData?.birthDate || null,
        status: 'PENDING'
      })
      .select('*').single()
    if (error) friendlyCheckViolation(error, 'El número de documento ingresado no es un RUT válido.')
    // Refleja "en revisión" en el perfil.
    await supabase.from('profiles').update({ identidad_estado: 'PENDIENTE' }).eq('id', uid)
    return row
  },

  deleteIdentityDocument() {
    throw { response: { status: 501, data: { message: 'deleteIdentityDocument pendiente: falta policy DELETE en identity_verifications' } } }
  },

  // Supabase no verifica la contraseña actual; updateUser exige sesión reciente.
  async changePassword(newPassword) {
    const { error } = await supabase.auth.updateUser({ password: newPassword })
    if (error) throw { response: { status: error.status || 400, data: { message: error.message } } }
    return { status: 'ok' }
  },

  async getProfessionalProfile() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('professional_profiles').select('*').eq('id', uid).maybeSingle()
    if (error) throw error
    return camelize(data)
  },

  // Estadísticas del alumno (reemplaza /users/me/stats): calculadas desde enrollments.
  async getStudentStats() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('enrollments')
      .select('status, class:classes(start_time)')
      .eq('student_id', uid).eq('status', 'ACTIVE')
    if (error) throw error
    const rows = data || []
    const now = new Date().toISOString()
    return {
      totalClases: rows.length,
      próximas: rows.filter((r) => r.class?.start_time && r.class.start_time > now).length
    }
  }
}
