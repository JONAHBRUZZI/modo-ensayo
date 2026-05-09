<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Gestión de Usuarios</h1>
        <p class="text-gray-400 text-sm mt-0.5">Administra todos los usuarios del sistema</p>
      </div>
    </div>

    <!-- Filtros -->
    <div class="flex gap-3 flex-wrap">
      <input v-model="busqueda" placeholder="Buscar por nombre o email..."
        class="px-3 py-2 bg-[#161824] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all flex-1 min-w-[200px]" />
      <select v-model="filtroRol"
        class="px-3 py-2 bg-[#161824] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
        <option value="">Todos los roles</option>
        <option>USER</option><option>TEACHER</option><option>VENUE_ADMIN</option><option>ADMIN</option>
      </select>
    </div>

    <!-- Tabla -->
    <div class="bg-[#161824] rounded-xl border border-white/10 overflow-hidden">
      <div v-if="loading" class="p-8 space-y-3">
        <div v-for="i in 5" :key="i" class="h-12 bg-white/5 rounded-lg animate-pulse"></div>
      </div>

      <table v-else class="w-full">
        <thead>
          <tr class="border-b border-white/5">
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Usuario</th>
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider hidden sm:table-cell">Rol</th>
            <th class="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider hidden md:table-cell">Estado</th>
            <th class="px-5 py-3"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-white/5">
          <tr v-for="u in filteredUsers" :key="u.id" class="hover:bg-white/3 transition-colors">
            <td class="px-5 py-3.5">
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 bg-gradient-to-br from-indigo-600 to-purple-600 rounded-full flex items-center justify-center flex-shrink-0">
                  <span class="text-xs font-bold text-white">{{ u.fullName?.charAt(0) }}</span>
                </div>
                <div class="min-w-0">
                  <p class="text-sm font-medium text-white truncate">{{ u.fullName }}</p>
                  <p class="text-xs text-gray-500 truncate">{{ u.email }}</p>
                </div>
              </div>
            </td>
            <td class="px-5 py-3.5 hidden sm:table-cell">
              <span class="px-2 py-0.5 bg-indigo-500/15 text-indigo-400 border border-indigo-500/20 rounded-full text-xs">
                {{ (u.roles || ['USER'])[0] }}
              </span>
            </td>
            <td class="px-5 py-3.5 hidden md:table-cell">
              <EstadoBadge :estado="u.active !== false ? 'ACTIVA' : 'CANCELADA'" />
            </td>
            <td class="px-5 py-3.5 text-right">
              <button class="text-xs text-gray-500 hover:text-white transition-colors">···</button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="!loading && !filteredUsers.length" class="text-center py-10">
        <p class="text-gray-500 text-sm">No se encontraron usuarios</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminService } from '../../services/adminService'
import EstadoBadge from '../../components/EstadoBadge.vue'

const loading = ref(true)
const users = ref([])
const busqueda = ref('')
const filtroRol = ref('')

const filteredUsers = computed(() => users.value.filter(u => {
  const matchQ = !busqueda.value || u.fullName?.toLowerCase().includes(busqueda.value.toLowerCase()) || u.email?.toLowerCase().includes(busqueda.value.toLowerCase())
  const matchRol = !filtroRol.value || (u.roles || []).includes(filtroRol.value)
  return matchQ && matchRol
}))

onMounted(async () => {
  try { users.value = await adminService.getUsers?.() || [] } catch { users.value = [] }
  finally { loading.value = false }
})
</script>
