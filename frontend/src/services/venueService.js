import { supabase, currentUserId, invokeFunction, camelize } from './supabase'

const NOT_MIGRATED = (name, extra = '') => {
  throw { response: { status: 501, data: { message: `"${name}" pendiente de migración${extra ? ': ' + extra : ''}` } } }
}

function mapVenueBody(data) {
  // `|| undefined` convierte strings vacíos a undefined: los campos opcionales
  // (email/url) no se envían vacíos, evitando que el Zod del Edge Function falle.
  return {
    name: data.name,
    city: data.city || undefined,
    region: data.region || undefined,
    comuna: data.comuna || undefined,
    address: data.address || undefined,
    description: data.description || undefined,
    phone: data.phone || undefined,
    email: data.email || undefined,
    tipo: data.tipo || 'SEDE',
    instagram: data.instagram || undefined,
    youtube: data.youtube || undefined,
    sitioWeb: data.sitioWeb || data.sitio_web || undefined,
    facebook: data.facebook || undefined
  }
}

export default {
  createVenue(data) {
    return invokeFunction('register-venue', { body: mapVenueBody(data) })
  },

  createVenueAdmin(data) {
    return invokeFunction('register-venue', { body: mapVenueBody(data) })
  },

  async getVenues() {
    const { data, error } = await supabase
      .from('venues').select('*').eq('status', 'APROBADA').order('name', { ascending: true })
    if (error) throw error
    return camelize(data)
  },

  async getMyVenues() {
    const uid = await currentUserId()
    const { data, error } = await supabase.from('venues').select('*').eq('admin_id', uid)
    if (error) throw error
    return camelize(data)
  },

  async getMiSolicitud() {
    try {
      const uid = await currentUserId()
      const { data: venues } = await supabase
        .from('venues').select('*').eq('admin_id', uid)
        .order('created_at', { ascending: false }).limit(1)
      const venue = venues?.[0]
      if (!venue) return null
      const { data: documentosGuardados } = await supabase
        .from('venue_documents').select('*').eq('venue_id', venue.id)
      return { venue: camelize(venue), documentosGuardados: camelize(documentosGuardados || []) }
    } catch {
      return null
    }
  },

  async updateVenue(id, data) {
    const { data: row, error } = await supabase
      .from('venues').update(mapVenueBody(data)).eq('id', id).select('*').single()
    if (error) throw error
    return row
  },

  async updateVenueSocial(id, data) {
    const patch = {
      instagram: data.instagram ?? null,
      youtube: data.youtube ?? null,
      sitio_web: data.sitioWeb ?? data.sitio_web ?? null,
      facebook: data.facebook ?? null
    }
    const { data: row, error } = await supabase
      .from('venues').update(patch).eq('id', id).select('*').single()
    if (error) throw error
    return row
  },

  async getVenueRooms(venueId) {
    const { data, error } = await supabase.from('rooms').select('*').eq('venue_id', venueId)
    if (error) throw error
    return camelize(data)
  },

  async getRooms(venueId) {
    const { data, error } = await supabase.from('rooms').select('*').eq('venue_id', venueId)
    if (error) throw error
    return camelize(data)
  },

  async createRoom(venueId, data) {
    const { data: row, error } = await supabase
      .from('rooms').insert({ venue_id: venueId, ...roomColumns(data) }).select('*').single()
    if (error) throw error
    return row
  },

  async updateRoom(roomId, data) {
    const { data: row, error } = await supabase
      .from('rooms').update(roomColumns(data)).eq('id', roomId).select('*').single()
    if (error) throw error
    return row
  },

  confirmClassRealized(classId) {
    return invokeFunction('confirm-class', { body: { classId, realized: true } })
  },

  confirmClassNotRealized(classId) {
    return invokeFunction('confirm-class', { body: { classId, realized: false } })
  },

  // ── Documentos de sede ──
  async getVenueDocuments(venueId) {
    const { data, error } = await supabase.from('venue_documents').select('*').eq('venue_id', venueId)
    if (error) throw error
    return camelize(data)
  },

  async addVenueDocument(venueId, data) {
    const { data: row, error } = await supabase
      .from('venue_documents')
      .insert({
        venue_id: venueId,
        file_url: data.fileUrl ?? data.file_url,
        tipo: data.tipo,
        nombre: data.nombre ?? null,
        tipo_archivo: data.tipoArchivo ?? data.tipo_archivo ?? null
      })
      .select('*').single()
    if (error) throw error
    return camelize(row)
  },

  // ── Fotos ──
  async getVenuePhotos(venueId) {
    const { data, error } = await supabase
      .from('venue_photos').select('*').eq('owner_id', venueId).eq('owner_type', 'VENUE')
      .order('display_order', { ascending: true })
    if (error) throw error
    return camelize(data)
  },

  async addVenuePhoto(venueId, data) {
    const { data: row, error } = await supabase
      .from('venue_photos')
      .insert({
        owner_id: venueId,
        owner_type: 'VENUE',
        photo_url: data.photoUrl ?? data.photo_url,
        alt_text: data.altText ?? data.alt_text ?? null,
        display_order: data.displayOrder ?? data.display_order ?? 0,
        principal: data.principal ?? false
      })
      .select('*').single()
    if (error) throw error
    return camelize(row)
  },

  async getRoomPhotos(roomId) {
    const { data, error } = await supabase
      .from('venue_photos').select('*').eq('owner_id', roomId).eq('owner_type', 'ROOM')
      .order('display_order', { ascending: true })
    if (error) throw error
    return camelize(data)
  },

  // Pendientes (huecos de RLS / flujo de Storage / lógica de agregación):
  // - getVenueClasses/getPendingClasses: RLS de classes no permite al admin de sede ver clases ajenas.
  // - registrarVenueConDocumentos: requiere subir archivos a Storage primero.
  // - deleteVenueDocument: falta policy DELETE en venue_documents.
  // - addRoomPhoto/deletePhoto: RLS de venue_photos solo permite ROOM a ADMIN / sin DELETE.
  // - getVenueMetrics/getVenueProfessors: requieren agregación server-side.
  getVenueClasses() { return NOT_MIGRATED('getVenueClasses', 'RLS de classes para admin de sede') },
  getPendingClasses() { return NOT_MIGRATED('getPendingClasses', 'RLS de classes para admin de sede') },
  registrarVenueConDocumentos() { return NOT_MIGRATED('registrarVenueConDocumentos', 'flujo de Storage') },
  deleteVenueDocument() { return NOT_MIGRATED('deleteVenueDocument', 'falta policy DELETE') },
  addRoomPhoto() { return NOT_MIGRATED('addRoomPhoto', 'RLS venue_photos ROOM') },
  deletePhoto() { return NOT_MIGRATED('deletePhoto', 'falta policy DELETE') },
  getVenueMetrics() { return NOT_MIGRATED('getVenueMetrics', 'agregación server-side') },
  getVenueProfessors() { return NOT_MIGRATED('getVenueProfessors', 'agregación server-side') }
}

// Convierte el payload de sala (camelCase) a columnas snake_case conocidas.
function roomColumns(d) {
  const map = {
    name: d.name,
    capacity: d.capacity,
    tamano_m2: d.tamanoM2 ?? d.tamano_m2,
    floor_type: d.floorType ?? d.floor_type,
    type: d.type,
    price_per_hour: d.pricePerHour ?? d.price_per_hour,
    activa: d.activa,
    has_mirrors: d.hasMirrors ?? d.has_mirrors,
    tiene_barra_ballet: d.tieneBarraBallet ?? d.tiene_barra_ballet,
    tiene_aire_acondicionado: d.tieneAireAcondicionado ?? d.tiene_aire_acondicionado,
    tiene_calefaccion: d.tieneCalefaccion ?? d.tiene_calefaccion,
    tiene_insonorizacion: d.tieneInsonorizacion ?? d.tiene_insonorizacion,
    has_sound: d.hasSound ?? d.has_sound,
    tiene_amplificacion: d.tieneAmplificacion ?? d.tiene_amplificacion,
    tiene_entrada_auxiliar: d.tieneEntradaAuxiliar ?? d.tiene_entrada_auxiliar,
    tiene_microfono: d.tieneMicrofono ?? d.tiene_microfono,
    tiene_equipo_grabacion: d.tieneEquipoGrabacion ?? d.tiene_equipo_grabacion,
    tiene_piano: d.tienePiano ?? d.tiene_piano,
    tiene_guitarra: d.tieneGuitarra ?? d.tiene_guitarra,
    tiene_bateria: d.tieneBateria ?? d.tiene_bateria,
    equipment: d.equipment,
    image_url: d.imageUrl ?? d.image_url
  }
  // Elimina undefined para no pisar columnas con null accidentalmente.
  return Object.fromEntries(Object.entries(map).filter(([, v]) => v !== undefined))
}
