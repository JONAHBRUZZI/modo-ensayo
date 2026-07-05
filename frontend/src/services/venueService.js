import { supabase, currentUserId, invokeFunction, camelize } from './supabase'

const NOT_MIGRATED = (name, extra = '') =>
  Promise.reject({ response: { status: 501, data: { message: `"${name}" pendiente de migración${extra ? ': ' + extra : ''}` } } })

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

  // Guarda en el perfil de la sede la foto de referencia (image_url) y los datos
  // de capacidad capturados en el registro.
  async updateVenueExtras(id, { imageUrl, capacidadMaxima, cantidadSalas } = {}) {
    const patch = {}
    if (imageUrl !== undefined && imageUrl !== null) patch.image_url = imageUrl
    if (capacidadMaxima !== undefined && capacidadMaxima !== '' && capacidadMaxima !== null) patch.capacidad_maxima = capacidadMaxima
    if (cantidadSalas !== undefined && cantidadSalas !== '' && cantidadSalas !== null) patch.cantidad_salas = cantidadSalas
    if (Object.keys(patch).length === 0) return null
    const { data: row, error } = await supabase
      .from('venues').update(patch).eq('id', id).select('*').single()
    if (error) throw error
    return camelize(row)
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

  // Reservas activas de una sala = bloques OCCUPIED (reservados) o HELD (en pago).
  // Se usa para impedir eliminar una sala que tiene reservas vigentes.
  async countReservasActivasSala(roomId) {
    const { count, error } = await supabase
      .from('room_schedule_blocks')
      .select('id', { count: 'exact', head: true })
      .eq('room_id', roomId)
      .in('status', ['OCCUPIED', 'HELD'])
    if (error) throw error
    return count || 0
  },

  // Elimina una sala. Los bloques de horario y mantenciones se borran en cascada.
  // Si la sala tiene clases asociadas, la FK (RESTRICT) hace fallar el borrado:
  // se traduce a un mensaje claro para el gestor.
  // Se usa .select() para confirmar que realmente se borró una fila: si RLS impide
  // el DELETE, PostgREST no devuelve error pero afecta 0 filas (data vacío).
  async deleteRoom(roomId) {
    const { data, error } = await supabase.from('rooms').delete().eq('id', roomId).select('id')
    if (error) {
      if (error.code === '23503') {
        throw { response: { status: 409, data: { message: 'La sala tiene clases asociadas y no puede eliminarse.' } } }
      }
      throw error
    }
    if (!Array.isArray(data) || data.length === 0) {
      throw { response: { status: 403, data: { message: 'No se pudo eliminar la sala (permisos insuficientes). Intenta nuevamente más tarde.' } } }
    }
    return { status: 'ok' }
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

  async addRoomPhoto(roomId, data) {
    const { data: row, error } = await supabase
      .from('venue_photos')
      .insert({
        owner_id: roomId,
        owner_type: 'ROOM',
        photo_url: data.photoUrl ?? data.photo_url,
        alt_text: data.altText ?? data.alt_text ?? null,
        display_order: data.displayOrder ?? data.display_order ?? 0,
        principal: data.principal ?? false
      })
      .select('*').single()
    if (error) throw error
    return camelize(row)
  },

  // Salas destacadas para la home: salas de sedes aprobadas (la RLS ya filtra por
  // sede APROBADA) que tengan al menos una foto. Devuelve la foto principal.
  async getFeaturedRooms(limit = 6) {
    const { data: rooms, error } = await supabase
      .from('rooms')
      .select('id, name, type, capacity, price_per_hour, venue:venues(name, city)')
      .limit(40)
    if (error) throw error
    if (!Array.isArray(rooms) || rooms.length === 0) return []

    const ids = rooms.map(r => r.id)
    const { data: photos } = await supabase
      .from('venue_photos').select('owner_id, photo_url, principal, display_order')
      .eq('owner_type', 'ROOM').in('owner_id', ids)
      .order('display_order', { ascending: true })

    const byRoom = {}
    for (const p of photos || []) {
      if (!byRoom[p.owner_id] || p.principal) byRoom[p.owner_id] = p.photo_url
    }

    return rooms
      .filter(r => byRoom[r.id])
      .slice(0, limit)
      .map(r => camelize({ ...r, photo_url: byRoom[r.id] }))
  },

  // Clases agendadas en las salas de la sede del usuario. Usa el RPC
  // get_venue_classes (SECURITY DEFINER, auto-scopeado a auth.uid()), que
  // cubre el hueco de RLS sin ampliar las policies de `classes`.
  async getVenueClasses() {
    const { data, error } = await supabase.rpc('get_venue_classes')
    if (error) throw { response: { status: 500, data: { message: error.message } } }
    return (data || []).map(c => camelize(c))
  },
  // Clases pendientes de validación de la sede (status POR_VALIDAR).
  async getPendingClasses() {
    const { data, error } = await supabase.rpc('get_venue_classes', { p_status: 'POR_VALIDAR' })
    if (error) throw { response: { status: 500, data: { message: error.message } } }
    return (data || []).map(c => camelize(c))
  },

  // Pendientes (huecos de RLS / flujo de Storage / lógica de agregación):
  // - registrarVenueConDocumentos: requiere subir archivos a Storage primero.
  // - deleteVenueDocument: falta policy DELETE en venue_documents.
  // - deletePhoto: falta policy DELETE en venue_photos para algunos casos.
  // - getVenueMetrics/getVenueProfessors: requieren agregación server-side.
  async registrarVenueConDocumentos(fd) {
    const userId = await currentUserId()

    // 1. Subir cada archivo a Storage bajo {userId}/{uuid}.ext
    const documentPaths = []
    const files = fd.getAll('documentos')
    const tipos = fd.getAll('tiposDocumento')

    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      const tipo = tipos[i]
      const ext = file.name.includes('.') ? file.name.split('.').pop() : 'bin'
      const path = `${userId}/${crypto.randomUUID()}.${ext}`

      const { error: upErr } = await supabase.storage
        .from('venue-documents')
        .upload(path, file, { contentType: file.type, upsert: false })

      if (upErr) {
        throw { response: { status: 500, data: { message: `Error al subir documento: ${upErr.message}` } } }
      }

      documentPaths.push({ path, tipo, nombre: file.name, tipoArchivo: file.type })
    }

    // 2. Crear la sede con los paths ya subidos (el EF inserta venue_documents)
    return invokeFunction('register-venue', {
      body: {
        name: fd.get('nombre'),
        city: fd.get('ciudad') || undefined,
        region: fd.get('region') || undefined,
        comuna: fd.get('comuna') || undefined,
        address: fd.get('direccion') || undefined,
        description: fd.get('descripcion') || undefined,
        phone: fd.get('teléfono') || undefined,
        email: fd.get('email') || undefined,
        tipo: fd.get('tipo') || 'SEDE',
        instagram: fd.get('instagram') || undefined,
        sitioWeb: fd.get('sitioWeb') || undefined,
        documentPaths,
      }
    })
  },
  deleteVenueDocument() { return NOT_MIGRATED('deleteVenueDocument', 'falta policy DELETE') },
  deletePhoto() { return NOT_MIGRATED('deletePhoto', 'falta policy DELETE') },

  // Ingresos de la sede por fuente (arriendo/clases) y total, agrupados por
  // período. granularidad: 'month' (default) | 'year'. RPC SECURITY DEFINER.
  async getVenueMetrics(granularidad = 'month') {
    const { data, error } = await supabase.rpc('get_venue_metrics', { p_granularidad: granularidad })
    if (error) throw { response: { status: 500, data: { message: error.message } } }
    return (data || []).map(r => camelize(r))
  },

  // Profesores dependientes de las sedes del usuario (RPC SECURITY DEFINER).
  // Cada fila: { id (venue_teachers.id), venueId, teacherId, status, email, fullName }.
  async getVenueProfessors() {
    const { data, error } = await supabase.rpc('get_venue_professors')
    if (error) throw { response: { status: 500, data: { message: error.message } } }
    return (data || []).map(r => camelize(r))
  },

  // Agrega un profe dependiente por email (el RPC valida la sede y resuelve el
  // email en auth.users, inaccesible desde el cliente).
  async addVenueTeacher(venueId, email) {
    const { error } = await supabase.rpc('add_venue_teacher', { p_venue_id: venueId, p_email: email })
    if (error) throw { response: { status: 400, data: { message: error.message } } }
    return { status: 'ok' }
  },

  // Quita un profe dependiente (la policy DELETE permite al admin de la sede).
  async removeVenueTeacher(id) {
    const { error } = await supabase.from('venue_teachers').delete().eq('id', id)
    if (error) throw error
  },

  // Candidatos a profesor (rol TEACHER o con perfil profesional) para sugerir el
  // correo al agregar un profe dependiente. Devuelve [{ email, fullName }].
  async getTeacherCandidates() {
    const { data, error } = await supabase.rpc('list_teacher_candidates')
    if (error) throw { response: { status: 500, data: { message: error.message } } }
    return (data || []).map(r => camelize(r))
  },

  // Cuánto se le debe a cada profesor dependiente (honorarios comprometidos).
  // Devuelve [{ teacherId, fullName, email, totalHonorario, clases }].
  async getVenueTeacherPayouts() {
    const { data, error } = await supabase.rpc('get_venue_teacher_payouts')
    if (error) throw { response: { status: 500, data: { message: error.message } } }
    return (data || []).map(r => camelize(r))
  },

  // Detalle completo de una clase de la sede (info + profesor + cupos).
  async getVenueClassDetail(classId) {
    const { data, error } = await supabase.rpc('get_venue_class_detail', { p_class_id: classId })
    if (error) throw { response: { status: 500, data: { message: error.message } } }
    const row = Array.isArray(data) ? data[0] : data
    return row ? camelize(row) : null
  },

  // Alumnos/beneficiarios inscritos en una clase de la sede (+ asistencia).
  async getVenueClassStudents(classId) {
    const { data, error } = await supabase.rpc('get_venue_class_students', { p_class_id: classId })
    if (error) throw { response: { status: 500, data: { message: error.message } } }
    return (data || []).map(r => camelize(r))
  }
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
