<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Confirmar Reagendamiento</h1>
    <p class="text-gray-400 text-sm mb-8">La sede propuso un nuevo horario para tu clase. Revisa el cambio y acepta o rechaza.</p>

    <div v-if="loading" class="card text-center py-10 text-gray-400">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <div v-else-if="!reagenda" class="card text-center py-12">
      <p class="text-gray-300 font-medium">Reagendamiento no encontrado</p>
      <p class="text-gray-500 text-sm mt-1">Puede que ya no exista o no tengas acceso.</p>
      <router-link to="/alumno/mis-clases" class="btn-primary inline-flex mt-4">Volver a Mis Clases</router-link>
    </div>

    <template v-else>
      <div class="card space-y-5">
        <div>
          <h3 class="text-white font-semibold text-lg">{{ reagenda.class?.title || 'Clase' }}</h3>
          <p class="text-gray-400 text-sm">{{ reagenda.class?.discipline }}{{ reagenda.class?.level ? ' — ' + reagenda.class.level : '' }}</p>
        </div>

        <!-- Cambio de horario -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div class="rounded-lg border border-white/10 bg-white/5 p-4">
            <p class="text-xs text-gray-500 uppercase tracking-wide mb-1">Horario actual</p>
            <p class="text-gray-300 text-sm line-through">{{ formatFecha(reagenda.class?.startTime) }}</p>
          </div>
          <div class="rounded-lg border border-primary/40 bg-primary/10 p-4">
            <p class="text-xs text-primary uppercase tracking-wide mb-1">Nuevo horario propuesto</p>
            <p class="text-white text-sm font-medium">{{ formatFecha(reagenda.proposedTime) }}</p>
          </div>
        </div>

        <div v-if="reagenda.reason">
          <p class="text-xs text-gray-500 uppercase tracking-wide mb-1">Motivo</p>
          <p class="text-gray-300 text-sm">{{ reagenda.reason }}</p>
        </div>

        <!-- Estado ya resuelto -->
        <div v-if="yaRespondido" class="rounded-lg border border-white/10 bg-white/5 p-4 text-sm text-gray-300">
          Ya respondiste este reagendamiento{{ miRespuesta ? ' (' + textoRespuesta(miRespuesta) + ')' : '' }}.
        </div>

        <!-- Countdown + acciones -->
        <template v-else>
          <p v-if="deadlineTexto" class="text-sm" :class="expirado ? 'text-red-400' : 'text-yellow-400'">
            {{ deadlineTexto }}
          </p>
          <div class="flex flex-col sm:flex-row gap-3 pt-1">
            <button @click="pedirConfirmacion(false)" :disabled="sending" class="btn-secondary flex-1">
              Rechazar
            </button>
            <button @click="pedirConfirmacion(true)" :disabled="sending" class="btn-primary flex-1">
              Aceptar nuevo horario
            </button>
          </div>
        </template>
      </div>

      <router-link to="/alumno/mis-clases" class="inline-flex mt-6 text-sm text-gray-400 hover:text-white transition-colors">
        ← Volver a Mis Clases
      </router-link>
    </template>

    <!-- Modal de confirmación -->
    <BottomSheet v-model="confirmAbierto">
      <h3 class="text-lg font-semibold text-white mb-2">
        {{ accionAceptar ? 'Aceptar reagendamiento' : 'Rechazar reagendamiento' }}
      </h3>
      <p class="text-gray-400 text-sm mb-6">
        <template v-if="accionAceptar">
          Confirmas asistir a tu clase en el nuevo horario:
          <span class="text-white">{{ formatFecha(reagenda?.proposedTime) }}</span>.
        </template>
        <template v-else>
          Si rechazas, se cancelará tu inscripción a esta clase y se procesará el
          <span class="text-white">reembolso</span> de tu pago. Esta acción no se puede deshacer.
        </template>
      </p>
      <div class="flex gap-3">
        <button @click="confirmAbierto = false" :disabled="sending" class="btn-secondary flex-1">Volver</button>
        <button @click="confirmar" :disabled="sending" class="btn-primary flex-1">
          {{ sending ? 'Enviando...' : (accionAceptar ? 'Confirmar' : 'Sí, rechazar') }}
        </button>
      </div>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import rescheduleService from '@/services/rescheduleService'
import { useToast } from '@/composables/useToast'
import BottomSheet from '@/components/BottomSheet.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const reagenda = ref(null)
const loading = ref(true)
const sending = ref(false)
const confirmAbierto = ref(false)
const accionAceptar = ref(true)
const miRespuesta = ref(null)

const ahora = ref(Date.now())
let timer = null

const yaRespondido = computed(() => miRespuesta.value != null)

const deadlineTexto = computed(() => {
  const dl = reagenda.value?.responseDeadline
  if (!dl) return ''
  const restante = new Date(dl).getTime() - ahora.value
  if (restante <= 0) return 'El plazo para responder venció.'
  const h = Math.floor(restante / 3_600_000)
  const m = Math.floor((restante % 3_600_000) / 60_000)
  return `Tienes ${h}h ${m}m para responder.`
})

const expirado = computed(() => {
  const dl = reagenda.value?.responseDeadline
  return dl ? new Date(dl).getTime() - ahora.value <= 0 : false
})

function textoRespuesta(r) {
  return { ACCEPTED: 'aceptado', REJECTED: 'rechazado', TIMEOUT: 'vencido' }[r] || r
}

function formatFecha(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  const fecha = d.toLocaleDateString('es-CL', { weekday: 'long', day: 'numeric', month: 'long' })
  const hora = d.toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' })
  return `${fecha}, ${hora}`
}

function pedirConfirmacion(aceptar) {
  accionAceptar.value = aceptar
  confirmAbierto.value = true
}

async function confirmar() {
  sending.value = true
  try {
    await rescheduleService.studentDecision(route.params.rescheduleId, accionAceptar.value)
    confirmAbierto.value = false
    toast.success(accionAceptar.value
      ? 'Confirmaste el nuevo horario de tu clase.'
      : 'Rechazaste el reagendamiento. Se procesará tu reembolso.')
    router.push('/alumno/mis-clases')
  } catch (e) {
    toast.error(e.response?.data?.message || 'No se pudo registrar tu respuesta.')
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  try {
    const { data } = await rescheduleService.getReschedule(route.params.rescheduleId)
    reagenda.value = data
    // ¿Ya respondí? La RLS de reschedule_responses (rrep_select_own) solo
    // devuelve mis propias filas, así que basta con buscar una ya respondida.
    const resp = await rescheduleService.getStudentResponses(route.params.rescheduleId)
    const mias = Array.isArray(resp.data) ? resp.data : []
    miRespuesta.value = mias.find(r => r.responseType != null)?.responseType ?? null
  } catch {
    reagenda.value = null
  } finally {
    loading.value = false
  }
  timer = setInterval(() => { ahora.value = Date.now() }, 30_000)
})

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>
