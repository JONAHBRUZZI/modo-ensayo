<template>
  <div class="min-h-screen flex flex-col bg-[#0f1119]">
    <!-- Navbar -->
    <nav class="sticky top-0 z-50 bg-[#0f1119]/95 backdrop-blur-sm border-b border-gray-800">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <!-- Logo -->
          <router-link to="/" class="flex items-center space-x-2">
            <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
              <span class="text-white font-bold text-sm">ME</span>
            </div>
            <span class="text-white font-semibold text-lg">Modo Ensayo</span>
          </router-link>

          <!-- Desktop Nav -->
          <div class="hidden md:flex items-center space-x-1">
            <router-link to="/" class="nav-link">Inicio</router-link>
            <router-link v-if="!isAuthenticated || modoActual === 'alumno'" to="/classes" class="nav-link">Cronograma</router-link>

            <template v-if="isAuthenticated">
              <!-- Profesor mode -->
              <template v-if="isTeacher && modoActual === 'profesor'">
                <router-link to="/profesor/dashboard" class="nav-link">Dashboard</router-link>
                <router-link to="/profesor/clases-propias" class="nav-link">Clases Propias</router-link>
                <router-link to="/profesor/clases-asignadas" class="nav-link">Clases Asignadas</router-link>
                <router-link to="/profesor/buscar-salas" class="nav-link">Buscar Salas</router-link>
                <router-link to="/profesor/metricas" class="nav-link">Metricas</router-link>
              </template>

              <!-- Sede mode -->
              <template v-if="isSede && modoActual === 'sede'">
                <router-link to="/sede/dashboard" class="nav-link">Panel</router-link>
                <router-link to="/sede/salas" class="nav-link">Salas</router-link>
                <router-link to="/sede/mis-clases" class="nav-link">Clases</router-link>
                <router-link to="/sede/clases-por-confirmar" class="nav-link">Confirmar</router-link>
                <router-link to="/sede/configuracion" class="nav-link">Config</router-link>
              </template>

              <!-- Admin mode -->
              <template v-if="isAdmin && modoActual === 'admin'">
                <router-link to="/admin" class="nav-link">Dashboard</router-link>
                <router-link to="/admin/roles" class="nav-link">Aprobaciones</router-link>
                <router-link to="/admin/usuarios" class="nav-link">Usuarios</router-link>
              </template>

              <!-- Alumno mode -->
              <template v-if="modoActual === 'alumno'">
                <router-link to="/alumno/dashboard" class="nav-link">Mi Espacio</router-link>
                <router-link to="/classes" class="nav-link">Clases</router-link>
                <router-link to="/alumno/pagos" class="nav-link">Pagos</router-link>
                <router-link to="/cart" class="nav-link relative">
                  Carrito
                  <span v-if="cartCount > 0" class="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">{{ cartCount }}</span>
                </router-link>
              </template>

              <!-- Admin quick link always visible if admin -->
              <router-link v-if="isAdmin && modoActual !== 'admin'" to="/admin" class="nav-link text-primary text-xs">Admin</router-link>
            </template>
          </div>

          <!-- Right side -->
          <div class="flex items-center space-x-3">
            <template v-if="isAuthenticated">
              <!-- Context Switcher -->
              <div v-if="puedeAlternarModo" class="hidden sm:flex items-center space-x-1 bg-[#1a1d2e] rounded-lg p-1">
                <button
                  v-for="mode in availableModes"
                  :key="mode.value"
                  @click="setModo(mode.value)"
                  :class="[
                    'px-3 py-1 rounded-md text-sm transition-all',
                    modoActual === mode.value ? 'bg-primary text-white' : 'text-gray-400 hover:text-white'
                  ]"
                >
                  {{ mode.label }}
                </button>
              </div>

              <!-- Notification bell -->
              <router-link to="/notificaciones" class="relative p-2 rounded-lg hover:bg-[#1a1d2e] transition-colors">
                <svg class="w-5 h-5 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>
                <span v-if="notifCount > 0" class="absolute -top-1 -right-1 bg-primary text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">{{ notifCount }}</span>
              </router-link>

              <!-- User dropdown -->
              <div class="relative" ref="userMenuRef">
                <button @click="showUserMenu = !showUserMenu" class="flex items-center space-x-2 p-2 rounded-lg hover:bg-[#1a1d2e] transition-colors">
                  <div class="w-8 h-8 bg-primary rounded-full flex items-center justify-center text-sm font-medium">
                    {{ displayName.charAt(0).toUpperCase() }}
                  </div>
                  <div class="hidden lg:block text-left">
                    <div class="text-sm font-medium text-white">{{ displayName }}</div>
                    <div class="text-xs text-gray-400">{{ user?.email }}</div>
                  </div>
                </button>

                <div v-if="showUserMenu" class="absolute right-0 mt-2 w-56 bg-[#161824] border border-gray-700 rounded-xl shadow-xl py-2">
                  <div class="px-4 py-2 border-b border-gray-700">
                    <div class="text-sm font-medium text-white">{{ displayName }}</div>
                    <div class="text-xs text-gray-400">{{ user?.email }}</div>
                    <div class="mt-1">
                      <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-primary/20 text-primary">
                        {{ modeLabel }}
                      </span>
                    </div>
                  </div>
                  <router-link to="/profile" class="flex items-center px-4 py-2 text-sm text-gray-300 hover:bg-[#1a1d2e]">
                    <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
                    Perfil
                  </router-link>
                  <router-link to="/notificaciones" class="flex items-center px-4 py-2 text-sm text-gray-300 hover:bg-[#1a1d2e]">
                    <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>
                    Notificaciones
                  </router-link>
                  <button @click="handleLogout" class="flex items-center w-full px-4 py-2 text-sm text-red-400 hover:bg-[#1a1d2e]">
                    <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/></svg>
                    Cerrar sesion
                  </button>
                </div>
              </div>
            </template>

            <template v-else>
              <router-link to="/login" class="text-gray-300 hover:text-white transition-colors text-sm">Iniciar sesion</router-link>
              <router-link to="/register" class="btn-primary text-sm !py-2 !px-4">Registrarse</router-link>
            </template>
          </div>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="flex-1">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="bg-[#161824] border-t border-gray-800 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex flex-col md:flex-row justify-between items-center">
          <div class="flex items-center space-x-2 mb-4 md:mb-0">
            <div class="w-6 h-6 bg-primary rounded flex items-center justify-center">
              <span class="text-white text-xs font-bold">ME</span>
            </div>
            <span class="text-gray-400 text-sm">Modo Ensayo &copy; {{ new Date().getFullYear() }}</span>
          </div>
          <div class="flex space-x-6">
            <router-link to="/" class="text-gray-500 hover:text-gray-300 text-sm">Inicio</router-link>
            <router-link to="/classes" class="text-gray-500 hover:text-gray-300 text-sm">Clases</router-link>
            <router-link to="/quiero-ser-profesor" class="text-gray-500 hover:text-gray-300 text-sm">Ser Profesor</router-link>
            <router-link to="/quiero-gestionar-sede" class="text-gray-500 hover:text-gray-300 text-sm">Gestionar Sede</router-link>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import paymentService from '@/services/paymentService'
import rescheduleService from '@/services/rescheduleService'

const router = useRouter()
const { user, isAuthenticated, isAdmin, isSede, isTeacher, identidadValidada, puedeAlternarModo, modoActual, displayName, setModo, logout } = useAuth()

const showUserMenu = ref(false)
const userMenuRef = ref(null)
const cartCount = ref(0)
const notifCount = ref(0)

const availableModes = computed(() => {
  const modes = [{ value: 'alumno', label: 'Alumno' }]
  if (isTeacher.value && identidadValidada.value) modes.push({ value: 'profesor', label: 'Maestro' })
  if (isSede.value && identidadValidada.value) modes.push({ value: 'sede', label: 'Mi Sede' })
  if (isAdmin.value) modes.push({ value: 'admin', label: 'Admin' })
  return modes
})

const modeLabel = computed(() => {
  const mode = availableModes.value.find((m) => m.value === modoActual.value)
  return mode?.label || 'Alumno'
})

function handleLogout() {
  logout()
}

function handleClickOutside(e) {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target)) {
    showUserMenu.value = false
  }
}

async function loadCartCount() {
  try {
    const data = await paymentService.getCart()
    cartCount.value = data?.items?.length || 0
  } catch {
    cartCount.value = 0
  }
}

async function loadNotifCount() {
  try {
    const data = await rescheduleService.getUnreadCount()
    notifCount.value = data?.data?.count || data?.count || data || 0
  } catch {
    notifCount.value = 0
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  if (isAuthenticated.value) {
    loadCartCount()
    loadNotifCount()
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.nav-link {
  @apply px-3 py-2 rounded-lg text-sm text-gray-300 hover:text-white hover:bg-[#1a1d2e] transition-all;
}

.nav-link.router-link-active {
  @apply text-primary bg-primary/10;
}
</style>
