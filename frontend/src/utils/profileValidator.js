// Validaciones para los campos editables del perfil (nombre social y teléfono).

// Teléfono chileno: acepta +56 9 XXXX XXXX con espacios/guiones opcionales.
// Normaliza a +569XXXXXXXX (9 dígitos tras el código de país).
export function normalizarTelefono(telefono) {
  if (!telefono) return ''
  // Conserva solo dígitos y un posible '+' inicial.
  let v = telefono.replace(/[^\d+]/g, '')
  if (v.startsWith('+')) v = '+' + v.slice(1).replace(/\+/g, '')
  return v
}

export function validarTelefono(telefono) {
  const v = normalizarTelefono(telefono)
  // Vacío es válido (campo opcional).
  if (!v) return { valido: true }
  // Formatos aceptados: +569XXXXXXXX, 569XXXXXXXX o 9XXXXXXXX (móvil chileno).
  const ok = /^(\+?56)?9\d{8}$/.test(v)
  if (!ok) {
    return { valido: false, error: 'Teléfono inválido. Usa un móvil chileno, ej: +56912345678' }
  }
  return { valido: true }
}

// Lista base de términos vulgares/ofensivos en español (raíces, para cubrir
// variantes). No pretende ser exhaustiva, sino bloquear lo obvio.
const PALABRAS_PROHIBIDAS = [
  'pene', 'pico', 'verga', 'polla', 'concha', 'conchatumadre', 'ctm',
  'puta', 'puto', 'puti', 'culiao', 'culia', 'culero', 'maricon', 'maraco',
  'weon', 'wn', 'mierda', 'caca', 'pendejo', 'pendeja', 'zorra', 'perra',
  'cabron', 'cabrona', 'imbecil', 'idiota', 'estupido', 'tarado',
  'vagina', 'vulva', 'teta', 'tetas', 'culo', 'ano', 'pija', 'falo',
  'coño', 'cono', 'joto', 'pajero', 'pajera', 'semen', 'orto',
  'nazi', 'hitler', 'violador', 'sexo', 'porno', 'penne'
]

// Normaliza para comparar: minúsculas, sin tildes y con sustituciones
// comunes (leetspeak) para evitar evasiones simples (p3n3, p e n e).
function normalizarTexto(texto) {
  return texto
    .toLowerCase()
    .normalize('NFD').replace(/[̀-ͯ]/g, '') // quita tildes
    .replace(/[0@]/g, 'o')
    .replace(/[1!|]/g, 'i')
    .replace(/3/g, 'e')
    .replace(/4/g, 'a')
    .replace(/5\$/g, 's')
    .replace(/7/g, 't')
    .replace(/[^a-z]/g, '') // deja solo letras (colapsa espacios/símbolos)
}

export function validarNombreSocial(nombre) {
  // Vacío es válido (campo opcional; cae al nombre real).
  if (!nombre || !nombre.trim()) return { valido: true }

  const limpio = nombre.trim()

  if (limpio.length < 2) {
    return { valido: false, error: 'El nombre social debe tener al menos 2 caracteres.' }
  }
  if (limpio.length > 40) {
    return { valido: false, error: 'El nombre social no puede superar los 40 caracteres.' }
  }
  // Solo letras (incl. acentos/ñ), espacios, guiones y apóstrofos.
  if (!/^[\p{L}\p{M}\s'’-]+$/u.test(limpio)) {
    return { valido: false, error: 'El nombre social solo puede contener letras y espacios.' }
  }

  // Filtro de contenido ofensivo: busca raíces prohibidas como palabra/substring
  // sobre el texto normalizado (sin tildes/leet).
  const normal = normalizarTexto(limpio)
  const ofensiva = PALABRAS_PROHIBIDAS.find((p) => normal.includes(p))
  if (ofensiva) {
    return { valido: false, error: 'El nombre social contiene lenguaje no permitido. Usa un nombre apropiado.' }
  }

  return { valido: true }
}
