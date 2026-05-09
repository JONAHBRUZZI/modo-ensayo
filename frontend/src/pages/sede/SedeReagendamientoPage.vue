<template>
  <div class="max-w-xl mx-auto space-y-6">
    <div>
      <router-link to="/sede/mis-clases" class="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-300 transition-colors mb-4">
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        Volver a mis clases
      </router-link>
      <h1 class="text-2xl font-bold text-white">Reagendamiento de Clase</h1>
      <p class="text-gray-400 text-sm mt-0.5">Revisa y responde la solicitud de cambio</p>
    </div>

    <div class="bg-[#161824] rounded-xl border border-white/10 p-5 space-y-4">
      <div class="flex items-start gap-3 pb-4 border-b border-white/5">
        <div class="w-9 h-9 bg-amber-500/20 rounded-full flex items-center justify-center flex-shrink-0">
          <svg class="w-4 h-4 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div>
          <p class="text-sm font-semibold text-white">Solicitud de reagendamiento</p>
          <p class="text-xs text-gray-400 mt-0.5">El profesor solicita cambiar el horario de esta clase</p>
        </div>
        <EstadoBadge estado="EN_ESPERA" />
      </div>

      <div class="grid grid-cols-2 gap-4 text-sm">
        <div>
          <p class="text-xs text-gray-500 mb-0.5">Horario actual</p>
          <p class="text-white font-medium">Lunes 18:00</p>
        </div>
        <div>
          <p class="text-xs text-gray-500 mb-0.5">Horario propuesto</p>
          <p class="text-amber-400 font-medium">Miércoles 19:00</p>
        </div>
        <div class="col-span-2">
          <p class="text-xs text-gray-500 mb-0.5">Motivo</p>
          <p class="text-white">"Compromiso académico el día lunes"</p>
        </div>
      </div>

      <div class="flex gap-2 pt-2">
        <button @click="responder('rechazado')" :disabled="processing"
          class="flex-1 py-2.5 border border-red-500/30 text-red-400 hover:bg-red-500/10 rounded-lg text-sm font-medium transition-colors disabled:opacity-50">
          Rechazar
        </button>
        <button @click="responder('aprobado')" :disabled="processing"
          class="flex-1 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-sm font-medium transition-colors disabled:opacity-50">
          Aprobar
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import EstadoBadge from '../../components/EstadoBadge.vue'

const router = useRouter()
const processing = ref(false)

const responder = async (decision) => {
  processing.value = true
  try {
    await new Promise(r => setTimeout(r, 800))
    router.push('/sede/mis-clases')
  } finally {
    processing.value = false
  }
}
</script>
