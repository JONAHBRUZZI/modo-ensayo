<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Gestion de Usuarios</h1>

    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>

    <div v-else class="space-y-4">
      <div v-for="u in users" :key="u.id" class="card flex items-center justify-between flex-wrap gap-3">
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
                  class="bg-[#1a1d2e] border border-gray-700 rounded-lg text-sm text-gray-300 px-2 py-1">
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

    <!-- Modal de confirmacion de eliminacion -->
    <div v-if="usuarioAEliminar"
         class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 px-4"
         @click.self="cerrarModal">
      <div class="bg-[#161824] border border-red-500/30 rounded-xl p-6 max-w-md w-full space-y-4 shadow-2xl">
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
            verificacion de identidad, asociados, perfil profesional y notificaciones.
          </p>
        </div>

        <p v-if="errorEliminar" class="text-red-400 text-sm">{{ errorEliminar }}</p>

        <div class="flex gap-3">
          <button @click="confirmarEliminacion" :disabled="eliminando"
                  class="flex-1 px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed">
            {{ eliminando ? 'Eliminando...' : 'Si, eliminar' }}
          </button>
          <button @click="cerrarModal" :disabled="eliminando"
                  class="flex-1 px-4 py-2 rounded-lg bg-[#1a1d2e] border border-white/10 text-gray-300 hover:text-white hover:border-white/20 transition-colors disabled:opacity-50">
            Cancelar
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminService from '@/services/adminService'
import EstadoBadge from '@/components/EstadoBadge.vue'

const users = ref([])
const loading = ref(true)

// Estado del modal de eliminacion
const usuarioAEliminar = ref(null)
const eliminando = ref(false)
const errorEliminar = ref('')

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
    alert(e?.response?.data?.message || 'Error al asignar rol')
  }
}

async function toggleUser(u) {
  const motivo = u.enabled ? prompt('Motivo de suspension:') : null
  if (u.enabled && motivo === null) return
  try {
    await adminService.toggleUser(u.id, motivo || '')
    u.enabled = !u.enabled
  } catch (e) {
    alert(e?.response?.data?.message || 'Error al cambiar estado del usuario')
  }
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
