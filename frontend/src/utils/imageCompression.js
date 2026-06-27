// Compresión de imágenes en el cliente (sin dependencias).
// Redimensiona con canvas manteniendo la proporción y re-codifica a JPEG con
// buena calidad para bajar el peso del archivo antes de subirlo a Storage.
// Se usa de forma transversal desde uploadService, por lo que TODA subida de
// imagen de la plataforma queda comprimida automáticamente.

function loadImage(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => { URL.revokeObjectURL(url); resolve(img) }
    img.onerror = (e) => { URL.revokeObjectURL(url); reject(e) }
    img.src = url
  })
}

function scaleDims(w, h, maxDim) {
  if (w <= maxDim && h <= maxDim) return { width: w, height: h }
  const ratio = w > h ? maxDim / w : maxDim / h
  return { width: Math.round(w * ratio), height: Math.round(h * ratio) }
}

function renameToJpg(name) {
  return name.replace(/\.[^.]+$/, '') + '.jpg'
}

/**
 * Comprime una imagen manteniendo calidad.
 * @param {File} file
 * @param {{ maxDim?: number, quality?: number }} [opts]
 * @returns {Promise<File>} archivo comprimido (o el original si no aplica/no mejora).
 */
export async function compressImage(file, { maxDim = 1600, quality = 0.8 } = {}) {
  // Solo imágenes rasterizadas. GIF (animación) y SVG (vector) se dejan intactos.
  if (!file?.type?.startsWith('image/')) return file
  if (file.type === 'image/gif' || file.type === 'image/svg+xml') return file

  try {
    const img = await loadImage(file)
    const { width, height } = scaleDims(img.naturalWidth || img.width, img.naturalHeight || img.height, maxDim)

    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0, width, height)

    const blob = await new Promise((res) => canvas.toBlob(res, 'image/jpeg', quality))
    // Si falló o no logra reducir el peso, conserva el original.
    if (!blob || blob.size >= file.size) return file

    return new File([blob], renameToJpg(file.name), { type: 'image/jpeg', lastModified: Date.now() })
  } catch {
    // Ante cualquier error de canvas/carga, sube el archivo original.
    return file
  }
}
