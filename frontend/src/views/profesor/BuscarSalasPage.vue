<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-4 flex-wrap gap-4">
      <div>
        <h1 class="text-3xl font-bold text-white mb-1">Agendar Sala</h1>
        <p class="text-gray-400">Encuentra la sala perfecta para tu clase.</p>
      </div>
      <button @click="vistaCalendario = !vistaCalendario" class="btn-primary text-sm flex items-center gap-2">
        <svg v-if="!vistaCalendario" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" stroke-width="2"/><path d="M3 10h18M8 2v4M16 2v4" stroke-width="2" stroke-linecap="round"/></svg>
        <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16"/></svg>
        {{ vistaCalendario ? 'Vista Lista' : 'Vista Calendario' }}
      </button>
    </div>
    <!-- Banner cuando asignamos sala a un borrador existente -->
    <div v-if="borradorId" class="bg-blue-500/10 border border-blue-500/30 rounded-xl px-4 py-3 mb-6 flex items-center gap-3">
      <svg class="w-5 h-5 text-blue-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      <p class="text-blue-300 text-sm">Selecciona un horario para asignarlo a tu clase y publicarla.</p>
    </div>

    <!-- Filtros -->
    <div class="card mb-6 grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
      <select v-model="filtros.region" class="input-field text-sm py-2" @change="onRegionChange">
        <option value="">TODAS las regiones</option>
        <option v-for="r in regiones" :key="r" :value="r">{{ r }}</option>
      </select>
      <select v-model="filtros.comuna" class="input-field text-sm py-2" @change="buscar">
        <option value="">TODAS las comunas</option>
        <option v-for="c in comunasFiltradas" :key="c" :value="c">{{ c }}</option>
      </select>
      <select v-model="filtros.tipo" class="input-field text-sm py-2" @change="buscar">
        <option value="">TODOS los tipos</option>
        <option value="DANZA">Danza</option>
        <option value="MUSICA">Música</option>
      </select>
      <div>
        <label class="text-xs text-gray-500 mb-0.5 block">Desde</label>
        <input v-model="filtros.fechaDesde" type="date" class="input-field text-sm py-2" @change="buscar" />
      </div>
      <div>
        <label class="text-xs text-gray-500 mb-0.5 block">Hasta</label>
        <input v-model="filtros.fechaHasta" type="date" class="input-field text-sm py-2" @change="buscar" />
      </div>
      <button @click="buscar" class="btn-primary text-sm">Buscar Salas</button>
    </div>

    <!-- Vista Calendario -->
    <div v-if="vistaCalendario" class="space-y-4">
      <!-- Filtros checkbox para calendario -->
      <div class="card">
        <p class="text-xs text-gray-500 mb-3">Filtrar salas:</p>
        <div class="flex flex-wrap gap-4">
          <label v-for="filtro in calendarFilters" :key="filtro.key" class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="filtro.checked" @change="onCalendarFilterChange" class="w-4 h-4 rounded border-dark-border bg-dark-bg text-primary focus:ring-primary/50" />
            <span class="text-sm text-gray-300">{{ filtro.label }}</span>
          </label>
        </div>
      </div>

      <div v-if="calendarLoading" class="text-center text-gray-400 py-20">Cargando calendario...</div>
      <div v-else-if="calendarSlots.length === 0" class="card text-center py-12"><p class="text-gray-400">No hay horarios disponibles con estos filtros.</p></div>

      <!-- Calendario semanal -->
      <div v-else class="card overflow-x-auto">
        <div class="flex items-center justify-between mb-4">
          <button @click="prevCalendarWeek" class="text-sm text-gray-400 hover:text-white px-3 py-1 rounded hover:bg-dark-border">← Anterior</button>
          <h3 class="text-white font-medium">{{ calendarWeekLabel }}</h3>
          <button @click="nextCalendarWeek" class="text-sm text-gray-400 hover:text-white px-3 py-1 rounded hover:bg-dark-border">Siguiente →</button>
        </div>

        <table class="w-full border-collapse">
          <thead>
            <tr>
              <th class="w-24 p-2 text-left text-xs text-gray-500 font-medium"></th>
              <th v-for="day in calendarWeekDays" :key="day.date" class="p-2 text-center text-xs font-medium text-gray-300 min-w-[120px]">
                {{ day.label }}<br /><span class="font-normal opacity-60">{{ day.dateFormatted }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="block in calendarTimeBlocks" :key="block.key">
              <td class="p-2 text-xs text-gray-500 align-top whitespace-nowrap">{{ block.label }}</td>
              <td v-for="day in calendarWeekDays" :key="day.date" class="p-1 align-top">
                <div
                  v-for="slot in getSlotsForCell(day.date, block)"
                  :key="slot.id"
                  :style="{ backgroundColor: getRoomColor(slot.roomId) + '20', borderColor: getRoomColor(slot.roomId) + '50' }"
                  class="rounded p-1 mb-0.5 cursor-pointer hover:opacity-80 border text-xs transition-opacity"
                  @click="openCalendarSlot(slot)"
                >
                  <span class="text-white text-[10px] block truncate">{{ slot.roomName }}</span>
                  <span class="text-gray-400 text-[9px]">{{ slot.venueName }}</span>
                </div>
                <div v-if="!getSlotsForCell(day.date, block).length" class="min-h-[32px]"></div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Leyenda -->
        <div class="flex items-center gap-6 mt-4 pt-4 border-t border-dark-border flex-wrap">
          <span v-for="room in calendarRooms" :key="room.id" class="flex items-center gap-1.5 text-xs text-gray-400">
            <span class="w-3 h-3 rounded-sm border" :style="{ backgroundColor: getRoomColor(room.id) + '40', borderColor: getRoomColor(room.id) + '60' }"></span>
            {{ room.name }}
          </span>
        </div>
      </div>
    </div>

    <!-- Vista Lista (original) -->
    <template v-if="!vistaCalendario">
      <div v-if="loading" class="text-center text-gray-400 py-20">Buscando salas disponibles...</div>
      <div v-else-if="venues.length === 0" class="card text-center py-12"><p class="text-gray-400">No se encontraron salas con esos filtros.</p></div>
      <div v-else class="space-y-4">
        <div v-for="venue in venues" :key="venue.id" class="card">
          <div @click="toggleVenue(venue.id)" class="cursor-pointer flex items-center justify-between">
            <div>
              <h3 class="text-lg font-semibold text-white">{{ venue.name }}</h3>
              <p class="text-gray-400 text-sm">{{ venue.address }}, {{ venue.city }}</p>
            </div>
            <svg :class="['w-5 h-5 text-gray-400 transition-transform', expandedVenue === venue.id && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
          </div>

          <div v-if="expandedVenue === venue.id" class="mt-4 space-y-3">
            <div v-if="venue.rooms?.length">
              <div v-for="room in venue.rooms" :key="room.id" class="bg-[#1a1d2e] rounded-xl p-4">
                <div @click="toggleRoom(room.id)" class="cursor-pointer flex items-center justify-between">
                  <div class="flex-1">
                    <p class="text-white font-medium">{{ room.name }}</p>
                    <p class="text-gray-400 text-sm">
                      Cap: {{ room.capacity }} pers.
                      <span v-if="room.tamanoM2"> · {{ room.tamanoM2 }} m²</span>
                      <span v-if="room.floorType"> · Piso {{ room.floorType }}</span>
                    </p>
                    <p v-if="room.pricePerHour" class="text-primary text-sm font-medium mt-0.5">${{ room.pricePerHour?.toLocaleString() }} / hora</p>
                    <div class="flex flex-wrap gap-1 mt-1.5">
                      <span v-if="room.hasMirrors" class="equip-tag">Espejos</span>
                      <span v-if="room.tieneBarraBallet" class="equip-tag">Barra ballet</span>
                      <span v-if="room.tieneAireAcondicionado" class="equip-tag">Aire AC</span>
                      <span v-if="room.tieneCalefaccion" class="equip-tag">Calefaccion</span>
                      <span v-if="room.tieneInsonorizacion" class="equip-tag">Insonorizado</span>
                      <span v-if="room.hasSound" class="equip-tag">Sonido</span>
                      <span v-if="room.tieneAmplificacion" class="equip-tag">Amplificacion</span>
                      <span v-if="room.tieneEntradaAuxiliar" class="equip-tag">AUX</span>
                      <span v-if="room.tieneMicrofono" class="equip-tag">Microfono</span>
                      <span v-if="room.tieneEquipoGrabacion" class="equip-tag">Grabacion</span>
                      <span v-if="room.tienePiano" class="equip-tag">Piano</span>
                      <span v-if="room.tieneGuitarra" class="equip-tag">Guitarra</span>
                      <span v-if="room.tieneBateria" class="equip-tag">Bateria</span>
                    </div>
                  </div>
                  <svg :class="['w-4 h-4 text-gray-500 ml-3 transition-transform', expandedRoom === room.id && 'rotate-180']" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
                </div>

                <div v-if="expandedRoom === room.id" class="mt-3">
                  <div v-if="loadingSlots === room.id" class="text-gray-500 text-sm py-2">Cargando horarios...</div>
                  <div v-else-if="roomSlots[room.id]?.length" class="space-y-2">
                    <p class="text-xs text-gray-500 mb-2">Horarios disponibles:</p>
                    <div v-for="slot in filteredSlots(room.id)" :key="slot.id" class="flex items-center justify-between bg-[#0d0f1a] rounded-lg p-3">
                      <div>
                        <p class="text-white text-sm">{{ formatDate(slot.startTime) }}</p>
                        <p class="text-gray-400 text-xs">{{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}</p>
                      </div>
                      <button @click="confirmarAgendamiento(room, venue, slot)" class="btn-primary text-xs !py-1.5 !px-3">Agendar</button>
                    </div>
                  </div>
                  <div v-else class="text-gray-500 text-sm py-2">Sin horarios disponibles en este rango</div>
                </div>
              </div>
            </div>
            <p v-else class="text-gray-500 text-sm">No hay salas en esta sede</p>
          </div>
        </div>
      </div>
    </template>

    <!-- Alerta identidad no validada -->
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

    <!-- Modal de confirmacion -->
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
import { ref, computed, onMounted, watch } from 'vue'
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
const loadingSlots = ref(null)
const comunas = ref([])
const modal = ref({ abierto: false, venue: null, room: null, slot: null, procesando: false })
const filtros = ref({ region: '', comuna: '', tipo: '', fechaDesde: '', fechaHasta: '' })

// ── Vista Calendario ──
const vistaCalendario = ref(false)
const calendarLoading = ref(false)
const calendarSlots = ref([])
const calendarRooms = ref([])
const calendarCurrentWeekStart = ref(getMonday(new Date()))

const ROOM_COLORS = [
  '#6366f1', '#8b5cf6', '#d946ef', '#ec4899', '#f43f5e',
  '#f97316', '#eab308', '#22c55e', '#14b8a6', '#06b6d4',
  '#3b82f6', '#a855f7'
]

function getRoomColor(roomId) {
  const idx = calendarRooms.value.findIndex(r => r.id === roomId)
  return ROOM_COLORS[Math.abs(idx) % ROOM_COLORS.length]
}

function getMonday(d) {
  const date = new Date(d)
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  date.setHours(0, 0, 0, 0)
  return date
}

const calendarFilters = ref([
  { key: 'danza', label: 'Danza', checked: false },
  { key: 'musica', label: 'Música', checked: false },
  { key: 'espejo', label: 'Con espejo', checked: false },
  { key: 'sinEspejo', label: 'Sin espejo', checked: false },
  { key: 'madera', label: 'Piso madera', checked: false },
  { key: 'flotante', label: 'Piso flotante', checked: false }
])

const calendarWeekDays = computed(() => {
  const result = []
  const monday = new Date(calendarCurrentWeekStart.value)
  const dowLabels = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
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

const calendarWeekLabel = computed(() => {
  const monday = new Date(calendarCurrentWeekStart.value)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return `Semana del ${monday.toLocaleDateString('es-CL', { day: 'numeric', month: 'long' })} al ${sunday.toLocaleDateString('es-CL', { day: 'numeric', month: 'long' })}`
})

const calendarTimeBlocks = computed(() => {
  const blocks = []
  for (let h = 8; h <= 21; h++) {
    const start = `${String(h).padStart(2, '0')}:00`
    const end = `${String(h).padStart(2, '0')}:00`
    blocks.push({ key: start + '-' + end, label: start, start, end })
  }
  return blocks
})

function getSlotsForCell(dateStr, block) {
  return calendarSlots.value.filter(s => {
    const slotDate = s.startTime?.slice(0, 10)
    const slotHour = s.startTime?.slice(11, 13) + ':00'
    return slotDate === dateStr && slotHour === block.start
  })
}

function onCalendarFilterChange() {
  loadCalendarData()
}

async function loadCalendarData() {
  calendarLoading.value = true
  try {
    const from = calendarWeekDays.value[0].date + 'T00:00:00'
    const to = calendarWeekDays.value[6].date + 'T23:59:59'
    const data = await scheduleService.searchAvailableRooms(from, to)
    let allSlots = Array.isArray(data) ? data : data?.content || []

    const activeFilters = calendarFilters.value.filter(f => f.checked).map(f => f.key)
    if (activeFilters.length > 0) {
      allSlots = allSlots.filter(slot => {
        let include = false
        const t = (slot.roomType || slot.type || '').toLowerCase()
        const ft = (slot.floorType || slot.tipoPiso || '').toLowerCase()
        if (activeFilters.includes('danza') && t === 'danza') include = true
        if (activeFilters.includes('musica') && t === 'musica') include = true
        if (activeFilters.includes('espejo') && slot.hasMirrors) include = true
        if (activeFilters.includes('sinEspejo') && !slot.hasMirrors) include = true
        if (activeFilters.includes('madera') && (ft.includes('madera') || ft.includes('wood'))) include = true
        if (activeFilters.includes('flotante') && (ft.includes('flotante') || ft.includes('floating'))) include = true
        return include
      })
    }

    const roomsMap = new Map()
    for (const slot of allSlots) {
      const rid = slot.roomId
      if (rid && !roomsMap.has(rid)) {
        roomsMap.set(rid, { id: rid, name: slot.roomName || '' })
      }
    }

    calendarRooms.value = Array.from(roomsMap.values())
    calendarSlots.value = allSlots
  } catch { calendarSlots.value = [] }
  calendarLoading.value = false
}

function openCalendarSlot(slot) {
  confirmarAgendamiento(
    slot.room || { id: slot.roomId, name: slot.roomName, capacity: slot.capacity, pricePerHour: slot.pricePerHour },
    slot.venue || { name: slot.venueName },
    slot
  )
}

function prevCalendarWeek() {
  const d = new Date(calendarCurrentWeekStart.value)
  d.setDate(d.getDate() - 7)
  calendarCurrentWeekStart.value = d
  loadCalendarData()
}

function nextCalendarWeek() {
  const d = new Date(calendarCurrentWeekStart.value)
  d.setDate(d.getDate() + 7)
  calendarCurrentWeekStart.value = d
  loadCalendarData()
}

watch(vistaCalendario, (val) => {
  if (val) loadCalendarData()
})
const regiones = [
  'XV - Arica y Parinacota', 'I - Tarapaca', 'II - Antofagasta', 'III - Atacama',
  'IV - Coquimbo', 'V - Valparaiso', 'RM - Metropolitana', "VI - O'Higgins",
  'VII - Maule', 'XVI - Ñuble', 'VIII - Biobio', 'IX - La Araucania',
  'XIV - Los Rios', 'X - Los Lagos', 'XI - Aysen', 'XII - Magallanes'
]

const comunasPorRegion = {
  'XV - Arica y Parinacota': ['Arica', 'Camarones', 'Putre', 'General Lagos'],
  'I - Tarapaca': ['Iquique', 'Alto Hospicio', 'Pozo Almonte', 'Camina', 'Colchane', 'Huara', 'Pica'],
  'II - Antofagasta': ['Antofagasta', 'Mejillones', 'Sierra Gorda', 'Taltal', 'Calama', 'Ollague', 'San Pedro de Atacama', 'Tocopilla', 'Maria Elena'],
  'III - Atacama': ['Copiapo', 'Caldera', 'Tierra Amarilla', 'Chanaral', 'Diego de Almagro', 'Vallenar', 'Alto del Carmen', 'Freirina', 'Huasco'],
  'IV - Coquimbo': ['La Serena', 'Coquimbo', 'Andacollo', 'La Higuera', 'Paiguano', 'Vicuna', 'Illapel', 'Canela', 'Los Vilos', 'Salamanca', 'Ovalle', 'Combarbala', 'Monte Patria', 'Punitaqui', 'Rio Hurtado'],
  'V - Valparaiso': ['Valparaiso', 'Vina del Mar', 'Concon', 'Quintero', 'Puchuncavi', 'Quilpue', 'Villa Alemana', 'Limache', 'Olmue', 'San Antonio', 'Cartagena', 'El Quisco', 'El Tabo', 'Algarrobo', 'Santo Domingo', 'San Felipe', 'Los Andes', 'Catemu', 'Llay-Llay', 'Panquehue', 'Putaendo', 'Santa Maria', 'Quillota', 'La Calera', 'Hijuelas', 'La Cruz', 'Nogales', 'Petorca', 'Cabildo', 'Papudo', 'La Ligua', 'Zapallar', 'Isla de Pascua', 'Juan Fernandez'],
  'RM - Metropolitana': ['Santiago', 'Cerrillos', 'Cerro Navia', 'Conchali', 'El Bosque', 'Estacion Central', 'Huechuraba', 'Independencia', 'La Cisterna', 'La Florida', 'La Granja', 'La Pintana', 'La Reina', 'Las Condes', 'Lo Barnechea', 'Lo Espejo', 'Lo Prado', 'Macul', 'Maipu', 'Nuñoa', 'Pedro Aguirre Cerda', 'Peñalolen', 'Providencia', 'Pudahuel', 'Quilicura', 'Quinta Normal', 'Recoleta', 'Renca', 'San Joaquin', 'San Miguel', 'San Ramon', 'Vitacura', 'Puente Alto', 'Pirque', 'San Jose de Maipo', 'Colina', 'Lampa', 'Tiltil', 'San Bernardo', 'Buin', 'Calera de Tango', 'Paine', 'Melipilla', 'Alhue', 'Curacavi', 'Maria Pinto', 'San Pedro', 'Talagante', 'El Monte', 'Isla de Maipo', 'Padre Hurtado', 'Peñaflor'],
  "VI - O'Higgins": ['Rancagua', 'Codegua', 'Coinco', 'Coltauco', 'Doñihue', 'Graneros', 'Las Cabras', 'Machali', 'Malloa', 'Mostazal', 'Olivar', 'Peumo', 'Pichidegua', 'Quinta de Tilcoco', 'Rengo', 'Requinoa', 'San Vicente', 'Pichilemu', 'La Estrella', 'Litueche', 'Marchihue', 'Navidad', 'Paredones', 'San Fernando', 'Chepica', 'Chimbarongo', 'Lolol', 'Nancagua', 'Placilla', 'Pumanque', 'Santa Cruz'],
  'VII - Maule': ['Talca', 'Constitucion', 'Curepto', 'Empedrado', 'Maule', 'Pelarco', 'Pencahue', 'Rio Claro', 'San Clemente', 'San Rafael', 'Cauquenes', 'Chanco', 'Pelluhue', 'Curico', 'Hualañe', 'Licanten', 'Molina', 'Rauco', 'Romeral', 'Sagrada Familia', 'Teno', 'Vichuquen', 'Linares', 'Colbun', 'Longavi', 'Parral', 'Retiro', 'San Javier', 'Villa Alegre', 'Yerbas Buenas'],
  'XVI - Ñuble': ['Chillan', 'Bulnes', 'Cobquecura', 'Coelemu', 'Coihueco', 'Chillan Viejo', 'El Carmen', 'Ninhue', 'Ñiquen', 'Pemuco', 'Pinto', 'Portezuelo', 'Quillon', 'Quirihue', 'Ranquil', 'San Carlos', 'San Fabian', 'San Ignacio', 'San Nicolas', 'Trehuaco', 'Yungay'],
  'VIII - Biobio': ['Concepcion', 'Coronel', 'Chiguayante', 'Florida', 'Hualqui', 'Lota', 'Penco', 'San Pedro de la Paz', 'Santa Juana', 'Talcahuano', 'Tome', 'Hualpen', 'Lebu', 'Arauco', 'Cañete', 'Contulmo', 'Curanilahue', 'Los Alamos', 'Tirua', 'Los Angeles', 'Antuco', 'Cabrero', 'Laja', 'Mulchen', 'Nacimiento', 'Negrete', 'Quilaco', 'Quilleco', 'San Rosendo', 'Santa Barbara', 'Tucapel', 'Yumbel', 'Alto Biobio'],
  'IX - La Araucania': ['Temuco', 'Carahue', 'Cunco', 'Curarrehue', 'Freire', 'Galvarino', 'Gorbea', 'Lautaro', 'Loncoche', 'Melipeuco', 'Nueva Imperial', 'Padre Las Casas', 'Perquenco', 'Pitrufquen', 'Pucon', 'Saavedra', 'Teodoro Schmidt', 'Tolten', 'Vilcun', 'Villarrica', 'Cholchol', 'Angol', 'Collipulli', 'Curacautin', 'Ercilla', 'Lonquimay', 'Los Sauces', 'Lumaco', 'Puren', 'Renaico', 'Traiguen', 'Victoria'],
  'XIV - Los Rios': ['Valdivia', 'Corral', 'Lanco', 'Los Lagos', 'Mafil', 'Mariquina', 'Paillaco', 'Panguipulli', 'La Union', 'Futrono', 'Lago Ranco', 'Rio Bueno'],
  'X - Los Lagos': ['Puerto Montt', 'Calbuco', 'Cochamo', 'Fresia', 'Frutillar', 'Los Muermos', 'Llanquihue', 'Maullin', 'Puerto Varas', 'Castro', 'Ancud', 'Chonchi', 'Curaco de Velez', 'Dalcahue', 'Puqueldon', 'Queilen', 'Quellon', 'Quemchi', 'Quinchao', 'Osorno', 'Puerto Octay', 'Purranque', 'Puyehue', 'Rio Negro', 'San Juan de la Costa', 'San Pablo', 'Chaiten', 'Futaleufu', 'Hualaihue', 'Palena'],
  'XI - Aysen': ['Coyhaique', 'Lago Verde', 'Aysen', 'Cisnes', 'Guaitecas', 'Cochrane', 'O\'Higgins', 'Tortel', 'Chile Chico', 'Rio Ibañez'],
  'XII - Magallanes': ['Punta Arenas', 'Laguna Blanca', 'Rio Verde', 'San Gregorio', 'Cabo de Hornos', 'Antartica', 'Porvenir', 'Primavera', 'Timaukel', 'Natales', 'Torres del Paine']
}

const comunasFiltradas = computed(() => {
  if (filtros.value.region && comunasPorRegion[filtros.value.region]) {
    return comunasPorRegion[filtros.value.region]
  }
  return comunas.value
})

onMounted(async () => {
  await syncAtributos()
  await cargarComunas()
  await buscar()
})

async function cargarComunas() {
  try {
    const data = await classService.getVenues()
    const list = Array.isArray(data) ? data : data?.content || []
    comunas.value = [...new Set(list.map(v => v.city).filter(Boolean))].sort()
  } catch { comunas.value = [] }
}

async function buscar() {
  loading.value = true
  expandedVenue.value = null
  expandedRoom.value = null
  roomSlots.value = {}
  try {
    const data = await classService.getVenues()
    let list = Array.isArray(data) ? data : data?.content || []
    if (filtros.value.comuna) list = list.filter(v => v.city === filtros.value.comuna)

    const venuesWithAvailableRooms = []
    for (const v of list) {
      try {
        const rooms = await venueService.getRooms(v.id)
        const roomsWithSlots = []
        for (const room of rooms) {
          try {
            const slots = await venueService.getPublicRoomAvailability(room.id)
            const available = Array.isArray(slots) ? slots : []
            let filtered = available
            if (filtros.value.fechaDesde) {
              filtered = filtered.filter(s => new Date(s.startTime) >= new Date(filtros.value.fechaDesde + 'T00:00:00'))
            }
            if (filtros.value.fechaHasta) {
              filtered = filtered.filter(s => new Date(s.endTime) <= new Date(filtros.value.fechaHasta + 'T23:59:59'))
            }
            if (filtered.length > 0) {
              roomsWithSlots.push(room)
              roomSlots.value[room.id] = filtered
            }
          } catch (err) {
            console.error('Error al cargar disponibilidad de sala', err)
          }
        }
        if (roomsWithSlots.length > 0) {
          venuesWithAvailableRooms.push({ ...v, rooms: roomsWithSlots })
        }
        } catch (err) {
          console.error('Error al cargar salas de la sede', err)
        }
    }
    venues.value = venuesWithAvailableRooms
  } catch { venues.value = [] }
  loading.value = false
}

function onRegionChange() {
  filtros.value.comuna = ''
  buscar()
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
    // Sincronizar atributos y roles (el backend puede haber asignado TEACHER en este paso)
    try {
      await syncAtributos()
      if (puedeVerContextoProfesor.value) {
        setModo('profesor')
      }
    } catch (err) {
      console.error('Error al sincronizar atributos tras reserva', err)
    }
    // Redirigir segun estado:
    // 1) Con rol TEACHER y perfil incompleto → completar perfil profesional (notificación)
    // 2) Con rol TEACHER y perfil completo → clases por asignar
    // 3) Sin rol TEACHER → borradores (fallback)
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

function toggleVenue(id) {
  expandedVenue.value = expandedVenue.value === id ? null : id
  expandedRoom.value = null
}

async function toggleRoom(roomId) {
  if (expandedRoom.value === roomId) { expandedRoom.value = null; return }
  expandedRoom.value = roomId
  if (!roomSlots.value[roomId]) {
    loadingSlots.value = roomId
    try {
      const slots = await venueService.getPublicRoomAvailability(roomId)
      roomSlots.value[roomId] = Array.isArray(slots) ? slots : []
    } catch {
      roomSlots.value[roomId] = []
    }
    loadingSlots.value = null
  }
}

function filteredSlots(roomId) {
  const slots = roomSlots.value[roomId] || []
  if (!filtros.value.fechaDesde && !filtros.value.fechaHasta) return slots
  return slots.filter(s => {
    const d = new Date(s.startTime).toISOString().split('T')[0]
    if (filtros.value.fechaDesde && d < filtros.value.fechaDesde) return false
    if (filtros.value.fechaHasta && d > filtros.value.fechaHasta) return false
    return true
  })
}

</script>

<style scoped>
.equip-tag {
  @apply text-xs text-gray-400 bg-white/5 border border-white/10 px-2 py-0.5 rounded;
}
</style>
