<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Mis Clases</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="clases.length === 0" class="card text-center py-12">
      <p class="text-gray-400">No tienes clases inscritas.</p>
      <router-link to="/classes" class="btn-primary mt-4 inline-block">Buscar Clases</router-link>
    </div>
    <div v-else class="space-y-8">
      <div v-for="(grupo, nombre) in clasesAgrupadas" :key="nombre" class="space-y-3">
        <h2 class="text-lg font-semibold text-gray-300 border-b border-white/10 pb-2">{{ nombre }}</h2>
        <div v-for="c in grupo" :key="c.enrollmentId || c.classId" class="card">
          <div class="flex items-center justify-between">
            <router-link :to="'/alumno/clases/' + c.classId" class="flex-1 min-w-0 mr-4">
              <h3 class="text-lg font-semibold text-white hover:text-primary transition-colors">{{ c.title }}</h3>
              <p class="text-gray-400 text-sm">{{ c.discipline }} {{ c.level ? '— ' + c.level : '' }}</p>
              <p class="text-gray-500 text-xs mt-1">{{ formatDate(c.startTime) }}</p>
            </router-link>
            <div class="flex items-center gap-3 flex-shrink-0">
              <span class="text-primary font-semibold text-sm">${{ c.price?.toLocaleString('es-CL') }}</span>
              <EstadoBadge :status="c.enrollmentStatus || c.status" />
              <button v-if="puedeCanc(c)" @click.prevent="iniciarCancelacion(c)" class="text-xs text-red-400 border border-red-400/30 rounded px-2 py-1 hover:bg-red-400/10 transition-colors">Cancelar</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal de confirmación de cancelación -->
    <div v-if="modalCancelar" class="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4">
      <div class="card max-w-md w-full space-y-4">
        <h3 class="text-xl font-bold text-white">Cancelar inscripcion</h3>
        <p class="text-gray-400 text-sm">
          ¿Confirmas la cancelacion de <span class="text-white font-medium">{{ claseSeleccionada?.title }}</span>?
        </p>
        <div class="bg-yellow-500/10 border border-yellow-500/30 rounded p-3">
          <p class="text-yellow-400 text-sm">
            Si tienes un pago asociado, se iniciará el proceso de reembolso de forma manual.
            El tiempo puede variar según el banco.
          </p>
        </div>
        <p v-if="errorCancelar" class="text-red-400 text-sm">{{ errorCancelar }}</p>
        <div class="flex gap-3">
          <button @click="confirmarCancelacion" :disabled="cancelando"
                  class="btn-primary flex-1 bg-red-600 hover:bg-red-700">
            {{ cancelando ? 'Cancelando...' : 'Sí, cancelar inscripcion' }}
          </button>
          <button @click="modalCancelar = false" class="btn-secondary flex-1">
            No, mantener
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import paymentService from '@/services/paymentService'
import api from '@/services/api'
import EstadoBadge from '@/components/EstadoBadge.vue'

const clases = ref([])
const loading = ref(true)
const modalCancelar = ref(false)
const claseSeleccionada = ref(null)
const cancelando = ref(false)
const errorCancelar = ref('')

const clasesAgrupadas = computed(() => {
  const grupos = {}
  for (const c of clases.value) {
    const nombre = c.beneficiaryName || 'Yo'
    if (!grupos[nombre]) grupos[nombre] = []
    grupos[nombre].push(c)
  }
  return grupos
})

onMounted(async () => {
  await cargarClases()
})

async function cargarClases() {
  loading.value = true
  try {
    const data = await paymentService.getMyEnrollments()
    clases.value = Array.isArray(data) ? data : []
  } catch {
    clases.value = []
  } finally {
    loading.value = false
  }
}

function puedeCanc(c) {
  const enrollStatus = c.enrollmentStatus || 'ACTIVE'
  if (enrollStatus === 'CANCELLED') return false
  if (!c.startTime) return true
  return new Date(c.startTime) > new Date()
}

function iniciarCancelacion(clase) {
  claseSeleccionada.value = clase
  errorCancelar.value = ''
  modalCancelar.value = true
}

async function confirmarCancelacion() {
  if (!claseSeleccionada.value?.enrollmentId) return
  cancelando.value = true
  errorCancelar.value = ''
  try {
    await api.post(`/payments/enrollments/${claseSeleccionada.value.enrollmentId}/cancel`)
    modalCancelar.value = false
    await cargarClases()
  } catch (e) {
    errorCancelar.value = e.response?.data?.message || 'Error al cancelar la inscripcion'
  } finally {
    cancelando.value = false
  }
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
}
</script>
