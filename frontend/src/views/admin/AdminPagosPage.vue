<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-2">
      <h1 class="text-3xl font-bold text-white">Pagos y giros</h1>
      <router-link to="/admin" class="text-sm text-gray-400 hover:text-white">← Panel</router-link>
    </div>
    <p class="text-gray-500 text-sm mb-8">
      Giros manuales a profesores, reembolsos fallidos y margen real de MercadoPago por ciclo de corte.
    </p>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <template v-else>
      <!-- Recordatorio: giros pendientes -->
      <div class="card border-yellow-500/30 bg-yellow-500/5 mb-8">
        <div class="flex items-center justify-between flex-wrap gap-3">
          <div>
            <h2 class="text-white font-semibold text-lg">
              {{ payoutsFiltrados.length }} giro{{ payoutsFiltrados.length === 1 ? '' : 's' }} pendiente{{ payoutsFiltrados.length === 1 ? '' : 's' }}
              por {{ money(totalPendiente) }}
            </h2>
            <p class="text-gray-400 text-sm mt-1">
              Clases validadas por la sede que quedan por pagar al profesor.
            </p>
          </div>
          <label class="flex items-center gap-2 text-sm text-gray-300">
            <input type="checkbox" v-model="soloCiclo" class="accent-primary" />
            Solo el ciclo seleccionado
          </label>
        </div>
      </div>

      <!-- Ciclo de corte + finanzas -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-10">
        <!-- Selector de ciclo + día de corte -->
        <div class="card">
          <h3 class="text-white font-medium mb-3">Ciclo de corte</h3>
          <label class="block text-xs text-gray-500 mb-1">Mes del corte</label>
          <input type="month" v-model="month" @change="cargarFinanzas" class="input-field mb-4" />

          <label class="block text-xs text-gray-500 mb-1">Día de corte del mes</label>
          <div class="flex gap-2">
            <input type="number" v-model.number="cutoffDay" min="1" max="28" class="input-field" />
            <button @click="guardarCorte" :disabled="guardandoCorte" class="btn-primary text-sm whitespace-nowrap disabled:opacity-50">
              {{ guardandoCorte ? '...' : 'Guardar' }}
            </button>
          </div>
          <p v-if="finance.cicloInicio" class="text-xs text-gray-500 mt-2">
            Ciclo: {{ formatDate(finance.cicloInicio) }} → {{ formatDate(finance.cicloFin) }}
          </p>
        </div>

        <!-- Margen -->
        <div class="card lg:col-span-2">
          <h3 class="text-white font-medium mb-4">Margen del ciclo</h3>
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p class="text-gray-500 text-xs uppercase tracking-wider mb-1">Ingresos</p>
              <p class="text-xl font-bold text-white">{{ money(finance.ingresos) }}</p>
            </div>
            <div>
              <p class="text-gray-500 text-xs uppercase tracking-wider mb-1">Comisión cobrada</p>
              <p class="text-xl font-bold text-purple-400">{{ money(finance.comisionCobrada) }}</p>
            </div>
            <div>
              <p class="text-gray-500 text-xs uppercase tracking-wider mb-1">Costo MercadoPago</p>
              <p class="text-xl font-bold text-red-400">{{ money(finance.costoMp) }}</p>
            </div>
            <div>
              <p class="text-gray-500 text-xs uppercase tracking-wider mb-1">Margen neto</p>
              <p class="text-xl font-bold" :class="(finance.margen || 0) >= 0 ? 'text-green-400' : 'text-red-400'">
                {{ money(finance.margen) }}
              </p>
            </div>
          </div>
          <p class="text-gray-600 text-xs mt-4">
            El costo real de MercadoPago solo se registra en pagos nuevos; los históricos no lo tienen.
            Úsalo para fijar un % de comisión que deje margen.
          </p>
        </div>
      </div>

      <!-- Giros pendientes agrupados por profesor -->
      <h2 class="text-lg font-semibold text-white mb-4">Giros pendientes por profesor</h2>
      <div v-if="porProfesor.length === 0" class="card text-center py-10 mb-10">
        <p class="text-gray-400 text-sm">No hay giros pendientes{{ soloCiclo ? ' en este ciclo' : '' }}.</p>
      </div>
      <div v-else class="space-y-4 mb-10">
        <div v-for="grupo in porProfesor" :key="grupo.teacherId" class="card">
          <div class="flex items-center justify-between mb-3 flex-wrap gap-2">
            <div>
              <h3 class="text-white font-medium">{{ grupo.teacherName || 'Profesor' }}</h3>
              <p class="text-gray-500 text-xs">{{ grupo.teacherEmail || '—' }}</p>
            </div>
            <div class="text-right">
              <p class="text-gray-500 text-xs uppercase tracking-wider">Total a girar</p>
              <p class="text-lg font-bold text-green-400">{{ money(grupo.total) }}</p>
            </div>
          </div>
          <div class="divide-y divide-white/5">
            <div v-for="p in grupo.items" :key="p.id" class="flex items-center justify-between py-2.5 gap-3 flex-wrap">
              <div class="min-w-0">
                <p class="text-sm text-gray-200 truncate">{{ p.classTitle || 'Clase' }}</p>
                <p class="text-xs text-gray-500">
                  {{ p.tipoClase || '—' }}<span v-if="p.venueName"> · {{ p.venueName }}</span> · {{ formatDate(p.createdAt) }}
                </p>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-sm font-medium text-white">{{ money(p.netAmount) }}</span>
                <button @click="pedirMarcarPagado(p)" class="btn-primary text-xs whitespace-nowrap">Marcar pagado</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Reembolsos fallidos -->
      <h2 class="text-lg font-semibold text-white mb-1">Reembolsos fallidos</h2>
      <p class="text-gray-500 text-sm mb-4">Devoluciones que MercadoPago rechazó y requieren atención manual.</p>
      <div v-if="data.failedRefunds.length === 0" class="card text-center py-10">
        <p class="text-gray-400 text-sm">No hay reembolsos fallidos. 🎉</p>
      </div>
      <div v-else class="space-y-3">
        <div v-for="r in data.failedRefunds" :key="r.paymentId" class="card">
          <div class="flex items-start justify-between gap-3 flex-wrap">
            <div class="min-w-0">
              <div class="flex items-center gap-2">
                <h4 class="text-white font-medium text-sm">{{ r.studentName || 'Alumno' }}</h4>
                <EstadoBadge status="FAILED" />
              </div>
              <p class="text-gray-400 text-sm mt-1">{{ r.classTitle || 'Clase' }} · {{ money(r.amount) }}</p>
              <p v-if="r.error" class="text-red-400/80 text-xs mt-1 line-clamp-2">{{ r.error }}</p>
              <p class="text-gray-600 text-xs mt-1">{{ formatDate(r.updatedAt) }}</p>
            </div>
            <div class="flex items-center gap-2 flex-shrink-0">
              <button @click="reintentar(r)" class="btn-secondary text-xs whitespace-nowrap">Reintentar</button>
              <button @click="pedirResolver(r)" class="text-xs text-gray-400 hover:text-white whitespace-nowrap">Marcar resuelto</button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Modal: confirmar giro pagado -->
    <BottomSheet v-model="modalPago.abierto">
      <h3 class="text-lg font-semibold text-white mb-2">Confirmar giro</h3>
      <p class="text-sm text-gray-400 mb-4">
        Vas a marcar como <span class="text-green-400 font-medium">pagado</span> el giro de
        <span class="text-white font-medium">{{ money(modalPago.payout?.netAmount) }}</span>
        a <span class="text-white font-medium">{{ modalPago.payout?.teacherName || 'el profesor' }}</span>.
        Esto no ejecuta la transferencia — solo registra que ya la hiciste.
      </p>
      <label class="block text-xs text-gray-500 mb-1">Referencia (opcional)</label>
      <input type="text" v-model="modalPago.reference" placeholder="N° de transferencia" class="input-field mb-4" />
      <div class="flex justify-end gap-3">
        <button @click="modalPago.abierto = false" class="text-sm text-gray-400 hover:text-white">Cancelar</button>
        <button @click="confirmarPago" :disabled="procesando" class="btn-primary text-sm disabled:opacity-50">
          {{ procesando ? 'Guardando...' : 'Confirmar pago' }}
        </button>
      </div>
    </BottomSheet>

    <!-- Modal: confirmar reembolso resuelto -->
    <BottomSheet v-model="modalResolver.abierto">
      <h3 class="text-lg font-semibold text-white mb-2">Marcar reembolso como resuelto</h3>
      <p class="text-sm text-gray-400 mb-4">
        Confirmas que el reembolso de <span class="text-white font-medium">{{ money(modalResolver.refund?.amount) }}</span>
        a <span class="text-white font-medium">{{ modalResolver.refund?.studentName || 'el alumno' }}</span>
        ya se resolvió por fuera. El pago pasará a <span class="text-blue-400 font-medium">Devuelto</span>.
      </p>
      <div class="flex justify-end gap-3">
        <button @click="modalResolver.abierto = false" class="text-sm text-gray-400 hover:text-white">Cancelar</button>
        <button @click="confirmarResolver" :disabled="procesando" class="btn-primary text-sm disabled:opacity-50">
          {{ procesando ? 'Guardando...' : 'Marcar resuelto' }}
        </button>
      </div>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import adminService from '@/services/adminService'
import EstadoBadge from '@/components/EstadoBadge.vue'
import BottomSheet from '@/components/BottomSheet.vue'
import { useToast } from '@/composables/useToast'
import { formatDate } from '@/utils/dateFormatter'

const toast = useToast()
const loading = ref(true)
const procesando = ref(false)
const guardandoCorte = ref(false)

const data = ref({ payouts: [], failedRefunds: [] })
const finance = ref({})
const soloCiclo = ref(false)

const now = new Date()
const month = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)
const cutoffDay = ref(24)

const modalPago = ref({ abierto: false, payout: null, reference: '' })
const modalResolver = ref({ abierto: false, refund: null })

const money = (v) => '$' + Number(v || 0).toLocaleString('es-CL')

// Filtra los giros al ciclo seleccionado (según la ventana que devuelve finance).
const payoutsFiltrados = computed(() => {
  const payouts = data.value.payouts || []
  if (!soloCiclo.value || !finance.value.cicloInicio) return payouts
  const ini = new Date(finance.value.cicloInicio).getTime()
  const fin = new Date(finance.value.cicloFin).getTime()
  return payouts.filter((p) => {
    const t = new Date(p.createdAt).getTime()
    return t > ini && t <= fin
  })
})

const totalPendiente = computed(() =>
  payoutsFiltrados.value.reduce((a, p) => a + Number(p.netAmount || 0), 0)
)

// Agrupa los giros filtrados por profesor, con total por profesor.
const porProfesor = computed(() => {
  const map = new Map()
  for (const p of payoutsFiltrados.value) {
    if (!map.has(p.teacherId)) {
      map.set(p.teacherId, { teacherId: p.teacherId, teacherName: p.teacherName, teacherEmail: p.teacherEmail, items: [], total: 0 })
    }
    const g = map.get(p.teacherId)
    g.items.push(p)
    g.total += Number(p.netAmount || 0)
  }
  return [...map.values()].sort((a, b) => b.total - a.total)
})

async function cargarPagos() {
  try {
    const res = await adminService.getPayments()
    data.value = { payouts: res?.payouts || [], failedRefunds: res?.failedRefunds || [] }
  } catch (e) {
    toast.error(e?.message || 'No se pudieron cargar los pagos.')
    data.value = { payouts: [], failedRefunds: [] }
  }
}

async function cargarFinanzas() {
  try {
    finance.value = await adminService.getFinance(month.value)
    if (finance.value?.cutoffDay) cutoffDay.value = finance.value.cutoffDay
  } catch { finance.value = {} }
}

async function guardarCorte() {
  if (!(cutoffDay.value >= 1 && cutoffDay.value <= 28)) {
    toast.error('El día de corte debe estar entre 1 y 28.')
    return
  }
  guardandoCorte.value = true
  try {
    await adminService.updateSetting('payout_cutoff_day', cutoffDay.value)
    toast.success('Día de corte actualizado.')
    await cargarFinanzas()
  } catch (e) {
    toast.error(e?.message || 'No se pudo guardar el día de corte.')
  } finally {
    guardandoCorte.value = false
  }
}

function pedirMarcarPagado(payout) {
  modalPago.value = { abierto: true, payout, reference: '' }
}

async function confirmarPago() {
  procesando.value = true
  try {
    await adminService.markPayoutPaid(modalPago.value.payout.id, modalPago.value.reference?.trim() || undefined)
    modalPago.value.abierto = false
    toast.success('Giro marcado como pagado.')
    await Promise.all([cargarPagos(), cargarFinanzas()])
  } catch (e) {
    toast.error(e?.message || 'No se pudo marcar el giro.')
  } finally {
    procesando.value = false
  }
}

async function reintentar(refund) {
  procesando.value = true
  try {
    await adminService.retryRefund(refund.paymentId)
    toast.success('Reembolso reencolado para reintento.')
    await cargarPagos()
  } catch (e) {
    toast.error(e?.message || 'No se pudo reintentar el reembolso.')
  } finally {
    procesando.value = false
  }
}

function pedirResolver(refund) {
  modalResolver.value = { abierto: true, refund }
}

async function confirmarResolver() {
  procesando.value = true
  try {
    await adminService.markRefundResolved(modalResolver.value.refund.paymentId)
    modalResolver.value.abierto = false
    toast.success('Reembolso marcado como resuelto.')
    await cargarPagos()
  } catch (e) {
    toast.error(e?.message || 'No se pudo marcar el reembolso.')
  } finally {
    procesando.value = false
  }
}

onMounted(async () => {
  await Promise.all([cargarPagos(), cargarFinanzas()])
  loading.value = false
})
</script>
