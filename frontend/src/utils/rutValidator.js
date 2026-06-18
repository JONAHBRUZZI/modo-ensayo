export function formatearRut(rut) {
  if (!rut) return ''
  let value = rut.replace(/[^0-9kK]/g, '')
  if (value.length < 2) return value
  const dv = value.slice(-1).toUpperCase()
  const cuerpo = value.slice(0, -1)
  return cuerpo.replace(/\B(?=(\d{3})+(?!\d))/g, '.') + '-' + dv
}

export function validateRut(rut) {
  if (!rut) return false
  let value = rut.replace(/[^0-9kK]/g, '')
  if (value.length < 2) return false
  const dv = value.slice(-1).toUpperCase()
  const cuerpo = value.slice(0, -1)
  let suma = 0
  let multiplo = 2
  for (let i = cuerpo.length - 1; i >= 0; i--) {
    suma += parseInt(cuerpo[i]) * multiplo
    multiplo = multiplo < 7 ? multiplo + 1 : 2
  }
  const dvEsperado = 11 - (suma % 11)
  const dvCalculado = dvEsperado === 11 ? '0' : dvEsperado === 10 ? 'K' : dvEsperado.toString()
  return dvCalculado === dv
}
