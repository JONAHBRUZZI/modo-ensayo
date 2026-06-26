import { ref, computed } from 'vue'
import { notificationService } from '../services/notificationService'

const notifs = ref([])
let polling = null

async function fetchNotifications() {
  try {
    const res = await notificationService.getAll()
    notifs.value = Array.isArray(res.data) ? res.data : []
  } catch {
    // sin sesión o error de red: no romper la UI
  }
}

function startPolling() {
  if (polling) return
  fetchNotifications()
  polling = setInterval(fetchNotifications, 30000)
}

function stopPolling() {
  if (polling) { clearInterval(polling); polling = null }
}

const unreadCount = computed(() => notifs.value.filter(n => !n.read).length)

const iconConfig = (type) => {
  const map = {
    pago:    { bg: 'bg-emerald-500/20', color: 'text-emerald-400', path: 'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z' },
    clase:   { bg: 'bg-indigo-500/20',  color: 'text-indigo-400',  path: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253' },
    sistema: { bg: 'bg-blue-500/20',    color: 'text-blue-400',    path: 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z' },
    reschedule: { bg: 'bg-yellow-500/20', color: 'text-yellow-400', path: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' },
  }
  return map[type] || map.sistema
}

async function markRead(id) {
  const n = notifs.value.find(n => n.id === id)
  if (n && !n.read) {
    n.read = true
    try { await notificationService.markRead(id) } catch { n.read = false }
  }
}

async function markAllRead() {
  const prevStates = notifs.value.map(n => n.read)
  notifs.value.forEach(n => { n.read = true })
  try {
    await notificationService.markAllRead()
  } catch {
    notifs.value.forEach((n, i) => { n.read = prevStates[i] })
  }
}

const formatTime = (d) => {
  const diff = Date.now() - new Date(d).getTime()
  if (diff < 3600000)  return `Hace ${Math.floor(diff / 60000)} min`
  if (diff < 86400000) return `Hace ${Math.floor(diff / 3600000)} h`
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short' })
}

export function useNotifications() {
  return { notifs, unreadCount, iconConfig, markRead, markAllRead, formatTime, startPolling, stopPolling, fetchNotifications }
}
