<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Registrar Sala</h1>

    <div v-if="!identidadValidada" class="card space-y-4 text-center">
      <div class="w-16 h-16 bg-amber-500/20 rounded-2xl flex items-center justify-center mx-auto">
        <svg class="w-8 h-8 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
        </svg>
      </div>
      <h2 class="text-xl font-semibold text-white">Identidad no validada</h2>
      <p class="text-gray-400 text-sm">Para registrar salas, primero debes validar tu identidad.</p>
      <router-link to="/profile/identity" class="btn-primary inline-block mt-2">Validar mi identidad</router-link>
    </div>

    <form v-else @submit.prevent="submit" class="space-y-6">
      <!-- Sede -->
      <div class="card space-y-4">
        <h3 class="text-white font-medium">Datos básicos</h3>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Sede *</label>
          <select v-model="form.venueId" required class="input-field">
            <option value="">Seleccionar sede</option>
            <option v-for="v in venues" :key="v.id" :value="v.id">{{ v.name }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Nombre de la Sala *</label>
          <input v-model="form.name" required class="input-field" placeholder="Ej: Sala A, Sala Grande" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Tipo de sala *</label>
            <select v-model="form.type" required class="input-field">
              <option value="INDIVIDUAL">Individual</option>
              <option value="GRUPAL">Grupal</option>
              <option value="BANDA">Banda</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Capacidad (personas) *</label>
            <input type="number" v-model.number="form.capacity" min="1" required class="input-field" />
          </div>
        </div>
        <div class="grid grid-cols-3 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Tamaño (m²)</label>
            <input type="number" v-model.number="form.tamanoM2" min="1" class="input-field" placeholder="Ej: 30" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Tipo de piso</label>
            <select v-model="form.tipoPiso" class="input-field">
              <option value="">Sin especificar</option>
              <option value="MADERA">Madera</option>
              <option value="FLOTANTE">Flotante</option>
              <option value="CEMENTO">Cemento</option>
              <option value="ALFOMBRA">Alfombra</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Precio / hora ($)</label>
            <input type="number" v-model.number="form.pricePerHour" min="0" class="input-field" placeholder="Ej: 15000" />
          </div>
        </div>
      </div>

      <!-- Requisitos previos: horario de la sede + cuenta MercadoPago -->
      <div v-if="form.venueId && !requisitosOk" class="card space-y-3 border-amber-500/30 bg-amber-500/5">
        <div class="flex items-start gap-3">
          <svg class="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
          </svg>
          <div class="min-w-0">
            <h3 class="text-white font-medium">Antes de registrar una sala</h3>
            <p class="text-gray-400 text-sm mt-0.5">Completa estos requisitos para que tu sala tenga agenda reservable y puedas recibir los pagos.</p>
          </div>
        </div>
        <ul class="space-y-2">
          <li class="flex items-center justify-between gap-3 text-sm">
            <span class="flex items-center gap-2 text-gray-300">
              <span :class="tieneHorario ? 'text-green-400' : 'text-amber-400'">{{ tieneHorario ? '✓' : '○' }}</span>
              Horario de la sede creado
            </span>
            <router-link v-if="!tieneHorario && !verificandoHorario" to="/sede/configuracion" class="text-primary text-sm hover:underline whitespace-nowrap">Crear horario</router-link>
            <span v-else-if="verificandoHorario" class="text-gray-500 text-xs">Verificando...</span>
          </li>
          <li class="flex items-center justify-between gap-3 text-sm">
            <span class="flex items-center gap-2 text-gray-300">
              <span :class="mpConectado ? 'text-green-400' : 'text-amber-400'">{{ mpConectado ? '✓' : '○' }}</span>
              Cuenta de MercadoPago vinculada
            </span>
            <router-link v-if="!mpConectado" to="/sede/configuracion" class="text-primary text-sm hover:underline whitespace-nowrap">Vincular cuenta</router-link>
          </li>
        </ul>
      </div>

      <!-- Equipamiento — espacio -->
      <div class="card space-y-3">
        <h3 class="text-white font-medium">Equipamiento del espacio</h3>
        <div class="grid grid-cols-2 gap-y-2">
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.hasMirrors" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Espejos</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tieneBarraBallet" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Barra de ballet</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tieneAireAcondicionado" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Aire acondicionado</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tieneCalefaccion" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Calefaccion</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tieneInsonorizacion" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Insonorizacion</span>
          </label>
        </div>
      </div>

      <!-- Equipamiento — audio/video -->
      <div class="card space-y-3">
        <h3 class="text-white font-medium">Equipamiento audio / video</h3>
        <div class="grid grid-cols-2 gap-y-2">
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.hasSound" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Sistema de sonido</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tieneAmplificacion" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Amplificacion</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tieneEntradaAuxiliar" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Entrada auxiliar (AUX)</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tieneMicrofono" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Microfono</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tieneEquipoGrabacion" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Equipo de grabacion</span>
          </label>
        </div>
      </div>

      <!-- Instrumentos disponibles -->
      <div class="card space-y-3">
        <h3 class="text-white font-medium">Instrumentos disponibles</h3>
        <div class="grid grid-cols-3 gap-y-2">
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tienePiano" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Piano / Teclado</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tieneGuitarra" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Guitarra</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="form.tieneBateria" class="rounded border-gray-600 bg-[var(--bg-elevated)] text-primary" />
            <span class="text-gray-300 text-sm">Bateria</span>
          </label>
        </div>
      </div>

      <p v-if="msg" :class="msgType === 'success' ? 'text-green-400' : 'text-red-400'" class="text-sm">{{ msg }}</p>
      <button type="submit" :disabled="sending || !requisitosOk" class="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed">
        {{ sending ? 'Registrando...' : 'Registrar Sala' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import venueService from '@/services/venueService'
import scheduleService from '@/services/scheduleService'
import paymentService from '@/services/paymentService'

const router = useRouter()
// identidadValidada se valida UNA sola vez al registrar el usuario.
// Todos los contextos (profesor, sede, alumno) leen el mismo estado desde el store.
const { identidadValidada } = useAuth()

const venues = ref([])
const sending = ref(false)
const msg = ref('')
const msgType = ref('')

// --- Requisitos previos para crear una sala ---
// 1. La sede debe tener su horario creado (de ahí se generan los bloques de
//    disponibilidad). Sin horario, la sala no tendría agenda reservable.
// 2. El gestor debe tener su cuenta de MercadoPago vinculada para recibir los
//    depósitos por el arriendo de la sala.
const tieneHorario = ref(false)
const verificandoHorario = ref(false)
const mpStatus = ref(null)
const mpConectado = computed(() => mpStatus.value?.status === 'CONNECTED' && mpStatus.value?.hasToken)
const requisitosOk = computed(() => !!form.venueId && tieneHorario.value && mpConectado.value)

const form = reactive({
  venueId: '',
  name: '',
  capacity: 10,
  tamanoM2: null,
  tipoPiso: '',
  type: 'GRUPAL',
  pricePerHour: null,
  // Espacio
  hasMirrors: false,
  tieneBarraBallet: false,
  tieneAireAcondicionado: false,
  tieneCalefaccion: false,
  tieneInsonorizacion: false,
  // Audio/video
  hasSound: false,
  tieneAmplificacion: false,
  tieneEntradaAuxiliar: false,
  tieneMicrofono: false,
  tieneEquipoGrabacion: false,
  // Instrumentos
  tienePiano: false,
  tieneGuitarra: false,
  tieneBateria: false
})

onMounted(async () => {
  try {
    venues.value = await venueService.getMyVenues()
    // Preseleccionar la sede si el gestor tiene solo una.
    if (venues.value.length === 1) form.venueId = venues.value[0].id
  } catch { venues.value = [] }
  try { mpStatus.value = await paymentService.getMpAccountStatus() } catch { mpStatus.value = null }
})

// Al elegir/cambiar la sede, verificar si tiene horario configurado.
watch(() => form.venueId, async (venueId) => {
  tieneHorario.value = false
  if (!venueId) return
  verificandoHorario.value = true
  try {
    const horario = await scheduleService.getSchedule(venueId)
    tieneHorario.value = Array.isArray(horario) && horario.length > 0
  } catch { tieneHorario.value = false }
  finally { verificandoHorario.value = false }
}, { immediate: true })

async function submit() {
  if (!requisitosOk.value) {
    msg.value = 'Completa los requisitos previos antes de registrar la sala.'
    msgType.value = 'error'
    return
  }
  sending.value = true
  try {
    await venueService.createRoom(form.venueId, {
      name: form.name,
      capacity: form.capacity,
      tamanoM2: form.tamanoM2,
      tipoPiso: form.tipoPiso || null,
      type: form.type,
      pricePerHour: form.pricePerHour,
      hasMirrors: form.hasMirrors,
      tieneBarraBallet: form.tieneBarraBallet,
      tieneAireAcondicionado: form.tieneAireAcondicionado,
      tieneCalefaccion: form.tieneCalefaccion,
      tieneInsonorizacion: form.tieneInsonorizacion,
      hasSound: form.hasSound,
      tieneAmplificacion: form.tieneAmplificacion,
      tieneEntradaAuxiliar: form.tieneEntradaAuxiliar,
      tieneMicrofono: form.tieneMicrofono,
      tieneEquipoGrabacion: form.tieneEquipoGrabacion,
      tienePiano: form.tienePiano,
      tieneGuitarra: form.tieneGuitarra,
      tieneBateria: form.tieneBateria
    })

    // La sala hereda el horario de la sede: se generan sus bloques de
    // disponibilidad inmediatamente (best-effort; el cron y el calendario de la
    // sede también regeneran si esto fallara).
    try { await scheduleService.generateBlocks() } catch { /* se regenerará luego */ }

    msg.value = 'Sala registrada correctamente'
    msgType.value = 'success'
    setTimeout(() => router.push('/sede/salas'), 1500)
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al registrar sala'
    msgType.value = 'error'
  } finally {
    sending.value = false
  }
}
</script>
