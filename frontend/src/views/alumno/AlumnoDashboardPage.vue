<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-[var(--text-primary)] mb-8">Hola, {{ displayName }}</h1>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-10">
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Clases Tomadas</h3>
        <p class="text-3xl font-bold text-white">{{ stats.totalClases || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Próximas Clases</h3>
        <p class="text-3xl font-bold text-primary">{{ stats.próximas || 0 }}</p>
      </div>
    </div>

    <!-- Acciones principales -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
      <router-link to="/alumno/mis-clases" class="card hover:border-primary/50 transition-colors group">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-primary/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-[var(--text-primary)] group-hover:text-primary transition-colors">Mis Clases</h3>
            <p class="text-gray-400 text-sm mt-1">Clases en las que estás inscrito y su estado.</p>
          </div>
        </div>
      </router-link>

      <router-link to="/classes" class="card hover:border-primary/50 transition-colors group">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-green-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-[var(--text-primary)] group-hover:text-primary transition-colors">Explorar Clases</h3>
            <p class="text-gray-400 text-sm mt-1">Busca y reserva clases de cualquier disciplina.</p>
          </div>
        </div>
      </router-link>

      <router-link to="/alumno/pagos" class="card hover:border-primary/50 transition-colors group">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-yellow-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-[var(--text-primary)] group-hover:text-primary transition-colors">Historial de Pagos</h3>
            <p class="text-gray-400 text-sm mt-1">Revisa tus pagos y comprobantes.</p>
          </div>
        </div>
      </router-link>

      <router-link to="/alumno/asociados" class="card hover:border-primary/50 transition-colors group">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-purple-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-[var(--text-primary)] group-hover:text-primary transition-colors">Asociados</h3>
            <p class="text-gray-400 text-sm mt-1">Gestióna personas que inscriben a tus clases.</p>
          </div>
        </div>
      </router-link>

      <router-link to="/alumno/mis-clases/calendario" class="card hover:border-primary/50 transition-colors group">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-blue-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-semibold text-[var(--text-primary)] group-hover:text-primary transition-colors">Calendario</h3>
            <p class="text-gray-400 text-sm mt-1">Tus clases próximas en vista de calendario.</p>
          </div>
        </div>
      </router-link>
    </div>

    <!-- Sección de estados: Identidad + Sede/HomeStudio + Maestro -->
    <div class="border-t border-white/5 pt-6">
      <h2 class="text-sm font-medium text-gray-500 uppercase tracking-wider mb-4">Mi perfil en Modo Ensayo</h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">

        <!-- ── IDENTIDAD ── -->

        <!-- Aprobada -->
        <div v-if="identidadValidada"
          class="card border border-green-500/30 bg-green-500/5 flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-green-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <div class="flex items-center gap-2 mb-0.5">
              <h3 class="text-base font-semibold text-white">Identidad</h3>
              <span class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-green-500/20 text-green-300 uppercase tracking-wider">Verificada</span>
            </div>
            <p class="text-gray-400 text-sm">Tu identidad ha sido validada correctamente.</p>
          </div>
        </div>

        <!-- En revisión -->
        <div v-else-if="identidadEnRevision"
          class="card border border-amber-500/30 bg-amber-500/5 flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-amber-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <div class="flex items-center gap-2 mb-0.5">
              <h3 class="text-base font-semibold text-white">Identidad</h3>
              <span class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 uppercase tracking-wider">Pendiente</span>
            </div>
            <p class="text-gray-400 text-sm">Tu documento está siendo revisado. Te notificaremos cuando sea aprobado.</p>
          </div>
        </div>

        <!-- Rechazada -->
        <div v-else-if="identidadRechazada"
          class="card border border-red-500/30 bg-red-500/5 flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-red-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-0.5">
              <h3 class="text-base font-semibold text-white">Identidad</h3>
              <span class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-red-500/20 text-red-400 uppercase tracking-wider">Rechazada</span>
            </div>
            <p class="text-gray-400 text-sm mb-3">Tu documento fue rechazado. Súbelo nuevamente con una imagen clara.</p>
            <router-link to="/profile/identity"
              class="inline-block text-xs font-semibold bg-red-500/20 hover:bg-red-500/30 text-red-300 px-3 py-1.5 rounded-lg transition-colors">
              Volver a validar
            </router-link>
          </div>
        </div>

        <!-- Sin validar -->
        <div v-else>
          <router-link to="/profile/identity"
            class="card hover:border-amber-500/50 transition-colors group flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-amber-500/20 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0a2 2 0 104 0m-5 8a2 2 0 100-4 2 2 0 000 4zm0 0c1.306 0 2.417.835 2.83 2M9 14a3.001 3.001 0 00-2.83 2M15 11h3m-3 4h2" />
              </svg>
            </div>
            <div>
              <h3 class="text-base font-semibold text-white group-hover:text-amber-400 transition-colors">Validar Identidad</h3>
              <p class="text-gray-400 text-sm mt-1">Sube tu cédula o pasaporte para desbloquear más funciones.</p>
            </div>
          </router-link>
        </div>

        <!-- ── SEDE / HOMESTUDIO ── -->

        <!-- Aprobada -->
        <div v-if="tieneSedeAprobada">
          <button @click="irAMiSede"
            class="card hover:border-emerald-500/50 transition-colors group flex items-start gap-3 text-left w-full">
            <div class="w-10 h-10 rounded-xl bg-emerald-500/20 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5" />
              </svg>
            </div>
            <div>
              <div class="flex items-center gap-2 mb-0.5">
                <h3 class="text-base font-semibold text-white group-hover:text-emerald-400 transition-colors">Mi Sede</h3>
                <span class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-green-500/20 text-green-300 uppercase tracking-wider">Aprobada</span>
              </div>
              <p class="text-gray-400 text-sm mt-1">Gestióna tu sede, salas y disponibilidad.</p>
            </div>
          </button>
        </div>

        <!-- Pendiente -->
        <div v-else-if="estadoSolicitudSede === 'PENDIENTE_APROBACION'"
          class="card border border-amber-500/30 bg-amber-500/5 flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-amber-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <div class="flex items-center gap-2 mb-0.5">
              <h3 class="text-base font-semibold text-white">Sede / HomeStudio</h3>
              <span class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 uppercase tracking-wider">Pendiente</span>
            </div>
            <p class="text-gray-400 text-sm">Tu solicitud está siendo revisada. Te notificaremos cuando sea aprobada.</p>
          </div>
        </div>

        <!-- Rechazada -->
        <div v-else-if="estadoSolicitudSede === 'RECHAZADA'"
          class="card border border-red-500/30 bg-red-500/5 flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-red-500/20 flex items-center justify-center shrink-0">
            <svg class="w-5 h-5 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-0.5">
              <h3 class="text-base font-semibold text-white">Sede / HomeStudio</h3>
              <span class="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-red-500/20 text-red-400 uppercase tracking-wider">Rechazada</span>
            </div>
            <p class="text-gray-400 text-sm mb-1">Tu solicitud fue rechazada. Corrige la información y vuelve a enviarla.</p>
            <p v-if="motivoRechazoSede" class="text-red-300/80 text-xs mb-3 italic">"{{ motivoRechazoSede }}"</p>
            <p v-else class="text-gray-500 text-xs mb-3">No se especificó un motivo.</p>
            <router-link to="/sede/registro"
              class="inline-block text-xs font-semibold bg-red-500/20 hover:bg-red-500/30 text-red-300 px-3 py-1.5 rounded-lg transition-colors">
              Volver a registrar
            </router-link>
          </div>
        </div>

        <!-- Sin solicitud -->
        <div v-else>
          <router-link to="/sede/registro"
            class="card hover:border-emerald-500/50 transition-colors group flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-emerald-500/20 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5" />
              </svg>
            </div>
            <div>
              <h3 class="text-base font-semibold text-white group-hover:text-emerald-400 transition-colors">Registrar Sede / HomeStudio</h3>
              <p class="text-gray-400 text-sm mt-1">Ofrece tu espacio para ensayos y clases.</p>
            </div>
          </router-link>
        </div>

        <!-- ── MAESTRO / BUSCAR SALAS ── -->
        <div v-if="identidadValidada">
          <button @click="irABuscarSalas"
            class="card hover:border-indigo-500/50 transition-colors group flex items-start gap-3 text-left w-full">
            <div class="w-10 h-10 rounded-xl bg-indigo-500/20 flex items-center justify-center shrink-0">
              <svg class="w-5 h-5 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
              </svg>
            </div>
            <div>
              <h3 class="text-base font-semibold text-white group-hover:text-indigo-400 transition-colors">
                Agenda tu Sala
                <span v-if="puedeVerContextoProfesor"
                  class="ml-2 inline-block text-[10px] font-medium px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 uppercase tracking-wider align-middle">
                  Maestro
                </span>
              </h3>
              <p class="text-gray-400 text-sm mt-1">
                {{ puedeVerContextoProfesor ? 'Reserva en tu contexto de Maestro.' : 'Coordina tus propias clases.' }}
              </p>
            </div>
          </button>
        </div>
        <div v-else
          class="card opacity-40 cursor-not-allowed flex items-start gap-3">
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import api from '@/services/api'

const router = useRouter()
const { displayName, identidadValidada, identidadEnRevision, identidadRechazada,
        puedeVerContextoProfesor, tieneSedeAprobada, estadoSolicitudSede, motivoRechazoSede,
        setModo, syncAtributos } = useAuth()

const stats = ref({ totalClases: 0, próximas: 0 })

onMounted(async () => {
  syncAtributos()
  try {
    const res = await api.get('/users/me/stats')
    stats.value = res.data
  } catch (err) {
    console.error('Error al cargar estadísticas del alumno', err)
  }
})

function irAMiSede() {
  setModo('sede')
  router.push('/sede/dashboard')
}

function irABuscarSalas() {
  if (puedeVerContextoProfesor.value) setModo('profesor')
  router.push('/profesor/buscar-salas')
}
</script>
