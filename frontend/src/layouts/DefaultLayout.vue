<template>
  <div class="min-h-screen flex flex-col bg-[var(--bg-base)] relative overflow-x-hidden">
    <!-- Aurora de fondo global -->
    <div class="aurora" aria-hidden="true">
      <div class="aurora-blob aurora-purple"></div>
      <div class="aurora-blob aurora-blue"></div>
      <div class="aurora-blob aurora-pink"></div>
    </div>
    <!-- Navbar -->
    <nav class="sticky top-0 z-50 bg-[var(--bg-base)] backdrop-blur-sm border-b border-[#6C63FF22]">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <!-- Logo -->
          <router-link to="/" class="flex items-center space-x-2">
            <div class="w-8 h-8 rounded-lg flex items-center justify-center" style="background: linear-gradient(135deg, #6C63FF, #9B8CFF);">
              <span class="text-white font-bold text-xs tracking-wide">ME</span>
            </div>
            <span class="font-semibold text-lg">Modo <span class="text-primary">Ensayo</span></span>
          </router-link>

          <!-- Desktop Nav -->
          <div class="hidden md:flex items-center space-x-1">
            <router-link v-if="!isAuthenticated" to="/" class="nav-link">Inicio</router-link>
            <router-link v-if="!isAuthenticated || modoActual === 'alumno'" to="/classes" class="nav-link">Cronograma</router-link>

            <template v-if="isAuthenticated">
              <!-- Profesor mode -->
              <template v-if="puedeVerContextoProfesor && modoActual === 'profesor'">
                <router-link to="/profesor/dashboard" class="nav-link">Home</router-link>
                <router-link to="/profesor/clases-por-asignar" class="nav-link relative">
                  Por Asignar
                  <span v-if="reservasSinClaseCount > 0" class="absolute -top-1 -right-1 bg-yellow-500 text-black text-xs font-bold rounded-full w-4 h-4 flex items-center justify-center leading-none">{{ reservasSinClaseCount }}</span>
                </router-link>
                <router-link to="/profesor/clases-propias" class="nav-link">Mis Clases</router-link>
                <router-link to="/profesor/clases-asignadas" class="nav-link">Asignadas</router-link>
                <router-link to="/profesor/borradores" class="nav-link">Borradores</router-link>
                <router-link to="/profesor/buscar-salas" class="nav-link">Agendar Sala</router-link>
                <router-link to="/profesor/metricas" class="nav-link">Métricas</router-link>
                <router-link to="/profesor/pagos" class="nav-link">Pagos</router-link>
              </template>

              <!-- Sede mode -->
              <template v-if="puedeVerContextoSede && modoActual === 'sede'">
                <router-link to="/sede/dashboard" class="nav-link">Panel</router-link>
                <router-link to="/sede/salas" class="nav-link">Salas</router-link>
                <router-link to="/sede/calendario" class="nav-link">Calendario</router-link>
                <router-link to="/sede/mis-clases" class="nav-link">Clases</router-link>
                <router-link to="/sede/clases-por-confirmar" class="nav-link relative">
                  Confirmar
                  <span v-if="sedeConfirmarCount > 0" class="absolute -top-1 -right-1 bg-yellow-500 text-black text-xs font-bold rounded-full w-4 h-4 flex items-center justify-center leading-none">{{ sedeConfirmarCount }}</span>
                </router-link>
                <router-link to="/sede/profesores" class="nav-link">Profesores</router-link>
                <router-link to="/sede/metricas" class="nav-link">Métricas</router-link>
                <router-link to="/sede/configuracion" class="nav-link">Config</router-link>
                <router-link to="/reviews" class="nav-link">Reseñas</router-link>
              </template>

              <!-- Admin mode -->
              <template v-if="isAdmin && modoActual === 'admin'">
                <router-link to="/admin" class="nav-link">Home</router-link>
                <router-link to="/admin/roles" class="nav-link">Aprobaciones</router-link>
                <router-link to="/admin/usuarios" class="nav-link">Usuarios</router-link>
                <router-link to="/admin/sedes" class="nav-link">Sedes</router-link>
              </template>

              <!-- Alumno mode -->
              <template v-if="modoActual === 'alumno'">
                <router-link to="/alumno/dashboard" class="nav-link">Mi Espacio</router-link>
                <router-link to="/reviews" class="nav-link">Reseñas</router-link>
                <router-link to="/cart" class="nav-link relative">
                  Carrito
                  <span v-if="cartCount > 0" class="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">{{ cartCount }}</span>
                </router-link>
              </template>
            </template>
          </div>

          <!-- Right side -->
          <div class="flex items-center space-x-3">
            <template v-if="isAuthenticated">
              <!-- Context Switcher -->
              <div v-if="puedeAlternarModo" class="hidden sm:flex items-center space-x-1 bg-[var(--bg-elevated)] rounded-lg p-1">
                <button
                  v-for="mode in availableModes"
                  :key="mode.value"
                  @click="cambiarModo(mode.value)"
                  :class="[
                    'px-3 py-1 rounded-md text-sm transition-all',
                        modoActual === mode.value ? 'bg-primary text-white' : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
                  ]"
                >
                  {{ mode.label }}
                </button>
              </div>

              <!-- Notification bell -->
              <NotificationBell />

              <!-- User dropdown -->
              <div class="relative" ref="userMenuRef">
                <button @click="showUserMenu = !showUserMenu" class="flex items-center space-x-2 p-2 rounded-lg hover:bg-[var(--bg-elevated)] transition-colors">
                  <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold text-white" style="background: linear-gradient(135deg, #6C63FF, #9B8CFF);">
                    {{ displayName.charAt(0).toUpperCase() }}
                  </div>
                  <div class="hidden lg:block text-left">
                    <div class="text-sm font-medium text-[var(--text-primary)]">{{ displayName }}</div>
                    <div class="text-xs text-[var(--text-secondary)]">{{ user?.email }}</div>
                  </div>
                </button>

                <div v-if="showUserMenu" class="absolute right-0 mt-2 w-56 bg-[var(--bg-overlay)] border border-gray-700 rounded-xl shadow-xl py-2">
                  <div class="px-4 py-2 border-b border-gray-700">
                    <div class="text-sm font-medium text-[var(--text-primary)]">{{ displayName }}</div>
                    <div class="text-xs text-[var(--text-secondary)]">{{ user?.email }}</div>
                    <div class="mt-1">
                      <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-primary/20 text-primary">
                        {{ modeLabel }}
                      </span>
                    </div>
                  </div>
                  <router-link to="/profile" class="flex items-center px-4 py-2 text-sm text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)]">
                    <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
                    Perfil
                  </router-link>
                  <router-link to="/notificaciones" class="flex items-center px-4 py-2 text-sm text-gray-300 hover:bg-[#1a1d2e]">
                    <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>
                    Notificaciónes
                  </router-link>
                  <button @click="toggleTheme" class="flex items-center w-full px-4 py-2 text-sm text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)]">
                    <svg v-if="isDark" class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"/></svg>
                    <svg v-else class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"/></svg>
                    {{ isDark ? 'Modo claro' : 'Modo oscuro' }}
                  </button>
                  <button @click="handleLogout" class="flex items-center w-full px-4 py-2 text-sm text-red-400 hover:bg-[var(--bg-elevated)]">
                    <svg class="w-4 h-4 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/></svg>
                    Cerrar sesión
                  </button>
                </div>
              </div>
            </template>

            <template v-else>
              <button @click="toggleTheme" class="p-2 rounded-lg text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-elevated)] transition-colors" :aria-label="isDark ? 'Modo claro' : 'Modo oscuro'">
                <svg v-if="isDark" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"/></svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"/></svg>
              </button>
              <router-link to="/login" class="text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors text-sm">Iniciar sesion</router-link>
              <router-link to="/register" class="btn-primary text-sm !py-2 !px-4">Registrarse</router-link>
            </template>

          <!-- Hamburger (mobile) -->
          <button
            @click="showMobileMenu = !showMobileMenu"
            class="md:hidden p-2 rounded-lg text-gray-400 hover:text-white hover:bg-[var(--bg-elevated)] transition-colors"
            aria-label="Abrir menú"
          >
            <svg v-if="!showMobileMenu" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
            </svg>
            <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- Mobile drawer -->
    <Transition
      enter-active-class="transition-all duration-250"
      enter-from-class="opacity-0 -translate-y-2"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition-all duration-200"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-2"
    >
      <div v-if="showMobileMenu" class="md:hidden bg-[var(--bg-base)] border-b border-[var(--border-subtle)] px-4 pb-4 pt-2">
        <div class="flex flex-col gap-1">
          <router-link v-if="!isAuthenticated" to="/" class="nav-link-mobile" @click="showMobileMenu = false">Inicio</router-link>
          <router-link v-if="!isAuthenticated || modoActual === 'alumno'" to="/classes" class="nav-link-mobile" @click="showMobileMenu = false">Cronograma</router-link>

          <template v-if="isAuthenticated">
            <!-- Cambiar de contexto (movil) -->
            <div v-if="puedeAlternarModo" class="flex items-center gap-1 bg-[var(--bg-elevated)] rounded-lg p-1 mb-2">
              <button v-for="mode in availableModes" :key="mode.value"
                      @click="cambiarModo(mode.value); showMobileMenu = false"
                      :class="[
                        'flex-1 px-2 py-1.5 rounded-md text-sm transition-all',
                    modoActual === mode.value ? 'bg-primary text-white' : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
                      ]">
                {{ mode.label }}
              </button>
            </div>
            <template v-if="puedeVerContextoProfesor && modoActual === 'profesor'">
              <router-link to="/profesor/dashboard" class="nav-link-mobile" @click="showMobileMenu = false">Dashboard</router-link>
              <router-link to="/profesor/clases-propias" class="nav-link-mobile" @click="showMobileMenu = false">Mis Clases</router-link>
              <router-link to="/profesor/clases-por-asignar" class="nav-link-mobile" @click="showMobileMenu = false">Por Asignar</router-link>
              <router-link to="/profesor/buscar-salas" class="nav-link-mobile" @click="showMobileMenu = false">Agendar Sala</router-link>
              <router-link to="/profesor/metricas" class="nav-link-mobile" @click="showMobileMenu = false">Métricas</router-link>
            </template>
            <template v-if="puedeVerContextoSede && modoActual === 'sede'">
              <router-link to="/sede/dashboard" class="nav-link-mobile" @click="showMobileMenu = false">Panel</router-link>
              <router-link to="/sede/salas" class="nav-link-mobile" @click="showMobileMenu = false">Salas</router-link>
              <router-link to="/sede/calendario" class="nav-link-mobile" @click="showMobileMenu = false">Calendario</router-link>
              <router-link to="/sede/mis-clases" class="nav-link-mobile" @click="showMobileMenu = false">Clases</router-link>
              <router-link to="/sede/clases-por-confirmar" class="nav-link-mobile relative" @click="showMobileMenu = false">
                Confirmar
                <span v-if="sedeConfirmarCount > 0" class="ml-2 bg-yellow-500 text-black text-xs font-bold rounded-full px-1.5 py-0.5 leading-none">{{ sedeConfirmarCount }}</span>
              </router-link>
              <router-link to="/sede/profesores" class="nav-link-mobile" @click="showMobileMenu = false">Profesores</router-link>
              <router-link to="/sede/metricas" class="nav-link-mobile" @click="showMobileMenu = false">Métricas</router-link>
              <router-link to="/sede/configuracion" class="nav-link-mobile" @click="showMobileMenu = false">Config</router-link>
              <router-link to="/reviews" class="nav-link-mobile" @click="showMobileMenu = false">Reseñas</router-link>
            </template>
            <template v-if="isAdmin && modoActual === 'admin'">
              <router-link to="/admin" class="nav-link-mobile" @click="showMobileMenu = false">Home</router-link>
              <router-link to="/admin/roles" class="nav-link-mobile" @click="showMobileMenu = false">Aprobaciones</router-link>
              <router-link to="/admin/usuarios" class="nav-link-mobile" @click="showMobileMenu = false">Usuarios</router-link>
              <router-link to="/admin/sedes" class="nav-link-mobile" @click="showMobileMenu = false">Sedes</router-link>
            </template>
            <template v-if="modoActual === 'alumno'">
              <router-link to="/alumno/dashboard" class="nav-link-mobile" @click="showMobileMenu = false">Mi Espacio</router-link>
              <router-link to="/reviews" class="nav-link-mobile" @click="showMobileMenu = false">Reseñas</router-link>
              <router-link to="/cart" class="nav-link-mobile" @click="showMobileMenu = false">Carrito</router-link>
            </template>
            <div class="h-px bg-[var(--border-subtle)] my-2"></div>
            <router-link to="/profile" class="nav-link-mobile" @click="showMobileMenu = false">Mi Perfil</router-link>
            <button @click="toggleTheme()" class="nav-link-mobile text-left">{{ isDark ? 'Modo claro' : 'Modo oscuro' }}</button>
            <button @click="handleLogout(); showMobileMenu = false" class="nav-link-mobile text-left text-red-400">Cerrar sesión</button>
          </template>
          <template v-else>
            <button @click="toggleTheme()" class="nav-link-mobile text-left">{{ isDark ? 'Modo claro' : 'Modo oscuro' }}</button>
            <router-link to="/login" class="nav-link-mobile" @click="showMobileMenu = false">Iniciar sesión</router-link>
            <router-link to="/register" class="nav-link-mobile font-medium text-primary" @click="showMobileMenu = false">Registrarse</router-link>
          </template>
        </div>
      </div>
    </Transition>

    <!-- Banner: perfil profesional incompleto (contexto Maestro) -->
    <div v-if="mostrarBannerPerfilIncompleto" class="bg-yellow-500/10 border-b border-yellow-500/30">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3">
        <div class="flex items-center justify-between gap-4 flex-wrap">
          <div class="flex items-start gap-3 flex-1 min-w-0">
            <svg class="w-5 h-5 text-yellow-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
            </svg>
            <div class="min-w-0">
              <p class="text-yellow-200 text-sm font-semibold">Completa tu perfil profesional</p>
              <p class="text-yellow-100/70 text-xs mt-0.5">
                Los alumnos necesitan conocer tu biografía y disciplina principal para inscribirse a tus clases.
              </p>
            </div>
          </div>
          <router-link to="/profesor/perfil-profesional"
                       class="px-4 py-2 rounded-lg bg-yellow-500 text-black text-sm font-semibold hover:bg-yellow-400 transition-colors whitespace-nowrap flex-shrink-0">
            Completar Ahora
          </router-link>
        </div>
      </div>
    </div>

    <!-- Banner: sede sin horario laboral (contexto Sede) -->
    <div v-if="mostrarBannerHorarioIncompleto" class="bg-yellow-500/10 border-b border-yellow-500/30">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3">
        <div class="flex items-center justify-between gap-4 flex-wrap">
          <div class="flex items-start gap-3 flex-1 min-w-0">
            <svg class="w-5 h-5 text-yellow-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
            </svg>
            <div class="min-w-0">
              <p class="text-yellow-200 text-sm font-semibold">Tu sede no tiene horario laboral configurado</p>
              <p class="text-yellow-100/70 text-xs mt-0.5">
                Sin horario no se generan bloques de disponibilidad: tus salas no aparecen reservables para los profesores.
              </p>
            </div>
          </div>
          <router-link to="/sede/configuracion"
                       class="px-4 py-2 rounded-lg bg-yellow-500 text-black text-sm font-semibold hover:bg-yellow-400 transition-colors whitespace-nowrap flex-shrink-0">
            Configurar horario
          </router-link>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <main class="flex-1">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="bg-[var(--bg-footer)] border-t border-[var(--border-subtle)]">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">

          <!-- Logo + descripción corta -->
          <div class="flex items-center gap-3">
            <div class="w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0" style="background: linear-gradient(135deg, #6C63FF, #9B8CFF);">
              <span class="text-white font-bold text-xs">ME</span>
            </div>
            <div>
              <span class="text-sm font-semibold text-[var(--text-primary)]">Modo <span class="text-primary">Ensayo</span></span>
              <p class="text-[var(--text-secondary)] text-xs mt-0.5">Plataforma chilena de danza y música</p>
            </div>
          </div>

          <!-- Links centrados -->
          <div class="flex flex-wrap gap-x-5 gap-y-2">
            <router-link to="/" class="text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs transition-colors">Inicio</router-link>
            <router-link to="/classes" class="text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs transition-colors">Clases</router-link>
            <router-link to="/quiero-ser-profesor" class="text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs transition-colors">Ser Maestro</router-link>
            <router-link to="/quiero-gestionar-sede" class="text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs transition-colors">Gestiónar Sede</router-link>
            <router-link to="/login" class="text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs transition-colors">Iniciar sesión</router-link>
            <router-link to="/register" class="text-[var(--text-secondary)] hover:text-[var(--text-primary)] text-xs transition-colors">Registrarse</router-link>
          </div>

          <!-- Copyright -->
          <span class="text-[var(--text-secondary)] text-xs whitespace-nowrap">© {{ new Date().getFullYear() }} Modo Ensayo · Chile 🇨🇱</span>

        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import paymentService from '@/services/paymentService'
import venueService from '@/services/venueService'
import scheduleService from '@/services/scheduleService'
import NotificationBell from '@/components/NotificationBell.vue'

const router = useRouter()
const route = useRoute()
const { user, isAuthenticated, isAdmin, isSede, isTeacher, identidadValidada, puedeAlternarModo, puedeVerContextoProfesor, puedeVerContextoSede, modoActual, displayName, setModo, logout, syncActividadMaestro, syncAtributos, reservasSinClaseCount, perfilProfesionalCompleto } = useAuth()
const { isDark, toggle: toggleTheme } = useTheme()

const mostrarBannerPerfilIncompleto = computed(() => {
  return modoActual.value === 'profesor'
    && puedeVerContextoProfesor.value
    && !perfilProfesionalCompleto.value
    && route.path !== '/profesor/perfil-profesional'
})

const mostrarBannerHorarioIncompleto = computed(() => {
  return modoActual.value === 'sede'
    && puedeVerContextoSede.value
    && sedeSinHorario.value
    && route.path !== '/sede/configuracion'
})

const showUserMenu = ref(false)
const showMobileMenu = ref(false)
const userMenuRef = ref(null)
const cartCount = ref(0)
const sedeConfirmarCount = ref(0)
const sedeSinHorario = ref(false)

const availableModes = computed(() => {
  const modes = [{ value: 'alumno', label: 'Alumno' }]
  if (puedeVerContextoProfesor.value) modes.push({ value: 'profesor', label: 'Maestro' })
  if (puedeVerContextoSede.value) modes.push({ value: 'sede', label: 'Mi Sede' })
  if (isAdmin.value) modes.push({ value: 'admin', label: 'Admin' })
  return modes
})

const modeLabel = computed(() => {
  const mode = availableModes.value.find((m) => m.value === modoActual.value)
  return mode?.label || 'Alumno'
})

function handleLogout() {
  // Clear auth state immediately to avoid race conditions with event listeners
  localStorage.removeItem('auth_token')
  localStorage.removeItem('auth_user')
  localStorage.removeItem('auth_refresh_token')
  localStorage.removeItem('modoActual')
  logout()
}

function cambiarModo(mode) {
  setModo(mode)
  const dashboards = {
    alumno: '/alumno/dashboard',
    profesor: '/profesor/dashboard',
    sede: '/sede/dashboard',
    admin: '/admin'
  }
  const target = dashboards[mode] || '/'
  router.push(target)
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

async function loadSedeConfirmarCount() {
  if (modoActual.value !== 'sede' || !puedeVerContextoSede.value) return
  try {
    const pending = await venueService.getPendingClasses()
    sedeConfirmarCount.value = Array.isArray(pending) ? pending.length : 0
  } catch {
    sedeConfirmarCount.value = 0
  }
}

// Verifica si la sede aprobada del gestor tiene horario laboral configurado.
// Sin horario no se generan bloques reservables, así que mostramos un aviso
// persistente que obliga a completarlo.
async function checkSedeHorario() {
  if (modoActual.value !== 'sede' || !puedeVerContextoSede.value) {
    sedeSinHorario.value = false
    return
  }
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues?.content || []
    const sede = vArr.find(v => v.status === 'APROBADA')
    if (!sede) { sedeSinHorario.value = false; return }
    const sched = await scheduleService.getSchedule(sede.id)
    sedeSinHorario.value = !(Array.isArray(sched) && sched.length > 0)
  } catch {
    sedeSinHorario.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  if (isAuthenticated.value) {
    loadCartCount()
    loadSedeConfirmarCount()
    checkSedeHorario()
    syncActividadMaestro()
    syncAtributos()
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

watch(() => route.path, () => {
  if (isAuthenticated.value) {
    loadCartCount()
    loadSedeConfirmarCount()
    checkSedeHorario()
  }
  showMobileMenu.value = false
})
</script>

<style scoped>
/* Aurora global */
.aurora {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.aurora-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(55px);
  opacity: 0.22;
  animation: aurora-float 14s ease-in-out infinite alternate;
}

.aurora-purple {
  width: 320px;
  height: 320px;
  background: #6C63FF;
  top: -60px;
  left: -60px;
  animation-duration: 16s;
  animation-delay: 0s;
}

.aurora-blue {
  width: 260px;
  height: 260px;
  background: #2563EB;
  top: 15%;
  right: -40px;
  animation-duration: 12s;
  animation-delay: -4s;
}

.aurora-pink {
  width: 280px;
  height: 280px;
  background: #7C3AED;
  bottom: 15%;
  left: 35%;
  animation-duration: 18s;
  animation-delay: -9s;
}

@keyframes aurora-float {
  0%   { transform: translate(0px, 0px) scale(1); }
  33%  { transform: translate(50px, -40px) scale(1.1); }
  66%  { transform: translate(-30px, 30px) scale(0.92); }
  100% { transform: translate(20px, -20px) scale(1.05); }
}

.nav-link {
  @apply px-3 py-2 rounded-lg text-sm text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-elevated)] transition-all;
}

.nav-link.router-link-active {
  @apply text-primary bg-primary/10;
}

.nav-link-mobile {
  @apply block px-3 py-2.5 rounded-lg text-sm text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-elevated)] transition-all;
}

.nav-link-mobile.router-link-active {
  @apply text-primary bg-primary/10;
}
</style>
