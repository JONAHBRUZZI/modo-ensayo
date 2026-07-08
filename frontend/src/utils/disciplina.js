// Normalización de nombres de disciplina, compartida por los formularios de
// creación de clase (profesor y sede) para evitar duplicados por formato.

// "Title Case" con espacios colapsados y recortados: "  KARATE " -> "Karate".
export function normalizarDisciplina(txt) {
  return (txt || '').trim().replace(/\s+/g, ' ').toLowerCase()
    .split(' ').filter(Boolean)
    .map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')
}

// Clave de comparación: sin acentos ni mayúsculas, para deduplicar
// ("Karate" == "karate" == "KÁRATE"). No fusiona grafías distintas (karate ≠ carate).
export function claveComparacion(txt) {
  return normalizarDisciplina(txt).toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
}
