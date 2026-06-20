import { supabase, currentUserId, camelize } from './supabase'

export default {
  async getAssociates() {
    const uid = await currentUserId()
    const { data, error } = await supabase
      .from('associates').select('*').eq('owner_id', uid)
      .order('created_at', { ascending: false })
    if (error) throw error
    return camelize(data)
  },

  async createAssociate(data) {
    const uid = await currentUserId()
    const { data: row, error } = await supabase
      .from('associates')
      .insert({
        owner_id: uid,
        name: data.name,
        email: data.email ?? null,
        relationship: data.relationship ?? null,
        birth_date: data.birthDate ?? null,
        rut: data.rut ?? null
      })
      .select('*').single()
    if (error) throw error
    return camelize(row)
  },

  async deleteAssociate(id) {
    const { error } = await supabase.from('associates').delete().eq('id', id)
    if (error) throw error
  }
}
