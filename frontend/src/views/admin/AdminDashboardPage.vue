<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Panel de Administración</h1>

    <!-- Stats cards -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-6 mb-10">
      <router-link to="/admin/usuarios" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Usuarios</h3>
        <p class="text-3xl font-bold text-white group-hover:text-primary">{{ stats.usuarios || 0 }}</p>
      </router-link>
      <router-link to="/admin/sedes" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Sedes</h3>
        <p class="text-3xl font-bold text-primary">{{ stats.sedes || 0 }}</p>
      </router-link>
      <router-link to="/admin/roles" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Verif. Pendientes</h3>
        <p class="text-3xl font-bold text-yellow-400">{{ stats.pendientes || 0 }}</p>
      </router-link>
      <router-link to="/admin/sedes" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-2">Sedes Pend.</h3>
        <p class="text-3xl font-bold text-red-400">{{ stats.sedesPendientes || 0 }}</p>
      </router-link>
    </div>

    <!-- Alerta: giros pendientes -->
    <router-link
      v-if="pagosPendientes.count > 0"
      to="/admin/pagos"
      class="card border-yellow-500/40 bg-yellow-500/5 flex items-center justify-between mb-10 hover:border-yellow-500/70 transition-colors group"
    >
      <div class="flex items-center gap-3">
        <span class="text-2xl">💸</span>
        <div>
          <p class="text-white font-semibold">
            {{ pagosPendientes.count }} giro{{ pagosPendientes.count === 1 ? '' : 's' }} pendiente{{ pagosPendientes.count === 1 ? '' : 's' }}
            por {{ money(pagosPendientes.total) }}
          </p>
          <p class="text-gray-400 text-sm">Clases validadas por pagar a profesores.</p>
        </div>
      </div>
      <span class="text-yellow-400 text-sm group-hover:translate-x-0.5 transition-transform">Gestionar →</span>
    </router-link>

    <!-- Charts grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-10">
      <!-- Usuarios por rol (Pie) -->
      <div class="card flex flex-col items-center">
        <h3 class="text-white font-medium mb-4 self-start">Usuarios por Rol</h3>
        <div class="w-64 h-64">
          <Pie :data="chartUsuariosRol" :options="pieOptions" />
        </div>
      </div>

      <!-- Sedes por estado (Bar) -->
      <div class="card flex flex-col items-center">
        <h3 class="text-white font-medium mb-4 self-start">Sedes por Estado</h3>
        <div class="w-full h-64">
          <Bar :data="chartSedesEstado" :options="barOptions" />
        </div>
      </div>

      <!-- Clases (Bar) -->
      <div class="card flex flex-col items-center">
        <h3 class="text-white font-medium mb-4 self-start">Clases</h3>
        <div class="w-full h-64">
          <Bar :data="chartClases" :options="clasesOptions" />
        </div>
      </div>

      <!-- Ingresos por fuente (Line, 3 series) -->
      <div class="card flex flex-col">
        <div class="flex items-center justify-between mb-4 w-full">
          <h3 class="text-white font-medium">Ingresos por fuente</h3>
          <div class="inline-flex rounded-lg border border-white/10 overflow-hidden text-xs">
            <button @click="setGranularidad('month')" :class="granularidad === 'month' ? 'bg-primary text-white' : 'text-gray-400 hover:text-white'" class="px-3 py-1 transition-colors">Mensual</button>
            <button @click="setGranularidad('year')" :class="granularidad === 'year' ? 'bg-primary text-white' : 'text-gray-400 hover:text-white'" class="px-3 py-1 transition-colors">Anual</button>
          </div>
        </div>
        <div v-if="chartIngresos.labels.length > 0" class="w-full h-64">
          <Line :data="chartIngresos" :options="lineOptions" />
        </div>
        <p v-else class="text-gray-500 text-sm text-center py-16">Sin datos de ingresos aun</p>
      </div>
    </div>

    <!-- KPIs de ingresos por fuente -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-10">
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Ingresos Clases</h3>
        <p class="text-2xl font-bold text-purple-400">${{ totalesIngreso.clases.toLocaleString('es-CL') }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Ingresos Arriendos</h3>
        <p class="text-2xl font-bold text-blue-400">${{ totalesIngreso.arriendos.toLocaleString('es-CL') }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Ingresos Total</h3>
        <p class="text-2xl font-bold text-green-400">${{ totalesIngreso.total.toLocaleString('es-CL') }}</p>
      </div>
    </div>

    <!-- Margen / costo MercadoPago del ciclo -->
    <div class="flex items-center justify-between mb-1 mt-6">
      <h2 class="text-lg font-semibold text-white">Margen · Costo MercadoPago</h2>
      <router-link to="/admin/pagos" class="text-xs text-primary hover:underline">Ver detalle →</router-link>
    </div>
    <p class="text-gray-500 text-sm mb-4">Ciclo de corte actual. Ayuda a fijar un % de comisión que deje margen.</p>
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-10">
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Ingresos</h3>
        <p class="text-2xl font-bold text-white">{{ money(finance.ingresos) }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Comisión cobrada</h3>
        <p class="text-2xl font-bold text-purple-400">{{ money(finance.comisionCobrada) }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Costo MercadoPago</h3>
        <p class="text-2xl font-bold text-red-400">{{ money(finance.costoMp) }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Margen neto</h3>
        <p class="text-2xl font-bold" :class="(finance.margen || 0) >= 0 ? 'text-green-400' : 'text-red-400'">
          {{ money(finance.margen) }}
        </p>
      </div>
    </div>

    <!-- Comisiones de la plataforma -->
    <h2 class="text-lg font-semibold text-white mb-1 mt-6">Comisiones de la plataforma</h2>
    <p class="text-gray-500 text-sm mb-4">Aplican solo a transacciones futuras. Cada pago guarda su propia comisión al procesarse.</p>
    <div class="card grid grid-cols-1 md:grid-cols-2 gap-6 mb-10">
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Comisión arriendo de salas (%)</label>
        <div class="flex gap-3">
          <input type="number" v-model.number="comisiones.arriendo" min="0" max="100" class="input-field" />
          <button @click="pedirGuardar('room_reservation_commission_pct', comisiones.arriendo)" class="btn-primary text-sm whitespace-nowrap">Guardar</button>
        </div>
        <p class="text-xs text-gray-500 mt-1">Actual: {{ comisionesActuales.room_reservation_commission_pct ?? '—' }}%</p>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Comisión clases (%)</label>
        <div class="flex gap-3">
          <input type="number" v-model.number="comisiones.clases" min="0" max="100" class="input-field" />
          <button @click="pedirGuardar('marketplace_commission_pct', comisiones.clases)" class="btn-primary text-sm whitespace-nowrap">Guardar</button>
        </div>
        <p class="text-xs text-gray-500 mt-1">Actual: {{ comisionesActuales.marketplace_commission_pct ?? '—' }}%</p>
      </div>
    </div>

    <!-- Modal de confirmación de cambio de comisión -->
    <BottomSheet v-model="modalComision.abierto">
      <h3 class="text-lg font-semibold text-white mb-2">Confirmar cambio de comisión</h3>
      <p class="text-sm text-gray-400 mb-4">
        Vas a cambiar la comisión de <span class="text-white font-medium">{{ modalComision.label }}</span> de
        <span class="text-white font-medium">{{ modalComision.actual }}%</span> →
        <span class="text-primary font-medium">{{ modalComision.nuevo }}%</span>.
        Aplica solo a transacciones futuras.
      </p>
      <div class="flex justify-end gap-3">
        <button @click="modalComision.abierto = false" class="text-sm text-gray-400 hover:text-white">Cancelar</button>
        <button @click="confirmarGuardar" :disabled="guardandoComision" class="btn-primary text-sm disabled:opacity-50">
          {{ guardandoComision ? 'Guardando...' : 'Confirmar' }}
        </button>
      </div>
    </BottomSheet>

    <!-- Métricas de rendimiento (clicleables → desglose por sede) -->
    <h2 class="text-lg font-semibold text-white mb-1 mt-6">Métricas de Rendimiento</h2>
    <p class="text-gray-500 text-sm mb-4">Toca cualquier métrica para ver su explicación y el desglose por sede.</p>
    <div class="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
      <button
        v-for="m in metricasCfg"
        :key="m.key"
        @click="abrirMetrica(m.key)"
        class="card text-center hover:border-primary/50 transition-colors cursor-pointer"
      >
        <h3 class="text-gray-400 text-xs mb-1">{{ m.titulo }}</h3>
        <p class="text-3xl font-bold mt-1" :class="colorMetrica(valorGlobal(m.key), m.objetivo)">
          {{ fmtPct(valorGlobal(m.key)) }}
        </p>
        <p class="text-xs text-gray-500 mt-1">objetivo &gt; {{ m.objetivo }}%</p>
      </button>
    </div>

    <!-- Modal: detalle de la métrica + desglose por sede -->
    <BottomSheet v-model="modalMetrica.abierto">
      <template v-if="modalMetrica.cfg">
        <h3 class="text-lg font-semibold text-white mb-1">{{ modalMetrica.cfg.titulo }}</h3>
        <p class="text-gray-400 text-sm mb-4">{{ modalMetrica.cfg.explicacion }}</p>

        <!-- Valor global -->
        <div class="flex items-center justify-between card mb-4">
          <span class="text-gray-300 text-sm">Global · Modo Ensayo</span>
          <span class="text-2xl font-bold"
                :class="colorMetrica(valorGlobal(modalMetrica.key), modalMetrica.cfg.objetivo)">
            {{ fmtPct(valorGlobal(modalMetrica.key)) }}
          </span>
        </div>

        <!-- M4: infraestructura global, sin desglose por sede -->
        <div v-if="modalMetrica.cfg.soloGlobal" class="text-sm text-gray-400">
          <p>
            La disponibilidad es infraestructura <span class="text-white">global</span>: la plataforma
            es la misma para todas las sedes, así que no se divide por sede. Se mide con un latido
            interno cada 5 minutos; el porcentaje son los latidos registrados vs. los esperados.
          </p>
        </div>

        <!-- M1/M2/M3/M5: desglose por sede -->
        <template v-else>
          <h4 class="text-white font-medium text-sm mb-2">Por sede</h4>
          <div v-if="sedesMetrica.length === 0" class="text-gray-500 text-sm py-6 text-center">
            Aún no hay datos de esta métrica por sede.
          </div>
          <div v-else class="divide-y divide-white/5 max-h-72 overflow-y-auto">
            <div v-for="s in sedesMetrica" :key="s.venueId" class="flex items-center justify-between py-2.5 gap-3">
              <span class="text-gray-300 text-sm truncate">{{ s.venueName }}</span>
              <span class="text-sm font-bold flex-shrink-0"
                    :class="colorMetrica(s[modalMetrica.key], modalMetrica.cfg.objetivo)">
                {{ fmtPct(s[modalMetrica.key]) }}
              </span>
            </div>
          </div>
        </template>
      </template>
    </BottomSheet>

    <!-- Comportamiento · Google Analytics -->
    <div class="flex items-center justify-between mb-1 mt-10">
      <h2 class="text-lg font-semibold text-white">Comportamiento · Google Analytics</h2>
      <span v-if="analytics.configured" class="flex items-center gap-1.5 text-xs text-gray-400">
        <span class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></span>
        {{ analytics.activeNow }} activo{{ analytics.activeNow === 1 ? '' : 's' }} ahora
      </span>
    </div>
    <p class="text-gray-500 text-sm mb-4">Uso real de la plataforma (últimos 7 días). Se actualiza cada 30s.</p>

    <div v-if="!analytics.configured" class="card text-gray-400 text-sm mb-10">
      Google Analytics aún no está conectado. Falta definir <code class="text-gray-300">GA_PROPERTY_ID</code> y
      <code class="text-gray-300">GA_SERVICE_ACCOUNT</code> (secretos de Edge Function) y
      <code class="text-gray-300">VITE_GA_MEASUREMENT_ID</code> (frontend).
    </div>
    <template v-else>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="card">
          <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Activos ahora</h3>
          <p class="text-2xl font-bold text-green-400">{{ analytics.activeNow }}</p>
        </div>
        <div class="card">
          <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Usuarios (7d)</h3>
          <p class="text-2xl font-bold text-white">{{ analytics.usuarios7d }}</p>
        </div>
        <div class="card">
          <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Sesiones (7d)</h3>
          <p class="text-2xl font-bold text-blue-400">{{ analytics.sesiones7d }}</p>
        </div>
        <div class="card">
          <h3 class="text-gray-500 text-xs uppercase tracking-wider mb-1">Vistas (7d)</h3>
          <p class="text-2xl font-bold text-purple-400">{{ analytics.vistas7d }}</p>
        </div>
      </div>
      <div v-if="analytics.topPages && analytics.topPages.length" class="card mb-10">
        <h3 class="text-white font-medium mb-3">Páginas más vistas (7 días)</h3>
        <div class="divide-y divide-white/5">
          <div v-for="p in analytics.topPages" :key="p.path" class="flex items-center justify-between py-2 gap-3">
            <span class="text-gray-300 text-sm truncate">{{ p.path }}</span>
            <span class="text-gray-400 text-sm flex-shrink-0">{{ p.views }} vistas</span>
          </div>
        </div>
      </div>
    </template>

    <!-- ════ Feedback de la plataforma (Voz del usuario) ════ -->
    <div class="flex items-center justify-between mb-1 mt-10">
      <h2 class="text-lg font-semibold text-white">Voz del Usuario · Modo Ensayo</h2>
      <span class="text-xs text-gray-500">Lo que opina la comunidad de la plataforma</span>
    </div>
    <p class="text-gray-500 text-sm mb-4">Datos para confirmar la intuitividad y priorizar mejoras.</p>

    <div v-if="(sysStats.total || 0) === 0" class="card text-center py-8">
      <p class="text-gray-400 text-sm">Aún no hay valoraciones de la plataforma.</p>
    </div>

    <template v-else>
      <!-- KPIs -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="card">
          <h3 class="text-gray-400 text-xs uppercase tracking-wider mb-1">Satisfacción global</h3>
          <p class="text-3xl font-bold text-yellow-400">★ {{ (sysStats.promedio || 0).toFixed(1) }}</p>
          <p class="text-xs text-gray-500 mt-1">{{ sysStats.total }} valoraciones</p>
        </div>
        <div class="card">
          <h3 class="text-gray-400 text-xs uppercase tracking-wider mb-1">% Satisfechos</h3>
          <p class="text-3xl font-bold" :class="(sysStats.satisfaccion||0) >= 70 ? 'text-green-400' : 'text-yellow-400'">
            {{ sysStats.satisfaccion || 0 }}%
          </p>
          <p class="text-xs text-gray-500 mt-1">califican 4★ o 5★</p>
        </div>
        <div class="card">
          <h3 class="text-gray-400 text-xs uppercase tracking-wider mb-1">% Detractores</h3>
          <p class="text-3xl font-bold" :class="(sysStats.detractores||0) <= 15 ? 'text-green-400' : 'text-red-400'">
            {{ sysStats.detractores || 0 }}%
          </p>
          <p class="text-xs text-gray-500 mt-1">califican 1★ o 2★</p>
        </div>
        <div class="card">
          <h3 class="text-gray-400 text-xs uppercase tracking-wider mb-1">Participación</h3>
          <p class="text-3xl font-bold text-primary">{{ sysStats.participacion || 0 }}%</p>
          <p class="text-xs text-gray-500 mt-1">{{ sysStats.total }} de {{ sysStats.totalUsuarios }} usuarios</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <!-- Distribución por estrella -->
        <div class="card">
          <h3 class="text-white font-medium mb-4">Distribución de puntuaciones</h3>
          <div class="space-y-2">
            <div v-for="n in [5,4,3,2,1]" :key="n" class="flex items-center gap-3">
              <span class="text-yellow-400 text-xs w-8 flex-shrink-0">{{ n }}★</span>
              <div class="flex-1 bg-[var(--bg-elevated)] rounded-full h-3 overflow-hidden">
                <div class="h-full rounded-full transition-all"
                     :class="n >= 4 ? 'bg-green-500' : n === 3 ? 'bg-yellow-500' : 'bg-red-500'"
                     :style="{ width: barPct(n) + '%' }"></div>
              </div>
              <span class="text-gray-400 text-xs w-8 text-right flex-shrink-0">{{ distCount(n) }}</span>
            </div>
          </div>
        </div>

        <!-- Desglose por perfil -->
        <div class="card">
          <h3 class="text-white font-medium mb-1">¿Quién lo encuentra más intuitivo?</h3>
          <p class="text-gray-500 text-xs mb-4">Promedio por tipo de usuario</p>
          <div class="space-y-3">
            <div v-for="(d, rol) in (sysStats.porRol || {})" :key="rol" class="flex items-center gap-3">
              <span class="text-gray-300 text-sm w-20 flex-shrink-0">{{ rol }}</span>
              <div class="flex-1 bg-[var(--bg-elevated)] rounded-full h-3 overflow-hidden">
                <div class="h-full rounded-full bg-primary transition-all" :style="{ width: ((d.promedio || 0) / 5 * 100) + '%' }"></div>
              </div>
              <span class="text-white text-sm w-16 text-right flex-shrink-0">
                {{ d.total ? '★ ' + d.promedio.toFixed(1) : '—' }}
              </span>
            </div>
          </div>
          <p class="text-gray-600 text-xs mt-4">El perfil con menor promedio señala dónde enfocar mejoras de usabilidad.</p>
        </div>
      </div>

      <!-- Comentarios (feedback cualitativo) -->
      <h3 class="text-white font-medium mb-3">Comentarios recientes</h3>
      <div class="space-y-3 max-h-96 overflow-y-auto pr-1">
        <div v-for="r in systemReviews" :key="r.id" class="card">
          <div class="flex items-start justify-between gap-3">
            <h4 class="text-white font-medium text-sm">{{ r.authorName || 'Usuario' }}</h4>
            <span class="text-yellow-400 text-sm flex-shrink-0">{{ estrellas(r.score) }}</span>
          </div>
          <p v-if="r.comment" class="text-gray-400 text-sm mt-2">{{ r.comment }}</p>
          <p class="text-gray-600 text-xs mt-2">{{ formatDate(r.createdAt) }}</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import adminService from '@/services/adminService'
import { reviewService } from '@/services/reviewService'
import { formatDate } from '@/utils/dateFormatter'
import BottomSheet from '@/components/BottomSheet.vue'
import { useToast } from '@/composables/useToast'
import { Pie, Bar, Line } from 'vue-chartjs'
import {
  Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale,
  LinearScale, BarElement, PointElement, LineElement, Filler
} from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, PointElement, LineElement, Filler)

const toast = useToast()
const stats = ref({})
const systemReviews = ref([])
const sysStats = ref({})
const granularidad = ref('month')

// Resumen de pagos: giros pendientes (alerta) y margen del ciclo actual.
const pagosPendientes = ref({ count: 0, total: 0 })
const finance = ref({})
const money = (v) => '$' + Number(v || 0).toLocaleString('es-CL')

// Métricas de rendimiento (global + por sede) y modal de detalle.
const metrics = ref({ global: {}, porSede: [] })
const modalMetrica = ref({ abierto: false, key: null, cfg: null })

const metricasCfg = [
  { key: 'ocupacion', titulo: 'M1 · Ocupación', objetivo: 80,
    explicacion: 'De la capacidad física de las salas donde se dictan las clases, qué porcentaje se llenó con alumnos inscritos. Se mide contra el aforo de la sala (no el cupo de la clase). Un número alto significa que las salas se aprovechan bien.' },
  { key: 'conversion', titulo: 'M2 · Conversión', objetivo: 70,
    explicacion: 'De cada checkout que un alumno inició, cuántos terminaron en un pago aprobado. Los que abandonan el carrito sin pagar bajan el número. Mide si el precio y el flujo de compra convencen.' },
  { key: 'asistencia', titulo: 'M3 · Asistencia', objetivo: 90,
    explicacion: 'De todos los alumnos inscritos a los que el profesor pasó lista, cuántos estuvieron presentes. Se mide sobre los inscritos, no sobre la capacidad de la sala. Un número alto significa buen compromiso de los alumnos.' },
  { key: 'disponibilidad', titulo: 'M4 · Disponibilidad', objetivo: 95, soloGlobal: true,
    explicacion: 'Qué porcentaje del tiempo la plataforma estuvo en línea. Se mide con un latido interno cada 5 minutos: si el sistema se cae, el latido no se registra y ese hueco baja el porcentaje. Es global (la app es la misma para todas las sedes).' },
  { key: 'pagosExitosos', titulo: 'M5 · Pagos Exitosos', objetivo: 98,
    explicacion: 'De los intentos de pago que llegaron a resolverse (excluye los abandonados), cuántos fueron exitosos. Un número bajo indica una falla técnica en el cobro (MercadoPago o el checkout), no falta de interés.' },
]

const fmtPct = (v) => (v == null ? '—' : v + '%')
const valorGlobal = (key) => metrics.value.global?.[key]
const colorMetrica = (v, objetivo) => (v == null ? 'text-gray-500' : (v >= objetivo ? 'text-green-400' : 'text-yellow-400'))

// Sedes con dato para la métrica abierta (oculta las que no tienen denominador).
const sedesMetrica = computed(() => {
  const key = modalMetrica.value.key
  if (!key) return []
  return (metrics.value.porSede || []).filter((s) => s[key] != null)
})

function abrirMetrica(key) {
  modalMetrica.value = { abierto: true, key, cfg: metricasCfg.find((m) => m.key === key) }
}

// Comisiones editables + estado del modal de confirmación.
const comisiones = ref({ arriendo: 0, clases: 0 })
const comisionesActuales = ref({})
const guardandoComision = ref(false)
const modalComision = ref({ abierto: false, key: null, label: '', actual: 0, nuevo: 0 })

const estrellas = (s) => { const n = Math.round(s || 0); return '★'.repeat(n) + '☆'.repeat(5 - n) }
const distCount = (n) => (sysStats.value.distribucion?.[n]) || 0
const barPct = (n) => {
  const dist = sysStats.value.distribucion || {}
  const max = Math.max(1, ...Object.values(dist).map(Number))
  return Math.round((distCount(n) / max) * 100)
}

const chartUsuariosRol = computed(() => ({
  labels: Object.keys(stats.value.usuariosPorRol || {}),
  datasets: [{
    data: Object.values(stats.value.usuariosPorRol || {}),
    backgroundColor: ['#ef4444', '#3b82f6', '#10b981', '#8b5cf6', '#f59e0b', '#ec4899']
  }]
}))

const chartSedesEstado = computed(() => ({
  labels: Object.keys(stats.value.sedesPorEstado || {}).map(s => s.replace(/_/g, ' ')),
  datasets: [{
    label: 'Sedes',
    data: Object.values(stats.value.sedesPorEstado || {}),
    backgroundColor: ['#10b981', '#f59e0b', '#ef4444', '#f97316']
  }]
}))

const chartClases = computed(() => ({
  labels: ['Total', 'Realizadas'],
  datasets: [{
    label: 'Clases',
    data: [stats.value.totalClases || 0, stats.value.clasesRealizadas || 0],
    backgroundColor: ['#8b5cf6', '#10b981']
  }]
}))

const chartIngresos = computed(() => {
  const serie = stats.value.ingresosPorPeriodo || []
  return {
    labels: serie.map(i => i.periodo || ''),
    datasets: [
      { label: 'Clases', data: serie.map(i => i.clases || 0), borderColor: '#8b5cf6', backgroundColor: 'rgba(139,92,246,0.15)', fill: false, tension: 0.3 },
      { label: 'Arriendos', data: serie.map(i => i.arriendos || 0), borderColor: '#3b82f6', backgroundColor: 'rgba(59,130,246,0.15)', fill: false, tension: 0.3 },
      { label: 'Total', data: serie.map(i => i.total || 0), borderColor: '#10b981', backgroundColor: 'rgba(16,185,129,0.15)', fill: true, tension: 0.3 }
    ]
  }
})

// Totales acumulados por fuente (KPIs).
const totalesIngreso = computed(() => (stats.value.ingresosPorPeriodo || []).reduce((a, i) => {
  a.clases += Number(i.clases || 0); a.arriendos += Number(i.arriendos || 0); a.total += Number(i.total || 0)
  return a
}, { clases: 0, arriendos: 0, total: 0 }))

const pieOptions = { plugins: { legend: { labels: { color: '#9ca3af' } } } }
const barOptions = { plugins: { legend: { display: false } }, scales: { x: { ticks: { color: '#9ca3af' } }, y: { ticks: { color: '#9ca3af' } } } }
const clasesOptions = { plugins: { legend: { display: false } }, scales: { x: { ticks: { color: '#9ca3af' } }, y: { ticks: { color: '#9ca3af' } } } }
const lineOptions = { plugins: { legend: { display: true, labels: { color: '#9ca3af' } } }, scales: { x: { ticks: { color: '#9ca3af' } }, y: { ticks: { color: '#9ca3af' } } } }

async function cargarStats() {
  try { stats.value = await adminService.getStats(granularidad.value) } catch { stats.value = {} }
}

function setGranularidad(g) {
  if (granularidad.value === g) return
  granularidad.value = g
  cargarStats()
}

async function cargarComisiones() {
  try {
    const s = await adminService.getSettings()
    comisionesActuales.value = s
    comisiones.value.arriendo = s.room_reservation_commission_pct ?? 0
    comisiones.value.clases = s.marketplace_commission_pct ?? 0
  } catch { /* sin permiso / sin datos */ }
}

// Abre el modal de confirmación mostrando valor actual → nuevo.
function pedirGuardar(key, nuevo) {
  const labels = { room_reservation_commission_pct: 'arriendo de salas', marketplace_commission_pct: 'clases' }
  modalComision.value = {
    abierto: true,
    key,
    label: labels[key] || key,
    actual: comisionesActuales.value[key] ?? 0,
    nuevo: Number(nuevo)
  }
}

async function confirmarGuardar() {
  const { key, nuevo } = modalComision.value
  guardandoComision.value = true
  try {
    await adminService.updateSetting(key, nuevo)
    comisionesActuales.value = { ...comisionesActuales.value, [key]: nuevo }
    modalComision.value.abierto = false
    toast.success('Comisión actualizada.')
  } catch (e) {
    toast.error(e?.message || 'No se pudo actualizar la comisión.')
  } finally {
    guardandoComision.value = false
  }
}

async function cargarPagos() {
  try {
    const p = await adminService.getPayments()
    const payouts = p?.payouts || []
    pagosPendientes.value = { count: payouts.length, total: payouts.reduce((a, x) => a + Number(x.netAmount || 0), 0) }
  } catch { /* sin permiso / sin datos */ }
  try { finance.value = await adminService.getFinance() } catch { finance.value = {} }
}

async function cargarMetrics() {
  try { metrics.value = await adminService.getMetrics() } catch { metrics.value = { global: {}, porSede: [] } }
}

// Comportamiento (Google Analytics): se refresca cada 30s (los usuarios activos
// en vivo cambian lento; 30s da tiempo real sin gastar cuota de la API de GA).
const analytics = ref({ configured: false })
let analyticsTimer = null
async function cargarAnalytics() {
  try { analytics.value = await adminService.getAnalytics() } catch { analytics.value = { configured: false } }
}

onMounted(async () => {
  // Todas las tarjetas del dashboard son independientes entre sí y cada una ya
  // maneja su propio error internamente — se cargan en paralelo en vez de en
  // cascada secuencial.
  await Promise.allSettled([
    cargarStats(),
    cargarComisiones(),
    cargarPagos(),
    cargarMetrics(),
    cargarAnalytics(),
    (async () => {
      try { systemReviews.value = (await reviewService.getSystemReviews()).data || [] } catch { systemReviews.value = [] }
    })(),
    (async () => {
      try { sysStats.value = (await reviewService.getSystemStats()).data || {} } catch { sysStats.value = {} }
    })()
  ])
  analyticsTimer = setInterval(cargarAnalytics, 30000)
})

onUnmounted(() => { if (analyticsTimer) clearInterval(analyticsTimer) })
</script>
