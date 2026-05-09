import { ref, computed } from 'vue'

// Estado global compartido entre campana y página
const notifs = ref([
  { id: 1, type: 'pago',    title: 'Pago confirmado',      message: 'Tu pago por "Tango principiantes" fue procesado exitosamente.', read: false, createdAt: new Date(Date.now() - 3600000) },
  { id: 2, type: 'clase',   title: 'Clase próxima',        message: 'Tienes "Jazz Moderno" mañana a las 18:00 en Sala Central.',      read: false, createdAt: new Date(Date.now() - 7200000) },
  { id: 3, type: 'sistema', title: 'Identidad en revisión',message: 'Tu documento está siendo revisado. Te notificaremos en 24 horas.', read: true, createdAt: new Date(Date.now() - 86400000) },
  { id: 4, type: 'clase',   title: 'Clase cancelada',      message: 'La clase "Yoga avanzado" del viernes fue cancelada. Se realizará un reembolso.', read: true, createdAt: new Date(Date.now() - 172800000) },
])

const unreadCount = computed(() => notifs.value.filter(n => !n.read).length)

const iconConfig = (type) => {
  const map = {
    pago:    { bg: 'bg-emerald-500/20', color: 'text-emerald-400', path: 'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z' },
    clase:   { bg: 'bg-indigo-500/20',  color: 'text-indigo-400',  path: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253' },
    sistema: { bg: 'bg-blue-500/20',    color: 'text-blue-400',    path: 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z' },
  }
  return map[type] || map.sistema
}

const markRead = (id) => {
  const n = notifs.value.find(n => n.id === id)
  if (n) n.read = true
}

const markAllRead = () => {
  notifs.value.forEach(n => { n.read = true })
}

const formatTime = (d) => {
  const diff = Date.now() - new Date(d).getTime()
  if (diff < 3600000)  return `Hace ${Math.floor(diff / 60000)} min`
  if (diff < 86400000) return `Hace ${Math.floor(diff / 3600000)} h`
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short' })
}

export function useNotifications() {
  return { notifs, unreadCount, iconConfig, markRead, markAllRead, formatTime }
}
