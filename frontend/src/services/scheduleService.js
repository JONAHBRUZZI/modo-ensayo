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

  // Mantenciones registradas de una sala en un rango (fuente de verdad de la
  // mantención: existe aunque la sala no tenga bloques de horario generados).
  async getRoomMaintenances(roomId, from, to) {
    let q = supabase.from('room_maintenances').select('*').eq('room_id', roomId)
    if (from) q = q.gte('start_time', from)
    if (to) q = q.lte('end_time', to)
    const { data, error } = await q.order('start_time', { ascending: true })
    if (error) throw error
    return camelize(data)
  },

  // Marca un horario como mantención. La mantención se registra en
  // room_maintenances (que el gestor sí puede escribir). Si además existe un
  // bloque de horario para ese tramo, se actualiza a MAINTENANCE (best-effort).
  async markMaintenance(roomId, startTime, endTime, blockId = null, reason = null) {
    const { data, error } = await supabase.from('room_maintenances').insert({
      room_id: roomId,
      start_time: startTime,
      end_time: endTime,
      reason: reason ?? 'Mantención programada'
    }).select('id').single()
    if (error) throw error
    if (blockId) {
      await supabase.from('room_schedule_blocks').update({ status: 'MAINTENANCE' }).eq('id', blockId)
    }
    return { status: 'ok', id: data?.id }
  },

  // Libera una mantención: borra el registro de room_maintenances y, si hay un
  // bloque asociado, lo devuelve a disponible.
  async releaseMaintenance(maintenanceId, blockId = null) {
    if (maintenanceId) {
      const { error } = await supabase.from('room_maintenances').delete().eq('id', maintenanceId)
      if (error) throw error
    }
    if (blockId) {
      await supabase.from('room_schedule_blocks').update({ status: 'AVAILABLE' }).eq('id', blockId)
    }
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
