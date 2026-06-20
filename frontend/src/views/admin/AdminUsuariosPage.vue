<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-8 flex-wrap gap-4">
      <h1 class="text-3xl font-bold text-white">Gestión de Usuarios</h1>
      <div class="relative w-full sm:w-80">
        <svg class="w-4 h-4 text-gray-500 absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
        </svg>
        <input v-model="busqueda" type="text" placeholder="Buscar por email o nombre..."
               class="w-full bg-[var(--bg-elevated)] border border-white/10 rounded-lg text-sm text-gray-200 pl-9 pr-3 py-2 focus:outline-none focus:border-primary/50" />
      </div>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <div v-else-if="usuariosFiltrados.length === 0" class="card text-center py-12">
      <p class="text-gray-400">No se encontraron usuarios para "{{ busqueda }}".</p>
    </div>

    <div v-else class="space-y-4">
      <div v-for="u in usuariosFiltrados" :key="u.id" class="card flex items-center justify-between flex-wrap gap-3">
        <div class="flex items-center space-x-4 min-w-0">
          <div class="w-10 h-10 bg-primary/20 rounded-full flex items-center justify-center text-primary font-bold flex-shrink-0">
            {{ (u.fullName || u.email || 'U').charAt(0).toUpperCase() }}
          </div>
          <div class="min-w-0">
            <h3 class="text-white font-medium truncate">{{ u.fullName || u.email }}</h3>
            <p class="text-gray-400 text-sm truncate">{{ u.email }}</p>
          </div>
        </div>

        <div class="flex items-center gap-2 flex-wrap">
          <div class="flex flex-wrap gap-1">
            <span v-for="r in u.roles" :key="r" class="badge badge-blue">{{ r }}</span>
          </div>

          <select @change="assignRole(u.id, $event.target.value); $event.target.value = ''"
                  class="bg-[var(--bg-elevated)] border border-gray-700 rounded-lg text-sm text-gray-300 px-2 py-1">
            <option value="">+ Rol</option>
            <option value="TEACHER">TEACHER</option>
            <option value="VENUE_ADMIN">VENUE_ADMIN</option>
            <option value="ADMIN">ADMIN</option>
          </select>

          <EstadoBadge :status="u.enabled ? 'ENABLED' : 'DISABLED'" />

          <button @click="toggleUser(u)"
                  :class="['text-xs px-3 py-1.5 rounded-lg transition-colors',
                           u.enabled ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30'
                                     : 'bg-green-500/20 text-green-400 hover:bg-green-500/30']">
            {{ u.enabled ? 'Suspender' : 'Activar' }}
          </button>

          <!-- Boton Eliminar: abre modal de confirmacion -->
          <button @click="abrirModalEliminar(u)"
                  class="text-xs px-3 py-1.5 rounded-lg bg-red-600/20 text-red-300 hover:bg-red-600/40 border border-red-500/30 transition-colors">
            Eliminar
          </button>
        </div>
      </div>
    </div>

    <!-- Modal de confirmacion de suspension con motivo -->
    <div v-if="suspendTarget"
         class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 px-4"
         @click.self="cerrarSuspender">
      <div class="bg-[var(--bg-overlay)] border border-yellow-500/30 rounded-xl p-6 max-w-md w-full space-y-4 shadow-2xl">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 bg-yellow-500/20 rounded-full flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M12 4l8 14H4l8-14z"/>
            </svg>
          </div>
          <div class="flex-1">
            <h3 class="text-white font-bold text-lg">Suspender usuario</h3>
            <p class="text-gray-300 text-sm mt-1">
              Suspenderas la cuenta de <span class="text-white font-medium">{{ suspendTarget.email }}</span>.
              Perdera acceso a la plataforma hasta que sea reactivado.
            </p>
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">
            Motivo de suspension <span class="text-red-400">*</span>
          </label>
          <textarea v-model="suspendMotivo" rows="3" class="input-field"
            placeholder="Describe el motivo de la suspension..."
            :class="suspendMotivoError ? 'border-red-500/50' : ''" />
          <p v-if="suspendMotivoError" class="text-red-400 text-xs mt-1">{{ suspendMotivoError }}</p>
        </div>

        <div class="flex gap-3">
          <button @click="confirmarSuspension" :disabled="suspendiendo"
                  class="flex-1 px-4 py-2 rounded-lg bg-yellow-600 hover:bg-yellow-700 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed">
            {{ suspendiendo ? 'Suspendiendo...' : 'Suspender' }}
          </button>
          <button @click="cerrarSuspender" :disabled="suspendiendo"
                  class="flex-1 px-4 py-2 rounded-lg bg-[var(--bg-elevated)] border border-white/10 text-gray-300 hover:text-white hover:border-white/20 transition-colors disabled:opacity-50">
            Cancelar
          </button>
        </div>
      </div>
    </div>
    <div v-if="usuarioAEliminar"
         class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 px-4"
         @click.self="cerrarModal">
      <div class="bg-[var(--bg-overlay)] border border-red-500/30 rounded-xl p-6 max-w-md w-full space-y-4 shadow-2xl">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 bg-red-500/20 rounded-full flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M1 7h22M9 7V4a2 2 0 012-2h2a2 2 0 012 2v3"/>
            </svg>
          </div>
          <div class="flex-1">
            <h3 class="text-white font-bold text-lg">Eliminar cuenta de usuario</h3>
            <p class="text-gray-300 text-sm mt-1">
              Estas seguro de eliminar la cuenta del usuario
              <span class="text-white font-medium">{{ usuarioAEliminar.email }}</span>?
            </p>
          </div>
        </div>

        <div class="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-3">
          <p class="text-yellow-300 text-xs">
            Esta accion es <strong>irreversible</strong>. Se eliminaran tambien sus roles asignados,
            verificación de identidad, asociados, perfil profesional y notificaciónes.
          </p>
        </div>

        <p v-if="errorEliminar" class="text-red-400 text-sm">{{ errorEliminar }}</p>

        <div class="flex gap-3">
          <button @click="confirmarEliminacion" :disabled="eliminando"
                  class="flex-1 px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed">
            {{ eliminando ? 'Eliminando...' : 'Si, eliminar' }}
          </button>
          <button @click="cerrarModal" :disabled="eliminando"
                  class="flex-1 px-4 py-2 rounded-lg bg-[var(--bg-elevated)] border border-white/10 text-gray-300 hover:text-white hover:border-white/20 transition-colors disabled:opacity-50">
            Cancelar
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import adminService from '@/services/adminService'
import EstadoBadge from '@/components/EstadoBadge.vue'
import { useToast } from '@/composables/useToast'

const toast = useToast()
const users = ref([])
const loading = ref(true)

// Busqueda por email o nombre (HU22)
const busqueda = ref('')
const usuariosFiltrados = computed(() => {
  const q = busqueda.value.trim().toLowerCase()
  if (!q) return users.value
  return users.value.filter(u =>
    (u.email || '').toLowerCase().includes(q) ||
    (u.fullName || '').toLowerCase().includes(q)
  )
})

// Estado del modal de eliminacion
const usuarioAEliminar = ref(null)
const eliminando = ref(false)
const errorEliminar = ref('')

// Estado de suspension con motivo
const suspendTarget = ref(null)
const suspendMotivo = ref('')
const suspendMotivoError = ref('')
const suspendiendo = ref(false)

onMounted(async () => {
  await cargar()
})

async function cargar() {
  loading.value = true
  try {
    const data = await adminService.getUsers()
    users.value = Array.isArray(data) ? data : data.content || []
  } catch {
    users.value = []
  }
  loading.value = false
}

async function assignRole(userId, role) {
  if (!role) return
  try {
    await adminService.assignRole(userId, role)
    await cargar()
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al asignar rol')
  }
}

function abrirSuspender(u) {
  suspendTarget.value = u
  suspendMotivo.value = ''
  suspendMotivoError.value = ''
}

function cerrarSuspender() {
  if (suspendiendo.value) return
  suspendTarget.value = null
  suspendMotivo.value = ''
  suspendMotivoError.value = ''
}

async function toggleUser(u) {
  if (u.enabled) {
    abrirSuspender(u)
    return
  }
  // Reactivar no requiere motivo
  try {
    await adminService.toggleUser(u.id, '')
    u.enabled = !u.enabled
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al cambiar estado del usuario')
  }
}

async function confirmarSuspension() {
  if (!suspendMotivo.value.trim()) {
    suspendMotivoError.value = 'El motivo de suspension es obligatorio'
    return
  }
  suspendiendo.value = true
  try {
    await adminService.toggleUser(suspendTarget.value.id, suspendMotivo.value)
    suspendTarget.value.enabled = !suspendTarget.value.enabled
    cerrarSuspender()
  } catch (e) {
    toast.error(e?.response?.data?.message || 'Error al suspender el usuario')
  }
  suspendiendo.value = false
}

function abrirModalEliminar(u) {
  usuarioAEliminar.value = u
  errorEliminar.value = ''
}

function cerrarModal() {
  if (eliminando.value) return
  usuarioAEliminar.value = null
  errorEliminar.value = ''
}

async function confirmarEliminacion() {
  if (!usuarioAEliminar.value) return
  eliminando.value = true
  errorEliminar.value = ''
  try {
    await adminService.deleteUser(usuarioAEliminar.value.id)
    users.value = users.value.filter(u => u.id !== usuarioAEliminar.value.id)
    usuarioAEliminar.value = null
  } catch (e) {
    errorEliminar.value = e?.response?.data?.message || 'Error al eliminar el usuario'
  } finally {
    eliminando.value = false
  }
}
</script>
