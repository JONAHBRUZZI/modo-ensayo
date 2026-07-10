// Mapea el tipo de una notificación a la ruta que debe abrir al hacer clic.
// Devuelve null si la notificación no es accionable (solo informativa).
//
// Los reagendamientos (RESCHEDULE_PENDING / RESCHEDULE_REJECTED) llevan al
// alumno a Mis Clases: la tabla notifications no guarda el id del reschedule,
// así que el detalle exacto se alcanza desde el banner de esa pantalla.
const RUTAS_POR_TIPO = {
  RESCHEDULE_PENDING: '/alumno/mis-clases',
  RESCHEDULE_REJECTED: '/alumno/mis-clases',
  // El profesor independiente, ante una clase caída, va a su pestaña de
  // reagendamientos para decidir si reagenda dentro del plazo de 24h.
  CLASS_RESCHEDULE_OFFERED: '/profesor/reagendamientos',
}

export function rutaNotificacion(type) {
  return RUTAS_POR_TIPO[type] || null
}
