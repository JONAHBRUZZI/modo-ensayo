import { supabase, invokeFunction, camelize } from './supabase'

export default {
  async getSchedule(venueId) {
    const { data, error } = await supabase
      .from('venue_schedules').select('*').eq('venue_id', venueId)
    if (error) throw error
    return camelize(data)
  },

  async saveSchedule(venueId, schedules) {
    // Reemplaza el horario completo de la sede (igual que el PUT del backend).
    const { error: delErr } = await supabase.from('venue_schedules').delete().eq('venue_id', venueId)
    if (delErr) throw delErr
    const rows = (schedules || []).map((s) => ({
      venue_id: venueId,
      day_of_week: s.dayOfWeek || s.day_of_week,
      open_time: s.openTime || s.open_time,
      close_time: s.closeTime || s.close_time
    }))
    if (rows.length === 0) return []
    const { data, error } = await supabase.from('venue_schedules').insert(rows).select('*')
    if (error) throw error
    return camelize(data)
  },

  async getBlockConfig(venueId) {
    const { data, error } = await supabase
      .from('venue_block_configs').select('*').eq('venue_id', venueId).maybeSingle()
    if (error) throw error
    return camelize(data)
  },

  async saveBlockConfig(venueId, config) {
    const row = {
      venue_id: venueId,
      block_duration_min: config.blockDurationMin ?? config.block_duration_min ?? 60,
      gap_between_blocks_min: config.gapBetweenBlocksMin ?? config.gap_between_blocks_min ?? 15
    }
    const { data, error } = await supabase
      .from('venue_block_configs').upsert(row, { onConflict: 'venue_id' }).select('*').single()
    if (error) throw error
    return camelize(data)
  },

  async generateBlocks() {
    // La Edge Function regenera todos los bloques (RPC regenerate_schedule_blocks).
    return invokeFunction('generate-blocks')
  },

  async getRoomSchedule(roomId, from, to) {
    let q = supabase.from('room_schedule_blocks').select('*').eq('room_id', roomId)
    if (from) q = q.gte('start_time', from)
    if (to) q = q.lte('end_time', to)
    const { data, error } = await q.order('start_time', { ascending: true })
    if (error) throw error
    return camelize(data)
  },

  async markMaintenance(roomId, blockId, reason) {
    const { error: updErr } = await supabase
      .from('room_schedule_blocks').update({ status: 'MAINTENANCE' }).eq('id', blockId)
    if (updErr) throw updErr
    const { data: block } = await supabase
      .from('room_schedule_blocks').select('start_time, end_time').eq('id', blockId).single()
    const { error } = await supabase.from('room_maintenances').insert({
      room_id: roomId,
      start_time: block?.start_time,
      end_time: block?.end_time,
      reason: reason ?? null
    })
    if (error) throw error
    return { status: 'ok' }
  },

  async releaseMaintenance(blockId) {
    const { error } = await supabase
      .from('room_schedule_blocks').update({ status: 'AVAILABLE' }).eq('id', blockId)
    if (error) throw error
    return { status: 'ok' }
  },

  async searchAvailableRooms(from, to) {
    let q = supabase
      .from('room_schedule_blocks')
      .select('*, room:rooms(*, venue:venues(*))')
      .eq('status', 'AVAILABLE')
    if (from) q = q.gte('start_time', from)
    if (to) q = q.lte('end_time', to)
    const { data, error } = await q.order('start_time', { ascending: true })
    if (error) throw error
    return camelize(data)
  },

  async bookSlot(roomId, blockId, classId) {
    return invokeFunction('book-slot', { body: { blockId, classId: classId || null } })
  },

  getUserCalendar() {
    throw { response: { status: 501, data: { message: 'getUserCalendar requiere una Edge Function/RPC aún no migrada' } } }
  }
}
