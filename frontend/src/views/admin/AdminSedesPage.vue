<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-8 flex-wrap gap-4">
      <div>
        <h1 class="text-3xl font-bold text-white">Gestión de Sedes</h1>
        <p class="text-gray-400 text-sm mt-1">Todas las sedes registradas en el sistema</p>
      </div>
      <router-link to="/admin/roles"
                   class="text-xs px-4 py-2 rounded-lg bg-yellow-500/10 text-yellow-300 border border-yellow-500/30 hover:bg-yellow-500/20 transition-colors">
        Ver solicitudes pendientes ({{ countPendientes }})
      </router-link>
    </div>

    <!-- Tabs de filtro por estado -->
    <div class="flex flex-wrap gap-2 mb-6">
      <button v-for="t in tabs" :key="t.value"
              @click="filtroActivo = t.value"
              :class="[
                'px-4 py-2 rounded-lg text-sm transition-colors',
                filtroActivo === t.value
                  ? 'bg-primary text-white'
                  : 'bg-[#1a1d2e] text-gray-400 hover:text-white border border-white/10'
              ]">
        {{ t.label }}
        <span class="ml-2 text-xs opacity-70">({{ contarPorEstado(t.value) }})</span>
      </button>
    </div>

    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando sedes...</div>

    <div v-else-if="sedesFiltradas.length === 0" class="card text-center py-12">
      <p class="text-gray-400">No hay sedes en este estado.</p>
    </div>

    <div v-else class="space-y-4">
      <div v-for="s in sedesFiltradas" :key="s.id" class="card">
        <div class="flex items-start justify-between flex-wrap gap-4">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <h3 class="text-white font-semibold text-lg">{{ s.name }}</h3>
              <span class="text-xs px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-300 border border-blue-500/30">
                {{ tipoLabel(s.tipo) }}
              </span>
              <span :class="['text-xs px-2 py-0.5 rounded-full border', estadoClase(s.status)]">
                {{ estadoLabel(s.status) }}
              </span>
            </div>

            <p class="text-gray-400 text-sm mt-1">{{ s.address || 'Sin direccion' }}<span v-if="s.city"> · {{ s.city }}</span></p>

            <div class="mt-3 grid grid-cols-1 md:grid-cols-2 gap-2 text-xs text-gray-400">
              <p v-if="s.adminFullName"><span class="text-gray-500">Admin:</span> <span class="text-white">{{ s.adminFullName }}</span> <span class="text-gray-500">({{ s.adminEmail }})</span></p>
              <p v-if="s.phone"><span class="text-gray-500">Teléfono:</span> <span class="text-white">{{ s.phone }}</span></p>
              <p v-if="s.email"><span class="text-gray-500">Email sede:</span> <span class="text-white">{{ s.email }}</span></p>
              <p v-if="s.createdAt"><span class="text-gray-500">Registrada:</span> <span class="text-white">{{ formatDate(s.createdAt) }}</span></p>
            </div>

            <p v-if="s.description" class="text-gray-300 text-sm mt-3">{{ s.description }}</p>

            <p v-if="s.status === 'RECHAZADA' && s.rejectionReason"
               class="mt-3 text-xs text-red-300 bg-red-500/10 border border-red-500/30 rounded px-3 py-2">
              <span class="font-semibold">Motivo de rechazo:</span> {{ s.rejectionReason }}
            </p>
            <p v-if="s.status === 'SUSPENDIDA' && s.rejectionReason"
               class="mt-3 text-xs text-orange-300 bg-orange-500/10 border border-orange-500/30 rounded px-3 py-2">
              <span class="font-semibold">Motivo de suspension:</span> {{ s.rejectionReason }}
            </p>
          </div>

          <div class="flex flex-col gap-2 flex-shrink-0 min-w-[110px]">
            <router-link :to="'/admin/sedes/' + s.id + '/documentos'"
                         class="text-xs px-3 py-1.5 rounded-lg bg-[#1a1d2e] border border-white/10 text-gray-300 hover:text-white text-center">
              Documentos
            </router-link>
            <button v-if="s.status === 'PENDIENTE_APROBACION'"
                    @click="aprobar(s)"
                    class="text-xs px-3 py-1.5 rounded-lg bg-green-500/20 text-green-300 hover:bg-green-500/30 transition-colors">
              Aprobar
            </button>
            <button v-if="s.status === 'PENDIENTE_APROBACION'"
                    @click="rechazar(s)"
                    class="text-xs px-3 py-1.5 rounded-lg bg-red-500/20 text-red-300 hover:bg-red-500/30 transition-colors">
              Rechazar
            </button>
            <!-- Sede APROBADA: se puede SUSPENDER -->
            <button v-if="s.status === 'APROBADA'"
                    @click="suspender(s)"
                    class="text-xs px-3 py-1.5 rounded-lg bg-orange-500/20 text-orange-300 hover:bg-orange-500/30 transition-colors">
              Suspender
            </button>
            <!-- Sede SUSPENDIDA: se puede REACTIVAR -->
            <button v-if="s.status === 'SUSPENDIDA'"
                    @click="reactivar(s)"
                    class="text-xs px-3 py-1.5 rounded-lg bg-green-500/20 text-green-300 hover:bg-green-500/30 transition-colors">
              Reactivar
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import adminService from '@/services/adminService'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const sedes = ref([])
const loading = ref(true)
const filtroActivo = ref('TODOS')

const tabs = [
  { value: 'TODOS', label: 'Todas' },
  { value: 'PENDIENTE_APROBACION', label: 'Pendientes' },
  { value: 'APROBADA', label: 'Aprobadas' },
  { value: 'SUSPENDIDA', label: 'Suspendidas' },
  { value: 'RECHAZADA', label: 'Rechazadas' }
]

const sedesFiltradas = computed(() => {
  if (filtroActivo.value === 'TODOS') return sedes.value
  return sedes.value.filter(s => s.status === filtroActivo.value)
})

const countPendientes = computed(() => sedes.value.filter(s => s.status === 'PENDIENTE_APROBACION').length)

function contarPorEstado(estado) {
  if (estado === 'TODOS') return sedes.value.length
  return sedes.value.filter(s => s.status === estado).length
}

onMounted(async () => {
  await cargar()
})

async function cargar() {
  loading.value = true
  try {
    const data = await adminService.getAllVenues()
    sedes.value = Array.isArray(data) ? data : []
  } catch {
    sedes.value = []
  }
  loading.value = false
}

async function aprobar(sede) {
  if (!confirm(`Aprobar la sede "${sede.name}"?`)) return
  try {
    await adminService.approveVenue(sede.id)
    await cargar()
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al aprobar la sede')
  }
}

async function rechazar(sede) {
  const motivo = prompt(`Motivo del rechazo de "${sede.name}":`)
  if (motivo === null) return
  try {
    await adminService.rejectVenue(sede.id, motivo || 'No especificado')
    await cargar()
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al rechazar la sede')
  }
}

// Suspende una sede APROBADA (reversible). Pide motivo opcional.
async function suspender(sede) {
  const motivo = prompt(`Motivo de la suspension de "${sede.name}":\n(opcional, pero recomendado para que el Admin de Sede lo entienda)`)
  if (motivo === null) return  // cancelado
  try {
    await adminService.toggleVenue(sede.id, motivo || '')
    await cargar()
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al suspender la sede')
  }
}

// Reactiva una sede SUSPENDIDA (vuelve a APROBADA).
async function reactivar(sede) {
  if (!confirm(`Reactivar la sede "${sede.name}"? Volvera a poder recibir reservas.`)) return
  try {
    await adminService.toggleVenue(sede.id, '')
    await cargar()
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al reactivar la sede')
  }
}

function tipoLabel(tipo) {
  if (tipo === 'HOME_STUDIO') return 'Home Studio'
  if (tipo === 'SEDE') return 'Sede'
  return tipo || 'Sin tipo'
}

function estadoLabel(s) {
  const map = {
    'PENDIENTE_APROBACION': 'Pendiente',
    'APROBADA': 'Aprobada',
    'RECHAZADA': 'Rechazada',
    'SUSPENDIDA': 'Suspendida'
  }
  return map[s] || s
}

function estadoClase(s) {
  if (s === 'APROBADA') return 'bg-green-500/10 text-green-300 border-green-500/30'
  if (s === 'PENDIENTE_APROBACION') return 'bg-yellow-500/10 text-yellow-300 border-yellow-500/30'
  if (s === 'RECHAZADA') return 'bg-red-500/10 text-red-300 border-red-500/30'
  if (s === 'SUSPENDIDA') return 'bg-orange-500/10 text-orange-300 border-orange-500/30'
  return 'bg-gray-500/10 text-gray-300 border-gray-500/30'
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', year: 'numeric' })
}
</script>
