<template>
  <Transition
    enter-active-class="transition-all duration-300 ease-out"
    enter-from-class="opacity-0 scale-95"
    enter-to-class="opacity-100 scale-100"
    leave-active-class="transition-all duration-150 ease-in"
    leave-from-class="opacity-100 scale-100"
    leave-to-class="opacity-0 scale-95">

    <div v-if="puedeAlternarModo" class="flex items-center gap-1.5">

      <!-- Alumno (siempre) -->
      <button @click="activar('alumno')" type="button"
        class="group flex flex-col items-center justify-center gap-1 w-14 h-12 rounded-xl border transition-all duration-300"
        :class="modoActual === 'alumno'
          ? 'bg-purple-500/10 border-purple-500/40 shadow-[0_0_14px_rgba(168,85,247,0.25)]'
          : 'bg-white/3 border-white/8 hover:border-purple-500/30 hover:bg-purple-500/5'">
        <svg class="w-4 h-4 transition-all duration-300"
          :class="modoActual === 'alumno'
            ? 'text-purple-400 drop-shadow-[0_0_5px_rgba(168,85,247,0.8)]'
            : 'text-gray-600 group-hover:text-purple-400'"
          fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"
            d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
        <span class="text-[9px] font-semibold leading-none transition-colors duration-300"
          :class="modoActual === 'alumno' ? 'text-purple-300' : 'text-gray-600 group-hover:text-purple-300'">
          Alumno
        </span>
      </button>

      <!-- Maestro: ACTIVO (normal), DORMIDO (gris + tooltip), INACTIVO (oculto) -->
      <div v-if="puedeVerContextoProfesor" class="relative group/dormido">
        <button
          @click="estadoProfesor !== 'DORMIDO' && activar('profesor')"
          type="button"
          :disabled="estadoProfesor === 'DORMIDO'"
          class="flex flex-col items-center justify-center gap-1 w-14 h-12 rounded-xl border transition-all duration-300"
          :class="estadoProfesor === 'DORMIDO'
            ? 'bg-white/2 border-white/5 cursor-not-allowed opacity-40'
            : modoActual === 'profesor'
              ? 'bg-indigo-500/10 border-indigo-400/40 shadow-[0_0_14px_rgba(99,102,241,0.25)]'
              : 'group bg-white/3 border-white/8 hover:border-indigo-500/30 hover:bg-indigo-500/5'">
          <svg class="w-4 h-4 transition-all duration-300"
            :class="estadoProfesor === 'DORMIDO'
              ? 'text-gray-600'
              : modoActual === 'profesor'
                ? 'text-indigo-400 drop-shadow-[0_0_5px_rgba(99,102,241,0.8)]'
                : 'text-gray-600 group-hover:text-indigo-400'"
            fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"
              d="M12 14l9-5-9-5-9 5 9 5z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"
              d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
          </svg>
          <span class="text-[9px] font-semibold leading-none transition-colors duration-300"
            :class="estadoProfesor === 'DORMIDO'
              ? 'text-gray-600'
              : modoActual === 'profesor' ? 'text-indigo-300' : 'text-gray-600 group-hover:text-indigo-300'">
            Maestro
          </span>
        </button>
        <!-- Tooltip DORMIDO -->
        <div v-if="estadoProfesor === 'DORMIDO'"
          class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-44 px-2 py-1.5 bg-gray-900 border border-white/10 rounded-lg text-[10px] text-gray-300 leading-tight pointer-events-none opacity-0 group-hover/dormido:opacity-100 transition-opacity duration-200 text-center z-50">
          Sin clases activas. Crea una clase para activar el contexto Maestro.
          <span class="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-gray-900"></span>
        </div>
      </div>

      <!-- Mi Sede (condicional) -->
      <button v-if="puedeVerContextoSede" @click="activar('sede')" type="button"
        class="group flex flex-col items-center justify-center gap-1 w-14 h-12 rounded-xl border transition-all duration-300"
        :class="modoActual === 'sede'
          ? 'bg-emerald-500/10 border-emerald-400/40 shadow-[0_0_14px_rgba(16,185,129,0.25)]'
          : 'bg-white/3 border-white/8 hover:border-emerald-500/30 hover:bg-emerald-500/5'">
        <svg class="w-4 h-4 transition-all duration-300"
          :class="modoActual === 'sede'
            ? 'text-emerald-400 drop-shadow-[0_0_5px_rgba(16,185,129,0.8)]'
            : 'text-gray-600 group-hover:text-emerald-400'"
          fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"
            d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
        </svg>
        <span class="text-[9px] font-semibold leading-none transition-colors duration-300"
          :class="modoActual === 'sede' ? 'text-emerald-300' : 'text-gray-600 group-hover:text-emerald-300'">
          Mi Sede
        </span>
      </button>

    </div>
  </Transition>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuth } from '@/stores/auth'
import { useRouter } from 'vue-router'

const {
  puedeAlternarModo,
  puedeVerContextoProfesor,
  puedeVerContextoSede,
  estadoProfesor,
  modoActual,
  setModo,
  isAuthenticated,
  syncAtributos
} = useAuth()

const router = useRouter()

const destinos = { alumno: '/alumno/dashboard', profesor: '/profesor/dashboard', sede: '/sede/dashboard' }

const activar = (modo) => {
  setModo(modo)
  router.push(destinos[modo])
}

onMounted(() => {
  if (isAuthenticated.value) syncAtributos()
})
</script>
