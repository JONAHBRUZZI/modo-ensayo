import { supabase, currentUserId } from './supabase'

// professional_profiles: PK = id (= auth.users.id). RLS pp_select_public / pp_*_own.
function toCamel(p) {
  if (!p) return null
  return {
    id: p.id,
    description: p.description,
    photoUrl: p.photo_url,
    averageRating: p.average_rating,
    specialty: p.specialty,
    experienceYears: p.experience_years,
    especialidad: p.especialidad,
    nivelEnsenanza: p.nivel_ensenanza,
    formacion: p.formacion,
    instagram: p.instagram,
    youtube: p.youtube,
    sitioWeb: p.sitio_web,
    linkedin: p.linkedin,
    biografia: p.biografia,
    disciplinaPrincipal: p.disciplina_principal,
    disciplinasSecundarias: p.disciplinas_secundarias || [],
    tipoFormacion: p.tipo_formacion || [],
    detalleFormacion: p.detalle_formacion
  }
}

function toSnake(id, d) {
  return {
    id,
    description: d.description ?? null,
    especialidad: d.especialidad ?? null,
    nivel_ensenanza: d.nivelEnsenanza ?? null,
    experience_years: d.experienceYears ?? null,
    formacion: d.formacion ?? null,
    biografia: d.biografia ?? null,
    disciplina_principal: d.disciplinaPrincipal ?? null,
    disciplinas_secundarias: Array.isArray(d.disciplinasSecundarias) ? d.disciplinasSecundarias : [],
    tipo_formacion: Array.isArray(d.tipoFormacion) ? d.tipoFormacion : [],
    detalle_formacion: d.detalleFormacion ?? null,
    instagram: d.instagram ?? null,
    youtube: d.youtube ?? null,
    sitio_web: d.sitioWeb ?? null,
    linkedin: d.linkedin ?? null
  }
}

export default {
  async getMine() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('professional_profiles').select('*').eq('id', uid).maybeSingle()
    if (error) throw error
    return toCamel(data)
  },

  async save(form) {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('professional_profiles')
      .upsert(toSnake(uid, form), { onConflict: 'id' })
      .select('*').single()
    if (error) throw error
    return toCamel(data)
  }
}
