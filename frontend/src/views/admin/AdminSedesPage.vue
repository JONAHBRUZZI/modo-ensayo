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

            <p class="text-gray-400 text-sm mt-1">{{ s.address || 'Sin direccion' }}<span v-if="s.city"> · {{ s.city }}</span><span v-if="s.region"> · {{ s.region }}</span><span v-if="s.comuna"> · {{ s.comuna }}</span></p>

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

  <!-- Modal: confirmar aprobar -->
  <Teleport to="body">
    <Transition enter-active-class="transition-opacity duration-200" enter-from-class="opacity-0" enter-to-class="opacity-100"
                leave-active-class="transition-opacity duration-150" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="modalAprobar.show" class="fixed inset-0 z-[9999] flex items-center justify-center">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="modalAprobar.show = false"></div>
        <Transition enter-active-class="transition-all duration-200" enter-from-class="opacity-0 scale-95 translate-y-2" enter-to-class="opacity-100 scale-100 translate-y-0">
          <div v-if="modalAprobar.show" class="relative bg-[#111420] border border-[#2a2d3e] rounded-2xl p-6 max-w-md w-full mx-4">
            <div class="text-center">
              <div class="w-12 h-12 bg-green-500/15 border border-green-500/25 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg class="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
              </div>
              <h3 class="text-base font-semibold text-white mb-2">Aprobar sede</h3>
              <p class="text-gray-400 text-sm mb-6 leading-relaxed">
                ¿Confirmas la aprobación de <span class="text-white font-medium">{{ modalAprobar.sede?.name }}</span>?
                El administrador de la sede será notificado.
              </p>
              <div class="flex justify-center gap-3">
                <button @click="modalAprobar.show = false" class="btn-secondary text-sm px-5">Cancelar</button>
                <button @click="confirmarAprobar" class="text-sm px-5 py-2 rounded-lg bg-green-500/20 text-green-300 hover:bg-green-500/30 border border-green-500/30 transition-colors font-medium">Aprobar sede</button>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>

  <!-- Modal: rechazo con motivo -->
  <Teleport to="body">
    <Transition enter-active-class="transition-opacity duration-200" enter-from-class="opacity-0" enter-to-class="opacity-100"
                leave-active-class="transition-opacity duration-150" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="modalMotivo.show" class="fixed inset-0 z-[9999] flex items-center justify-center">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="modalMotivo.show = false"></div>
        <Transition enter-active-class="transition-all duration-200" enter-from-class="opacity-0 scale-95 translate-y-2" enter-to-class="opacity-100 scale-100 translate-y-0">
          <div v-if="modalMotivo.show" class="relative bg-[#111420] border border-[#2a2d3e] rounded-2xl p-6 max-w-md w-full mx-4">
            <div class="w-12 h-12 rounded-full flex items-center justify-center mx-auto mb-4"
                 :class="modalMotivo.tipo === 'rechazar' ? 'bg-red-500/15 border border-red-500/25' : 'bg-orange-500/15 border border-orange-500/25'">
              <svg class="w-6 h-6" :class="modalMotivo.tipo === 'rechazar' ? 'text-red-400' : 'text-orange-400'" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/>
              </svg>
            </div>
            <h3 class="text-base font-semibold text-white mb-1 text-center">
              {{ modalMotivo.tipo === 'rechazar' ? 'Rechazar sede' : 'Suspender sede' }}
            </h3>
            <p class="text-gray-400 text-sm mb-4 text-center">
              <span class="text-white font-medium">{{ modalMotivo.sede?.name }}</span>
            </p>
            <label class="block text-sm text-gray-300 mb-2">
              {{ modalMotivo.tipo === 'rechazar' ? 'Motivo del rechazo *' : 'Motivo de la suspensión (opcional)' }}
            </label>
            <textarea v-model="modalMotivo.motivo" rows="3" class="input-field text-sm mb-4"
              :placeholder="modalMotivo.tipo === 'rechazar' ? 'Ej: Falta documentación legal, dirección no verificada...' : 'Ej: Problemas con el espacio, revisión de cumplimiento...'"
            ></textarea>
            <p v-if="modalMotivo.error" class="text-red-400 text-xs mb-3">{{ modalMotivo.error }}</p>
            <div class="flex justify-end gap-3">
              <button @click="modalMotivo.show = false" class="btn-secondary text-sm px-5">Cancelar</button>
              <button @click="confirmarMotivo"
                class="text-sm px-5 py-2 rounded-lg font-medium transition-colors border"
                :class="modalMotivo.tipo === 'rechazar'
                  ? 'bg-red-500/20 text-red-300 hover:bg-red-500/30 border-red-500/30'
                  : 'bg-orange-500/20 text-orange-300 hover:bg-orange-500/30 border-orange-500/30'">
                {{ modalMotivo.tipo === 'rechazar' ? 'Rechazar sede' : 'Suspender sede' }}
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>

  <!-- Modal: confirmar reactivar -->
  <Teleport to="body">
    <Transition enter-active-class="transition-opacity duration-200" enter-from-class="opacity-0" enter-to-class="opacity-100"
                leave-active-class="transition-opacity duration-150" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="modalReactivar.show" class="fixed inset-0 z-[9999] flex items-center justify-center">
        <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="modalReactivar.show = false"></div>
        <Transition enter-active-class="transition-all duration-200" enter-from-class="opacity-0 scale-95 translate-y-2" enter-to-class="opacity-100 scale-100 translate-y-0">
          <div v-if="modalReactivar.show" class="relative bg-[#111420] border border-[#2a2d3e] rounded-2xl p-6 max-w-md w-full mx-4">
            <div class="text-center">
              <div class="w-12 h-12 bg-green-500/15 border border-green-500/25 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg class="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                </svg>
              </div>
              <h3 class="text-base font-semibold text-white mb-2">Reactivar sede</h3>
              <p class="text-gray-400 text-sm mb-6 leading-relaxed">
                ¿Confirmas la reactivación de <span class="text-white font-medium">{{ modalReactivar.sede?.name }}</span>?
                Volverá a aparecer disponible para reservas.
              </p>
              <div class="flex justify-center gap-3">
                <button @click="modalReactivar.show = false" class="btn-secondary text-sm px-5">Cancelar</button>
                <button @click="confirmarReactivar" class="text-sm px-5 py-2 rounded-lg bg-green-500/20 text-green-300 hover:bg-green-500/30 border border-green-500/30 transition-colors font-medium">Reactivar</button>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import adminService from '@/services/adminService'
import { useToast } from '@/composables/useToast'
import { formatDate } from '@/utils/dateFormatter'

const toast = useToast()

const sedes = ref([])
const loading = ref(true)
const filtroActivo = ref('TODOS')

const modalAprobar = reactive({ show: false, sede: null })
const modalReactivar = reactive({ show: false, sede: null })
const modalMotivo = reactive({ show: false, sede: null, tipo: 'rechazar', motivo: '', error: '' })

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

function aprobar(sede) {
  modalAprobar.sede = sede
  modalAprobar.show = true
}

async function confirmarAprobar() {
  modalAprobar.show = false
  try {
    await adminService.approveVenue(modalAprobar.sede.id)
    toast.success?.('Sede aprobada correctamente')
    await cargar()
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al aprobar la sede')
  }
}

function rechazar(sede) {
  modalMotivo.sede = sede
  modalMotivo.tipo = 'rechazar'
  modalMotivo.motivo = ''
  modalMotivo.error = ''
  modalMotivo.show = true
}

function suspender(sede) {
  modalMotivo.sede = sede
  modalMotivo.tipo = 'suspender'
  modalMotivo.motivo = ''
  modalMotivo.error = ''
  modalMotivo.show = true
}

async function confirmarMotivo() {
  if (modalMotivo.tipo === 'rechazar' && !modalMotivo.motivo.trim()) {
    modalMotivo.error = 'El motivo es obligatorio para rechazar una sede.'
    return
  }
  modalMotivo.show = false
  try {
    if (modalMotivo.tipo === 'rechazar') {
      await adminService.rejectVenue(modalMotivo.sede.id, modalMotivo.motivo.trim() || 'No especificado')
      toast.success?.('Sede rechazada')
    } else {
      await adminService.toggleVenue(modalMotivo.sede.id, modalMotivo.motivo.trim())
      toast.success?.('Sede suspendida')
    }
    await cargar()
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al procesar la acción')
  }
}

function reactivar(sede) {
  modalReactivar.sede = sede
  modalReactivar.show = true
}

async function confirmarReactivar() {
  modalReactivar.show = false
  try {
    await adminService.toggleVenue(modalReactivar.sede.id, '')
    toast.success?.('Sede reactivada correctamente')
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
</script>
