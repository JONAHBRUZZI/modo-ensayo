<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-2">Mi Espacio</h1>
    <p class="text-gray-400 mb-8">Bienvenido, {{ displayName }}</p>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
      <div class="card">
        <h3 class="text-gray-400 text-sm mb-1">Clases Tomadas</h3>
        <p class="text-3xl font-bold text-white">{{ stats.totalClases || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-400 text-sm mb-1">Próximas Clases</h3>
        <p class="text-3xl font-bold text-primary">{{ stats.proximas || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-400 text-sm mb-1">Identidad</h3>
        <EstadoBadge :status="identidadEstado" />
      </div>
    </div>

    <!-- Banner identidad no validada -->
    <div v-if="!identidadValidada && !identidadEnRevision"
      class="mb-8 p-4 rounded-xl border border-amber-500/30 bg-amber-500/10 flex items-start gap-3">
      <svg class="w-5 h-5 text-amber-400 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <div class="flex-1">
        <p class="text-amber-300 text-sm font-medium">Valida tu identidad para desbloquear más funciones</p>
        <p class="text-amber-200/70 text-xs mt-1">
          Con identidad validada podrás registrar una sede, reservar salas y crear clases como maestro.
        </p>
      </div>
      <router-link to="/profile/identity"
        class="shrink-0 text-xs font-semibold text-amber-300 hover:text-amber-200 border border-amber-500/40 rounded-lg px-3 py-1.5 transition-colors">
        Validar ahora
      </router-link>
    </div>

    <!-- Acciones principales -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
      <router-link to="/alumno/mis-clases"
        class="card hover:border-primary/50 transition-colors group">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-primary/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Mis Clases</h3>
            <p class="text-gray-400 text-sm mt-1">Clases en las que estás inscrito y su estado.</p>
          </div>
        </div>
      </router-link>

      <router-link to="/classes"
        class="card hover:border-primary/50 transition-colors group">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-green-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Explorar Clases</h3>
            <p class="text-gray-400 text-sm mt-1">Busca y reserva clases de cualquier disciplina.</p>
          </div>
        </div>
      </router-link>

      <router-link to="/alumno/pagos"
        class="card hover:border-primary/50 transition-colors group">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-yellow-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Historial de Pagos</h3>
            <p class="text-gray-400 text-sm mt-1">Revisa tus pagos y comprobantes.</p>
          </div>
        </div>
      </router-link>

      <router-link to="/alumno/asociados"
        class="card hover:border-primary/50 transition-colors group">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-purple-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Asociados</h3>
            <p class="text-gray-400 text-sm mt-1">Gestiona personas que inscriben a tus clases.</p>
          </div>
        </div>
      </router-link>
    </div>

    <!-- Acciones bloqueadas hasta validar identidad -->
    <div class="border-t border-white/5 pt-6">
      <h2 class="text-sm font-medium text-gray-500 uppercase tracking-wider mb-4">Requieren identidad validada</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">

        <!-- Registrar sede -->
        <div v-if="identidadValidada">
          <router-link to="/sede/registro"
            class="card hover:border-emerald-500/50 transition-colors group flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-emerald-500/20 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5" />
              </svg>
            </div>
            <div>
              <h3 class="text-base font-semibold text-white group-hover:text-emerald-400 transition-colors">Registrar mi Sede</h3>
              <p class="text-gray-400 text-sm mt-1">Sede o HomeStudio para ofrecer espacios de ensayo.</p>
            </div>
          </router-link>
        </div>
        <div v-else class="card opacity-50 cursor-not-allowed flex items-start gap-3" @click="mostrarBloqueo">
          <div class="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-emerald-900" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5" />
            </svg>
          </div>
          <div>
            <h3 class="text-base font-semibold text-gray-600">Registrar mi Sede</h3>
            <p class="text-gray-600 text-sm mt-1">Requiere identidad validada.</p>
          </div>
        </div>

        <!-- Crear perfil de maestro / buscar salas -->
        <div v-if="identidadValidada">
          <router-link to="/profesor/buscar-salas"
            class="card hover:border-indigo-500/50 transition-colors group flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-indigo-500/20 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
              </svg>
            </div>
            <div>
              <h3 class="text-base font-semibold text-white group-hover:text-indigo-400 transition-colors">Agenda tu Sala</h3>
              <p class="text-gray-400 text-sm mt-1">Coordina tus propias clases.</p>
            </div>
          </router-link>
        </div>
        <div v-else class="card opacity-50 cursor-not-allowed flex items-start gap-3" @click="mostrarBloqueo">
          <div class="w-10 h-10 rounded-xl bg-indigo-500/10 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-indigo-900" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 14l9-5-9-5-9 5 9 5z" />
            </svg>
          </div>
          <div>
            <h3 class="text-base font-semibold text-gray-600">Agenda tu Sala</h3>
            <p class="text-gray-600 text-sm mt-1">Requiere identidad validada.</p>
          </div>
        </div>

      </div>
    </div>

    <!-- Toast bloqueo -->
    <Transition enter-active-class="transition duration-200" enter-from-class="opacity-0 translate-y-2"
      enter-to-class="opacity-100 translate-y-0" leave-active-class="transition duration-150"
      leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="toastVisible"
        class="fixed bottom-6 left-1/2 -translate-x-1/2 bg-gray-900 border border-amber-500/40 text-amber-300 text-sm px-5 py-3 rounded-xl shadow-xl z-50">
        Debes validar tu identidad para acceder a esta función.
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '@/stores/auth'
import EstadoBadge from '@/components/EstadoBadge.vue'
import api from '@/services/api'

const { displayName, identidadValidada, identidadEnRevision, identidadRechazada, syncIdentityStatus, syncActividadMaestro } = useAuth()

const stats = ref({ totalClases: 0, proximas: 0 })
const toastVisible = ref(false)

onMounted(async () => {
  syncIdentityStatus()
  syncActividadMaestro()
  try {
    const res = await api.get('/users/me/stats')
    stats.value = res.data
  } catch {}
})

const identidadEstado = computed(() => {
  if (identidadValidada.value) return 'APPROVED'
  if (identidadEnRevision.value) return 'PENDING'
  if (identidadRechazada.value) return 'REJECTED'
  return 'NO_SOLICITADA'
})

function mostrarBloqueo() {
  toastVisible.value = true
  setTimeout(() => { toastVisible.value = false }, 3000)
}
</script>
