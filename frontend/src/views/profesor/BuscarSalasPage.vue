<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-4 flex-wrap gap-4">
      <div>
        <h1 class="text-3xl font-bold text-white mb-1">Buscar Salas</h1>
        <p class="text-gray-400">Encuentra la sala perfecta para tu clase.</p>
      </div>
    </div>

    <div v-if="borradorId" class="bg-blue-500/10 border border-blue-500/30 rounded-xl px-4 py-3 mb-6 flex items-center gap-3">
      <svg class="w-5 h-5 text-blue-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      <p class="text-blue-300 text-sm">Selecciona un horario para asignarlo a tu clase y publicarla.</p>
    </div>

    <!-- ====== STEP 1: Filtros ====== -->
    <div class="card mb-6">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
        <div>
          <label class="text-xs text-gray-500 mb-0.5 block">Region</label>
          <select v-model="filtros.region" class="input-field text-sm py-2" @change="onRegionChange">
            <option value="">Todas las regiones</option>
            <option v-for="r in regiones" :key="r" :value="r">{{ r }}</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-gray-500 mb-0.5 block">Comuna</label>
          <input v-model="filtros.comuna" type="text" class="input-field text-sm py-2" placeholder="Ej: Providencia" />
        </div>
        <div>
          <label class="text-xs text-gray-500 mb-0.5 block">Desde</label>
          <input v-model="filtros.fechaDesde" type="date" class="input-field text-sm py-2" />
        </div>
        <div>
          <label class="text-xs text-gray-500 mb-0.5 block">Hasta</label>
          <input v-model="filtros.fechaHasta" type="date" class="input-field text-sm py-2" />
        </div>
      </div>

      <div class="mb-4">
        <label class="text-xs text-gray-500 mb-1.5 block">Disciplina</label>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="d in disciplinas"
            :key="d"
            type="button"
            :class="[
              'px-4 py-1.5 rounded-full text-sm font-medium border transition-colors',
              filtros.disciplina === d
                ? 'bg-primary/20 border-primary text-primary'
                : 'border-white/10 text-gray-400 hover:border-white/30 hover:text-white bg-[#1a1d2e]'
            ]"
            @click="toggleDisciplina(d)"
          >
            {{ d }}
          </button>
        </div>
      </div>

      <div v-if="filtros.disciplina" class="mb-4">
        <label class="text-xs text-gray-500 mb-1.5 block">Caracteristicas</label>
        <div class="flex flex-wrap gap-3">
          <label v-for="c in caracteristicasFiltradas" :key="c.key" class="flex items-center gap-1.5 cursor-pointer">
            <input type="checkbox" v-model="filtros.caracteristicas" :value="c.key" class="w-4 h-4 rounded border-dark-border bg-dark-bg text-primary focus:ring-primary/50" />
            <span class="text-sm text-gray-300">{{ c.label }}</span>
          </label>
        </div>
      </div>

      <button @click="buscar" class="btn-primary text-sm w-full sm:w-auto">Buscar</button>
    </div>

    <!-- ====== STEP 2: Venues ====== -->
    <div v-if="loading" class="text-center text-gray-400 py-20">Buscando salas disponibles...</div>
    <div v-else-if="venues.length === 0" class="card text-center py-12"><p class="text-gray-400">No se encontraron salas con esos filtros.</p></div>
    <div v-else class="space-y-4">
      <div v-for="venue in venues" :key="venue.id" class="card">
        <div @click="toggleVenue(venue.id)" class="cursor-pointer flex items-center justify-between">
          <div class="flex-1">
            <h3 class="text-lg font-semibold text-white">{{ venue.name }}</h3>
            <p class="text-gray-400 text-sm">{{ venue.address }}</p>
            <p class="text-gray-500 text-xs mt-1">{{ venue.region || '' }}{{ venue.city ? ' . ' + venue.city : '' }}</p>
            <p class="text-primary text-xs mt-0.5">{{ venue.rooms?.length || 0 }} sala(s) disponible(s)</p>
          </div>
          <svg :class="['w-5 h-5 text-gray-400 transition-transform flex-shrink-0 ml-3', expandedVenue === venue.id && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
        </div>

        <!-- ====== STEP 3: Rooms ====== -->
        <div v-if="expandedVenue === venue.id" class="mt-4 space-y-3">
          <div v-if="venue.rooms?.length">
            <div v-for="room in venue.rooms" :key="room.id" class="bg-[#1a1d2e] rounded-xl p-4">
              <div @click="toggleRoom(room.id)" class="cursor-pointer flex items-center justify-between">
                <div class="flex-1">
                  <p class="text-white font-medium">{{ room.name }}</p>
                  <p class="text-gray-400 text-sm">
                    Cap: {{ room.capacity }} pers.
                    <span v-if="room.tamanoM2"> . {{ room.tamanoM2 }} m2</span>
                    <span v-if="room.floorType"> . Piso {{ room.floorType }}</span>
                  </p>
                  <p class="text-primary text-sm font-medium mt-0.5">${{ room.pricePerHour?.toLocaleString() }} / hora</p>

                  <div v-if="getCaracteristicasDanza(room).length" class="mt-2">
                    <span class="text-xs text-gray-500">Danza:</span>
                    <div class="flex flex-wrap gap-1 mt-0.5">
                      <span v-for="c in getCaracteristicasDanza(room)" :key="c" class="equip-tag">{{ c }}</span>
                    </div>
                  </div>
                  <div v-if="getCaracteristicasMusica(room).length" class="mt-1">
                    <span class="text-xs text-gray-500">Musica:</span>
                    <div class="flex flex-wrap gap-1 mt-0.5">
                      <span v-for="c in getCaracteristicasMusica(room)" :key="c" class="equip-tag">{{ c }}</span>
                    </div>
                  </div>
                </div>
                <svg :class="['w-4 h-4 text-gray-500 ml-3 flex-shrink-0 transition-transform', expandedRoom === room.id && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
              </div>

              <!-- ====== STEP 4: Weekly Calendar ====== -->
              <div v-if="expandedRoom === room.id" class="mt-3">
                <div v-if="roomLoading === room.id" class="text-gray-500 text-sm py-4 text-center">Cargando disponibilidad...</div>
                <div v-else-if="roomSlots[room.id]?.length" class="overflow-x-auto">
                  <div class="flex items-center justify-between mb-3">
                    <button @click.stop="prevRoomWeek(room.id)" class="text-xs text-gray-400 hover:text-white px-2 py-1 rounded hover:bg-dark-border">&larr; Anterior</button>
                    <span class="text-xs text-gray-400 font-medium">{{ getRoomWeekLabel(room.id) }}</span>
                    <button @click.stop="nextRoomWeek(room.id)" class="text-xs text-gray-400 hover:text-white px-2 py-1 rounded hover:bg-dark-border">Siguiente &rarr;</button>
                  </div>

                  <table class="w-full border-collapse">
                    <thead>
                      <tr>
                        <th class="w-20 p-2 text-left text-xs text-gray-500 font-medium"></th>
                        <th v-for="day in calendarWeekDays" :key="day.date" class="p-2 text-center text-xs font-medium text-gray-300 min-w-[100px]">
                          {{ day.label }}<br /><span class="font-normal opacity-60">{{ day.dateFormatted }}</span>
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="block in calendarTimeBlocks" :key="block.key">
                        <td class="p-2 text-xs text-gray-500 align-top whitespace-nowrap">{{ block.label }}</td>
                        <td v-for="day in calendarWeekDays" :key="day.date" class="p-1 align-top">
                          <div
                            v-for="slot in getRoomSlotsForCell(room.id, day.date, block)"
                            :key="slot.id"
                            class="rounded p-1.5 mb-0.5 cursor-pointer hover:opacity-80 border bg-green-500/15 border-green-500/30 text-xs transition-opacity"
                            @click="confirmarAgendamiento(room, venue, slot)"
                          >
                            <span class="text-green-300 text-[10px] block">Disponible</span>
                          </div>
                          <div v-if="!getRoomSlotsForCell(room.id, day.date, block).length" class="min-h-[28px]"></div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div v-else class="text-gray-500 text-sm py-2">Sin horarios disponibles en este rango.</div>
              </div>
            </div>
          </div>
          <p v-else class="text-gray-500 text-sm">No hay salas en esta sede.</p>
        </div>
      </div>
    </div>

    <!-- Alerta identidad -->
    <div v-if="alertaIdentidad" class="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
      <div class="bg-[#1a1d2e] rounded-2xl border border-yellow-500/30 p-6 max-w-sm w-full mx-4">
        <div class="flex items-start gap-3 mb-4">
          <svg class="w-6 h-6 text-yellow-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
          </svg>
          <div>
            <h3 class="text-white font-semibold mb-1">Identidad no validada</h3>
            <p class="text-gray-400 text-sm">Debes validar tu identidad antes de reservar una sala y crear clases.</p>
          </div>
        </div>
        <div class="flex gap-3">
          <button @click="alertaIdentidad = false" class="flex-1 px-4 py-2 rounded-xl border border-white/10 text-gray-300 hover:bg-white/5 text-sm">Cerrar</button>
          <router-link to="/profile/identity" class="flex-1 text-center px-4 py-2 rounded-xl bg-primary text-white text-sm font-medium hover:bg-primary/80">Validar identidad</router-link>
        </div>
      </div>
    </div>

    <!-- ====== STEP 5: Modal de confirmacion ====== -->
    <div v-if="modal.abierto" class="fixed inset-0 bg-black/60 flex items-center justify-center z-50" @click.self="modal.abierto = false">
      <div class="bg-[#1a1d2e] rounded-2xl border border-white/10 p-6 max-w-md w-full mx-4">
        <h3 class="text-lg font-semibold text-white mb-2">Confirmar Reserva</h3>
        <div class="text-gray-400 text-sm space-y-2 mb-6">
          <p><span class="text-gray-500">Sede:</span> {{ modal.venue?.name }}</p>
          <p><span class="text-gray-500">Sala:</span> {{ modal.room?.name }} ({{ modal.room?.capacity }} personas)</p>
          <p><span class="text-gray-500">Fecha:</span> {{ modal.slot ? formatDate(modal.slot.startTime) : '' }}</p>
          <p><span class="text-gray-500">Horario:</span> {{ modal.slot ? formatTime(modal.slot.startTime) + ' - ' + formatTime(modal.slot.endTime) : '' }}</p>
          <p class="text-primary font-semibold"><span class="text-gray-500">Precio:</span> ${{ modal.room?.pricePerHour?.toLocaleString() }} / hora</p>
        </div>
        <p class="text-white text-sm mb-6">Selecciona tu metodo de pago para confirmar la reserva:</p>
        <div class="space-y-2 mb-6">
          <button @click="pagar('transferencia')" :disabled="modal.procesando" class="w-full text-left px-4 py-3 bg-[#0d0f1a] rounded-xl border border-white/10 hover:border-primary/50 transition-colors">
            <span class="text-white text-sm font-medium">Transferencia Bancaria</span>
            <p class="text-gray-500 text-xs">Pago simulado - se registrara la reserva</p>
          </button>
        </div>
        <div class="flex space-x-3 justify-end">
          <button @click="modal.abierto = false" class="px-4 py-2 rounded-xl border border-white/10 text-gray-300 hover:bg-white/5 text-sm">Cancelar</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import classService from '@/services/classService'
import venueService from '@/services/venueService'
import scheduleService from '@/services/scheduleService'
import api from '@/services/api'
import { useAuth } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { formatDate, formatTime } from '@/utils/dateFormatter'

const toast = useToast()
const route = useRoute()
const auth = useAuth()
const { syncAtributos, setModo, puedeVerContextoProfesor, perfilProfesionalCompleto, identidadValidada } = auth

const borradorId = computed(() => route.query.borradorId || null)
const alertaIdentidad = ref(false)
const venues = ref([])
const loading = ref(true)
const expandedVenue = ref(null)
const expandedRoom = ref(null)
const roomSlots = ref({})
const roomLoading = ref(null)
const modal = ref({ abierto: false, venue: null, room: null, slot: null, procesando: false })
const filtros = ref({ region: '', comuna: '', disciplina: '', fechaDesde: '', fechaHasta: '', caracteristicas: [] })

const roomWeekStarts = ref({})
let roomWeekSlotsCache = {}

const disciplinas = ['Danza', 'Musica', 'Teatro']

const caracteristicasPorDisciplina = {
  'Danza': [
    { key: 'espejos', label: 'Espejos' },
    { key: 'barraBallet', label: 'Barra ballet' },
    { key: 'pisoMadera', label: 'Piso madera' },
    { key: 'pisoFlotante', label: 'Piso flotante' },
    { key: 'aireAcondicionado', label: 'Aire acondicionado' }
  ],
  'Musica': [
    { key: 'insonorizado', label: 'Insonorizado' },
    { key: 'sonido', label: 'Equipo de sonido' },
    { key: 'amplificacion', label: 'Amplificacion' },
    { key: 'piano', label: 'Piano' },
    { key: 'guitarra', label: 'Guitarra' },
    { key: 'bateria', label: 'Bateria' },
    { key: 'grabacion', label: 'Grabacion' }
  ],
  'Teatro': [
    { key: 'espejos', label: 'Espejos' },
    { key: 'aireAcondicionado', label: 'Aire acondicionado' },
    { key: 'calefaccion', label: 'Calefaccion' },
    { key: 'sonido', label: 'Equipo de sonido' },
    { key: 'amplificacion', label: 'Amplificacion' }
  ]
}

const caracteristicasFiltradas = computed(() => {
  return caracteristicasPorDisciplina[filtros.value.disciplina] || []
})

const regiones = [
  'XV - Arica y Parinacota', 'I - Tarapaca', 'II - Antofagasta', 'III - Atacama',
  'IV - Coquimbo', 'V - Valparaiso', 'RM - Metropolitana', "VI - O'Higgins",
  'VII - Maule', 'XVI - Nuble', 'VIII - Biobio', 'IX - La Araucania',
  'XIV - Los Rios', 'X - Los Lagos', 'XI - Aysen', 'XII - Magallanes'
]

function toggleDisciplina(d) {
  if (filtros.value.disciplina === d) {
    filtros.value.disciplina = ''
    filtros.value.caracteristicas = []
  } else {
    filtros.value.disciplina = d
    filtros.value.caracteristicas = []
  }
}

function getCaracteristicasDanza(room) {
  const tags = []
  if (room.hasMirrors) tags.push('Espejos')
  if (room.tieneBarraBallet) tags.push('Barra ballet')
  if (room.floorType) tags.push('Piso ' + room.floorType)
  if (room.tieneAireAcondicionado) tags.push('Aire AC')
  return tags
}

function getCaracteristicasMusica(room) {
  const tags = []
  if (room.tieneInsonorizacion) tags.push('Insonorizado')
  if (room.hasSound) tags.push('Sonido')
  if (room.tieneAmplificacion) tags.push('Amplificacion')
  if (room.tienePiano) tags.push('Piano')
  if (room.tieneGuitarra) tags.push('Guitarra')
  if (room.tieneBateria) tags.push('Bateria')
  if (room.tieneEquipoGrabacion) tags.push('Grabacion')
  return tags
}

function getMonday(d) {
  const date = new Date(d)
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  date.setHours(0, 0, 0, 0)
  return date
}

const calendarWeekDays = computed(() => {
  const result = []
  const monday = new Date(getMonday(new Date()))
  const dowLabels = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom']
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    result.push({
      date: d.toISOString().slice(0, 10),
      dateFormatted: d.toLocaleDateString('es-CL', { day: 'numeric', month: 'short' }),
      label: dowLabels[i]
    })
  }
  return result
})

const calendarTimeBlocks = computed(() => {
  const blocks = []
  for (let h = 8; h <= 21; h++) {
    const start = `${String(h).padStart(2, '0')}:00`
    blocks.push({ key: start, label: start, start })
  }
  return blocks
})

function getRoomWeekStart(roomId) {
  if (!roomWeekStarts.value[roomId]) {
    roomWeekStarts.value[roomId] = getMonday(new Date())
  }
  return roomWeekStarts.value[roomId]
}

function getRoomWeekLabel(roomId) {
  const monday = getRoomWeekStart(roomId)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return `Semana del ${monday.toLocaleDateString('es-CL', { day: 'numeric', month: 'long' })} al ${sunday.toLocaleDateString('es-CL', { day: 'numeric', month: 'long' })}`
}

function getRoomSlotsForCell(roomId, dateStr, block) {
  const slots = roomSlots.value[roomId] || []
  return slots.filter(s => {
    const slotDate = s.startTime?.slice(0, 10)
    const slotHour = s.startTime?.slice(11, 13) + ':00'
    return slotDate === dateStr && slotHour === block.start
  })
}

async function loadRoomCalendar(roomId) {
  roomLoading.value = roomId
  try {
    const monday = getRoomWeekStart(roomId)
    const sunday = new Date(monday)
    sunday.setDate(monday.getDate() + 6)
    sunday.setHours(23, 59, 59, 999)
    const slots = await scheduleService.getRoomSchedule(roomId, monday.toISOString(), sunday.toISOString())
    // Solo los bloques disponibles son reservables en el buscador.
    roomSlots.value[roomId] = (Array.isArray(slots) ? slots : []).filter(s => s.status === 'AVAILABLE')
  } catch {
    roomSlots.value[roomId] = []
  }
  roomLoading.value = null
}

function prevRoomWeek(roomId) {
  const current = getRoomWeekStart(roomId)
  const d = new Date(current)
  d.setDate(d.getDate() - 7)
  roomWeekStarts.value[roomId] = d
  loadRoomCalendar(roomId)
}

function nextRoomWeek(roomId) {
  const current = getRoomWeekStart(roomId)
  const d = new Date(current)
  d.setDate(d.getDate() + 7)
  roomWeekStarts.value[roomId] = d
  loadRoomCalendar(roomId)
}

function toggleVenue(id) {
  expandedVenue.value = expandedVenue.value === id ? null : id
  expandedRoom.value = null
}

async function toggleRoom(roomId) {
  if (expandedRoom.value === roomId) { expandedRoom.value = null; return }
  expandedRoom.value = roomId
  if (!roomSlots.value[roomId]) {
    await loadRoomCalendar(roomId)
  }
}

function onRegionChange() {
  filtros.value.comuna = ''
  buscar()
}

async function buscar() {
  loading.value = true
  expandedVenue.value = null
  expandedRoom.value = null
  roomSlots.value = {}
  try {
    const data = await classService.getVenues()
    let list = Array.isArray(data) ? data : data?.content || []

    if (filtros.value.comuna) {
      const q = filtros.value.comuna.toLowerCase()
      list = list.filter(v => (v.city || '').toLowerCase().includes(q))
    }

    const venuesWithRooms = []
    for (const v of list) {
      try {
        const rooms = await venueService.getRooms(v.id)
        let filteredRooms = rooms

        if (filtros.value.caracteristicas.length > 0) {
          filteredRooms = filteredRooms.filter(room => {
            return filtros.value.caracteristicas.every(key => {
              switch (key) {
                case 'espejos': return room.hasMirrors
                case 'barraBallet': return room.tieneBarraBallet
                case 'pisoMadera': return (room.floorType || '').toLowerCase().includes('madera') || (room.floorType || '').toLowerCase().includes('wood')
                case 'pisoFlotante': return (room.floorType || '').toLowerCase().includes('flotante') || (room.floorType || '').toLowerCase().includes('floating')
                case 'aireAcondicionado': return room.tieneAireAcondicionado
                case 'insonorizado': return room.tieneInsonorizacion
                case 'sonido': return room.hasSound
                case 'amplificacion': return room.tieneAmplificacion
                case 'piano': return room.tienePiano
                case 'guitarra': return room.tieneGuitarra
                case 'bateria': return room.tieneBateria
                case 'grabacion': return room.tieneEquipoGrabacion
                case 'calefaccion': return room.tieneCalefaccion
                default: return true
              }
            })
          })
        }

        if (filtros.value.disciplina) {
          filteredRooms = filteredRooms.filter(room => {
            const charDan = getCaracteristicasDanza(room).length
            const charMus = getCaracteristicasMusica(room).length
            if (filtros.value.disciplina === 'Danza') return charDan > 0
            if (filtros.value.disciplina === 'Musica') return charMus > 0
            return true
          })
        }

        if (filteredRooms.length > 0) {
          venuesWithRooms.push({ ...v, rooms: filteredRooms, region: v.region || '' })
        }
      } catch (err) {
        console.error('Error al cargar salas de la sede', err)
      }
    }
    venues.value = venuesWithRooms
  } catch { venues.value = [] }
  loading.value = false
}

function confirmarAgendamiento(room, venue, slot) {
  if (!identidadValidada.value) {
    alertaIdentidad.value = true
    return
  }
  modal.value = { abierto: true, venue, room, slot, procesando: false }
}

async function pagar(metodo) {
  modal.value.procesando = true
  try {
    const blockId = modal.value.slot?.blockId || modal.value.slot?.id
    if (blockId) {
      const roomId = modal.value.room.id || modal.value.slot?.roomId
      await scheduleService.bookSlot(roomId, blockId, borradorId.value || null)
    } else if (borradorId.value) {
      await api.post(`/profesor/clases/${borradorId.value}/asignar-reserva`, {
        roomId: modal.value.room.id,
        startTime: modal.value.slot.startTime,
        duration: 60
      })
    } else {
      await api.post('/classes?draft=true', {
        title: 'Reserva - ' + modal.value.room.name,
        discipline: null,
        level: 'BASICO',
        capacity: modal.value.room.capacity,
        duration: 60,
        price: modal.value.room.pricePerHour || 0,
        startTime: modal.value.slot.startTime,
        roomId: modal.value.room.id
      })
    }
    modal.value.abierto = false
    try {
      await syncAtributos()
      if (puedeVerContextoProfesor.value) {
        setModo('profesor')
      }
    } catch (err) {
      console.error('Error al sincronizar atributos tras reserva', err)
    }
    if (puedeVerContextoProfesor.value) {
      if (!perfilProfesionalCompleto.value) {
        window.location.href = '/profesor/perfil-profesional?primeraVez=true'
      } else {
        window.location.href = '/profesor/clases-por-asignar'
      }
    } else {
      window.location.href = '/profesor/borradores'
    }
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al procesar la reserva')
  }
  modal.value.procesando = false
}

onMounted(async () => {
  await syncAtributos()
  await buscar()
})
</script>

<style scoped>
.equip-tag {
  @apply text-xs text-gray-400 bg-white/5 border border-white/10 px-2 py-0.5 rounded;
}
</style>