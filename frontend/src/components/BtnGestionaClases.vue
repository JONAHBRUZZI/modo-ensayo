<template>
  <!-- Estado D: invisible cuando InterruptorContexto toma control -->
  <template v-if="estado !== 'D'">
    <!-- Estado A: ir a validar identidad -->
    <router-link v-if="estado === 'A'" to="/profile/identity"
                 class="hidden md:flex items-center gap-1.5 px-3 py-1.5 bg-amber-500/15 hover:bg-amber-500/25 border border-amber-500/30 text-amber-400 rounded-lg text-xs font-medium transition-colors">
      <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V8a2 2 0 00-2-2h-5m-4 0V5a2 2 0 114 0v1m-4 0a2 2 0 104 0m-5 8a2 2 0 100-4 2 2 0 000 4zm0 0c1.306 0 2.417.835 2.83 2M9 14a3.001 3.001 0 00-2.83 2M15 11h3m-3 4h2" />
      </svg>
      Validar Identidad
    </router-link>

    <!-- Estado B: identidad en revisión -->
    <div v-else-if="estado === 'B'"
         class="hidden md:flex items-center gap-1.5 px-3 py-1.5 bg-blue-500/10 border border-blue-500/20 text-blue-400 rounded-lg text-xs font-medium cursor-not-allowed opacity-70">
      <svg class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
      </svg>
      En Revisión
    </div>

    <!-- Estado C-SEDE: director de sede → setModo('sede') + crear clase -->
    <button v-else-if="estado === 'C-SEDE'" @click="irAGestiónar('sede', '/sede/crear-clase')" type="button"
            class="hidden md:flex flex-col items-center justify-center gap-1 w-14 h-12 rounded-xl border transition-all duration-300 bg-emerald-500/10 border-emerald-400/40 shadow-[0_0_14px_rgba(16,185,129,0.25)]">
      <svg class="w-4 h-4 text-emerald-400 drop-shadow-[0_0_5px_rgba(16,185,129,0.8)]"
           fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75" d="M12 14l9-5-9-5-9 5 9 5z" />
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"
          d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
      </svg>
      <span class="text-[9px] font-semibold leading-none text-emerald-300">Gestiónar</span>
    </button>

    <!-- Estado C: identidad validada → setModo('profesor') + buscar sala -->
    <button v-else-if="estado === 'C'" @click="irAGestiónar('profesor', '/profesor/buscar-salas')" type="button"
            class="hidden md:flex flex-col items-center justify-center gap-1 w-14 h-12 rounded-xl border transition-all duration-300 bg-indigo-500/10 border-indigo-400/40 shadow-[0_0_14px_rgba(99,102,241,0.25)]">
      <svg class="w-4 h-4 text-indigo-400 drop-shadow-[0_0_5px_rgba(99,102,241,0.8)]"
           fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75" d="M12 14l9-5-9-5-9 5 9 5z" />
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"
          d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
      </svg>
      <span class="text-[9px] font-semibold leading-none text-indigo-300">Gestiónar</span>
    </button>
  </template>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../hooks/useAuth'

const { isAuthenticated, identidadValidada, identidadEnRevision, tieneReservasActivas, tieneAsignacionesActivas, isSede, setModo } = useAuth()
const router = useRouter()

const estado = computed(() => {
  if (!isAuthenticated.value) return null
  if (tieneReservasActivas.value && tieneAsignacionesActivas.value) return 'D'
  if (isSede.value) return 'C-SEDE'
  if (identidadValidada.value) return 'C'
  if (identidadEnRevision.value) return 'B'
  return 'A'
})

const irAGestiónar = (modo, ruta) => {
  setModo(modo)
  router.push(ruta)
}
</script>
