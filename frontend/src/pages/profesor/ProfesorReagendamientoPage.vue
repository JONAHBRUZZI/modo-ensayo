<template>
  <div class="max-w-xl mx-auto space-y-6">
    <div>
      <router-link to="/profesor/clases-propias" class="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-300 transition-colors mb-4">
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
        Volver a mis clases
      </router-link>
      <h1 class="text-2xl font-bold text-white">Solicitar Reagendamiento</h1>
      <p class="text-gray-400 text-sm mt-0.5">Propón un nuevo horario para esta clase</p>
    </div>

    <!-- Ventana de tiempo -->
    <div class="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4">
      <ContadorRegresivo v-if="claseActual?.startTime" :deadline="ventanaDeadline" />
      <p class="text-xs text-amber-400/70 mt-1">Solo puedes reagendar dentro de las 72 horas previas a la clase</p>
    </div>

    <div v-if="claseActual" class="bg-[#161824] rounded-xl border border-white/10 p-5">
      <h2 class="text-sm font-semibold text-white mb-1">{{ claseActual.title }}</h2>
      <p class="text-xs text-gray-500 mb-5">Horario actual: {{ formatFecha(claseActual.startTime) }}</p>

      <div class="space-y-4">
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Nuevo horario propuesto *</label>
          <input v-model="form.newStartTime" type="datetime-local"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white text-sm [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-amber-500 transition-all" />
        </div>

        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Motivo del reagendamiento *</label>
          <textarea v-model="form.reason" rows="3" placeholder="Explica el motivo de la solicitud..."
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-amber-500 transition-all resize-none"></textarea>
        </div>

        <button @click="enviar" :disabled="!form.newStartTime || !form.reason || sending"
          class="w-full py-2.5 bg-amber-600 hover:bg-amber-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg text-sm font-semibold transition-colors">
          {{ sending ? 'Enviando...' : 'Enviar Solicitud' }}
        </button>
      </div>
    </div>

    <div v-else class="text-center py-10 bg-[#161824] rounded-xl border border-white/10">
      <div class="animate-spin w-6 h-6 border-2 border-amber-500 border-t-transparent rounded-full mx-auto"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { classService } from '../../services/classService'
import ContadorRegresivo from '../../components/ContadorRegresivo.vue'

const route = useRoute()
const router = useRouter()
const claseActual = ref(null)
const sending = ref(false)
const form = ref({ newStartTime: '', reason: '' })

const ventanaDeadline = computed(() => claseActual.value?.startTime || new Date())

onMounted(async () => {
  try {
    const clases = await classService.getMyClasses()
    claseActual.value = clases.find(c => String(c.id) === String(route.params.claseId))
  } catch { /* ignore */ }
})

const formatFecha = (d) => d ? new Date(d).toLocaleDateString('es-CL', { weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' }) : '—'

const enviar = async () => {
  if (!form.value.newStartTime || !form.value.reason) return
  sending.value = true
  try {
    await classService.requestReschedule?.(route.params.claseId, form.value)
    router.push('/profesor/clases-propias')
  } catch (e) {
    alert(e?.response?.data?.message || 'Error al enviar solicitud')
  } finally {
    sending.value = false
  }
}
</script>
