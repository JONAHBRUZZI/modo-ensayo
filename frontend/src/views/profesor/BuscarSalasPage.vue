<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-4">Agendar Sala</h1>
    <p class="text-gray-400 mb-6">Encuentra la sala perfecta para tu clase.</p>

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
        <option value="MUSICA">Musica</option>
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

    <!-- Resultados -->
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
                  <p class="text-gray-400 text-sm">Cap: {{ room.capacity }} | Piso: {{ room.floorType || 'N/A' }}</p>
                  <p v-if="room.pricePerHour" class="text-primary text-sm font-medium mt-0.5">${{ room.pricePerHour?.toLocaleString() }} / hora</p>
                  <div class="flex flex-wrap gap-1 mt-1">
                    <span v-if="room.hasMirrors" class="text-xs text-gray-500 bg-white/5 px-2 py-0.5 rounded">Espejos</span>
                    <span v-if="room.hasSound" class="text-xs text-gray-500 bg-white/5 px-2 py-0.5 rounded">Sonido</span>
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
import { ref, computed, onMounted } from 'vue'
import classService from '@/services/classService'
import venueService from '@/services/venueService'
import api from '@/services/api'
import { useAuth } from '@/stores/auth'

const auth = useAuth()

const venues = ref([])
const loading = ref(true)
const expandedVenue = ref(null)
const expandedRoom = ref(null)
const roomSlots = ref({})
const loadingSlots = ref(null)
const comunas = ref([])
const modal = ref({ abierto: false, venue: null, room: null, slot: null, procesando: false })
const filtros = ref({ region: '', comuna: '', tipo: '', fechaDesde: '', fechaHasta: '' })
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
    const venuesWithRooms = await Promise.all(list.map(async (v) => {
      try {
        const rooms = await venueService.getRooms(v.id)
        return { ...v, rooms: Array.isArray(rooms) ? rooms : [] }
      } catch {
        return { ...v, rooms: [] }
      }
    }))
    venues.value = venuesWithRooms
  } catch { venues.value = [] }
  loading.value = false
}

function onRegionChange() {
  filtros.value.comuna = ''
  buscar()
}

function confirmarAgendamiento(room, venue, slot) {
  modal.value = { abierto: true, venue, room, slot, procesando: false }
}

async function pagar(metodo) {
  modal.value.procesando = true
  try {
    await api.post('/classes?draft=true', {
      title: 'Reserva - ' + modal.value.room.name,
      discipline: 'OTRO',
      level: 'BASICO',
      capacity: modal.value.room.capacity,
      duration: 60,
      price: modal.value.room.pricePerHour || 0,
      startTime: modal.value.slot.startTime,
      roomId: modal.value.room.id
    })
    modal.value.abierto = false
    alert('Reserva confirmada. Se activo tu perfil de Maestro.')
    // Refrescar perfil para obtener rol TEACHER
    try { await auth.refreshProfile() } catch {}
    window.location.href = '/alumno/dashboard'
  } catch (e) {
    alert(e?.response?.data?.message || 'Error al procesar la reserva')
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

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('es-CL', { weekday: 'short', day: 'numeric', month: 'short' }) : ''
}
function formatTime(d) {
  return d ? new Date(d).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' }) : ''
}
</script>
