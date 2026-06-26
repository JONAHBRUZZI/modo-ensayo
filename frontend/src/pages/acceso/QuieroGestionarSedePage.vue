<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="min-h-[70vh] flex items-center justify-center py-16 px-4"
  >
    <div class="max-w-md w-full space-y-6">

      <!-- Icono principal -->
      <div class="text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl mb-5"
          :class="estadoConfig.iconBg">
          <svg class="w-8 h-8" :class="estadoConfig.iconColor" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="estadoConfig.iconPath" />
          </svg>
        </div>
        <h1 class="text-2xl font-bold text-white mb-2">{{ estadoConfig.titulo }}</h1>
        <p class="text-gray-400 text-sm leading-relaxed">{{ estadoConfig.descripcion }}</p>
      </div>

      <!-- Pasos del proceso (siempre visible) -->
      <div class="bg-[var(--bg-overlay)] rounded-2xl border border-white/10 p-5 space-y-3">
        <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-4">Proceso para registrar una Sede</p>
        <div v-for="(paso, i) in pasos" :key="i" class="flex items-start gap-3">
          <div class="w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5 text-xs font-bold"
            :class="paso.completado ? 'bg-emerald-500/20 text-emerald-400' : paso.activo ? 'bg-indigo-600 text-white' : 'bg-white/5 text-gray-500'">
            <svg v-if="paso.completado" class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
            </svg>
            <span v-else>{{ i + 1 }}</span>
          </div>
          <div>
            <p class="text-sm font-medium" :class="paso.activo ? 'text-white' : paso.completado ? 'text-emerald-400' : 'text-gray-500'">
              {{ paso.titulo }}
            </p>
            <p class="text-xs text-gray-600 mt-0.5">{{ paso.detalle }}</p>
          </div>
        </div>
      </div>

      <!-- CTA según estado -->
      <div class="space-y-3">
        <!-- No autenticado -->
        <template v-if="!isAuthenticated">
          <router-link to="/register"
            class="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-sm font-semibold transition-colors">
            Crear cuenta gratis
          </router-link>
          <router-link to="/login"
            class="w-full flex items-center justify-center gap-2 py-2.5 border border-[var(--border-default)] text-[var(--text-secondary)] rounded-xl text-sm hover:bg-[var(--bg-elevated)] hover:text-[var(--text-primary)] transition-colors">
            Ya tengo cuenta
          </router-link>
        </template>

        <!-- Autenticado sin identidad validada -->
        <template v-else-if="!identidadValidada && !identidadEnRevision">
          <router-link to="/profile/identity"
            class="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-sm font-semibold transition-colors shadow-lg shadow-indigo-600/20">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0a2 2 0 104 0m-5 8a2 2 0 100-4 2 2 0 000 4zm0 0c1.306 0 2.417.835 2.83 2M9 14a3.001 3.001 0 00-2.83 2M15 11h3m-3 4h2" />
            </svg>
            Validar mi identidad
          </router-link>
        </template>

        <!-- Identidad en revisión -->
        <template v-else-if="identidadEnRevision && !identidadValidada">
          <div class="w-full flex items-center justify-center gap-2 py-3 bg-blue-500/15 border border-blue-500/30 text-blue-300 rounded-xl text-sm font-medium">
            <svg class="w-4 h-4 animate-pulse" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Tu identidad está siendo revisada
          </div>
          <p class="text-center text-xs text-gray-600">Te notificaremos cuando sea aprobada (hasta 24 hrs)</p>
        </template>

        <!-- Identidad validada, puede registrar sede -->
        <template v-else-if="identidadValidada">
          <router-link to="/sede/registro"
            class="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-sm font-semibold transition-colors shadow-lg shadow-indigo-600/20">
            Registrar mi sede
          </router-link>
        </template>

        <router-link to="/"
          class="w-full flex items-center justify-center gap-1.5 pt-4 border-t border-[var(--border-subtle)] text-xs font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors">
          <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
          Volver al inicio
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useAuth } from '@/stores/auth'

const { isAuthenticated, identidadValidada, identidadEnRevision, syncIdentityStatus } = useAuth()

onMounted(() => {
  if (isAuthenticated.value) syncIdentityStatus()
})

const estadoConfig = computed(() => {
  if (!isAuthenticated.value) return {
    titulo: 'Quiero gestiónar una Sede',
    descripcion: 'Para registrar tu sede y ofrecer espacios a profesores, primero debes crear una cuenta y validar tu identidad.',
    iconBg: 'bg-indigo-500/20', iconColor: 'text-indigo-400',
    iconPath: 'M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z',
  }
  if (!identidadValidada.value && !identidadEnRevision.value) return {
    titulo: 'Valida tu identidad primero',
    descripcion: 'Antes de registrar una sede, debes verificar quién eres. Es un proceso simple y se hace una sola vez.',
    iconBg: 'bg-amber-500/20', iconColor: 'text-amber-400',
    iconPath: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z',
  }
  if (identidadEnRevision.value && !identidadValidada.value) return {
    titulo: 'Identidad en revisión',
    descripcion: 'Tu documento está siendo revisado por el equipo de Modo Ensayo. Una vez aprobado, podrás registrar tu sede.',
    iconBg: 'bg-blue-500/20', iconColor: 'text-blue-400',
    iconPath: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
  }
  return {
    titulo: '¡Listo para registrar tu sede!',
    descripcion: 'Tu identidad fue verificada. Ahora puedes registrar tu sede, agregar salas y gestiónar la disponibilidad.',
    iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400',
    iconPath: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
  }
})

const pasos = computed(() => [
  {
    titulo: 'Crear cuenta',
    detalle: 'Regístrate con tu email',
    completado: isAuthenticated.value,
    activo: !isAuthenticated.value,
  },
  {
    titulo: 'Validar identidad',
    detalle: 'Sube tu cédula o pasaporte',
    completado: identidadValidada.value,
    activo: isAuthenticated.value && !identidadValidada.value && !identidadEnRevision.value,
  },
  {
    titulo: 'Revisión del equipo',
    detalle: 'Aprobación en hasta 24 horas',
    completado: identidadValidada.value,
    activo: identidadEnRevision.value && !identidadValidada.value,
  },
  {
    titulo: 'Registrar tu sede',
    detalle: 'Agrega salas y configura disponibilidad',
    completado: false,
    activo: identidadValidada.value,
  },
])
</script>
