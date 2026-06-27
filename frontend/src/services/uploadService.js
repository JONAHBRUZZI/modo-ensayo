import { supabase, currentUserId } from './supabase'
import { compressImage } from '@/utils/imageCompression'

// Mapea el "type" lógico al bucket de Storage y a cómo se construye el path.
// Las RLS de Storage exigen que el primer segmento del path sea:
//  - el uid del usuario   (avatars, identity-docs)
//  - el id de la sede/sala (venue-photos, room-photos, venue-documents)
// `private: true` ⇒ devolvemos el path (no la public URL): se guarda como
// document_url/file_url y se visualiza vía signed URL (resolveDocUrl).
const BUCKETS = {
  identity: { bucket: 'identity-docs', owner: 'user', private: true },
  documents: { bucket: 'identity-docs', owner: 'user', private: true },
  avatar: { bucket: 'avatars', owner: 'user' },
  'venue-photos': { bucket: 'venue-photos', owner: 'arg' },
  venue: { bucket: 'venue-photos', owner: 'arg' },
  'room-photos': { bucket: 'room-photos', owner: 'arg' },
  room: { bucket: 'room-photos', owner: 'arg' },
  'venue-documents': { bucket: 'venue-documents', owner: 'arg', private: true }
}

export default {
  /**
   * Sube un archivo a Supabase Storage.
   * @param {File} file
   * @param {string} type   Clave lógica (ver BUCKETS).
   * @param {string} [ownerId] Requerido para buckets de sede/sala (venueId/roomId).
   * @returns {Promise<{ url: string, path: string, bucket: string }>}
   */
  async uploadFile(file, type = 'documents', ownerId = null) {
    const cfg = BUCKETS[type] || BUCKETS.documents
    const prefix = cfg.owner === 'user' ? await currentUserId() : ownerId
    if (!prefix) {
      throw { response: { status: 400, data: { message: `Falta ownerId para subir a ${cfg.bucket}` } } }
    }
    // Comprime cualquier imagen antes de subir (baja el peso manteniendo calidad).
    // No afecta a PDFs ni otros archivos no-imagen.
    file = await compressImage(file)
    const safeName = `${Date.now()}-${file.name.replace(/[^\w.\-]/g, '_')}`
    const path = `${prefix}/${safeName}`
    const { error } = await supabase.storage.from(cfg.bucket).upload(path, file, { upsert: false })
    if (error) throw error
    if (cfg.private) {
      // Buckets privados: el path es lo que se persiste; se firma al visualizar.
      return { url: path, path, bucket: cfg.bucket }
    }
    const { data: pub } = supabase.storage.from(cfg.bucket).getPublicUrl(path)
    return { url: pub.publicUrl, path, bucket: cfg.bucket }
  },

  /**
   * Devuelve una URL visualizable para un valor guardado (document_url / file_url).
   * Si ya es http(s) la devuelve tal cual; si es un path de bucket privado
   * (identity-docs / venue-documents) genera una signed URL.
   * @returns {Promise<string|null>}
   */
  async resolveDocUrl(value) {
    if (!value) return null
    if (/^https?:\/\//.test(value)) return value
    for (const bucket of ['identity-docs', 'venue-documents']) {
      const { data } = await supabase.storage.from(bucket).createSignedUrl(value, 3600)
      if (data?.signedUrl) return data.signedUrl
    }
    return null
  }
}
