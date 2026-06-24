<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-white">Registrar Sede</h1>
      <p class="text-gray-400 mt-2">
        Tu solicitud será revisada por el equipo de Modo Ensayo.
        Al ser aprobada, tendrás acceso al panel de gestión de tu sede.
      </p>
    </div>

    <!-- Carga inicial -->
    <div v-if="cargando" class="flex flex-col items-center justify-center py-20">
      <div class="w-10 h-10 border-4 border-primary/30 border-t-primary rounded-full animate-spin mb-4"></div>
      <p class="text-gray-400 text-sm">Cargando formulario...</p>
    </div>

    <!-- Solicitud enviada -->
    <div v-if="enviado" class="card space-y-4 text-center">
      <div class="w-16 h-16 bg-green-500/20 rounded-2xl flex items-center justify-center mx-auto">
        <svg class="w-8 h-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <h2 class="text-xl font-semibold text-white">Solicitud enviada</h2>
      <p class="text-gray-400 text-sm">
        Tu sede ha sido registrada y está en revisión. Te notificaremos cuando sea aprobada.
      </p>
      <router-link to="/alumno/dashboard" class="btn-primary inline-block mt-2">
        Volver al inicio
      </router-link>
    </div>

    <!-- Formulario -->
    <form v-else @submit.prevent="submit" class="space-y-6">

      <!-- Tipo de espacio -->
      <div class="card space-y-3">
        <h2 class="text-lg font-semibold text-white border-b border-white/10 pb-3">Tipo de espacio</h2>
        <div class="grid grid-cols-2 gap-3">
          <button type="button" @click="form.tipo = 'SEDE'"
            :class="['p-4 rounded-xl border-2 text-left transition-all',
              form.tipo === 'SEDE' ? 'border-primary bg-primary/10' : 'border-white/10 hover:border-white/20']">
            <div class="flex items-center gap-2 mb-1">
              <svg class="w-5 h-5" :class="form.tipo === 'SEDE' ? 'text-primary' : 'text-gray-400'"
                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5" />
              </svg>
              <span class="font-semibold" :class="form.tipo === 'SEDE' ? 'text-white' : 'text-gray-300'">Sede</span>
            </div>
            <p class="text-gray-400 text-xs">Academia, escuela o espacio comercial con múltiples salas</p>
          </button>
          <button type="button" @click="form.tipo = 'HOME_STUDIO'"
            :class="['p-4 rounded-xl border-2 text-left transition-all',
              form.tipo === 'HOME_STUDIO' ? 'border-primary bg-primary/10' : 'border-white/10 hover:border-white/20']">
            <div class="flex items-center gap-2 mb-1">
              <svg class="w-5 h-5" :class="form.tipo === 'HOME_STUDIO' ? 'text-primary' : 'text-gray-400'"
                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              </svg>
              <span class="font-semibold" :class="form.tipo === 'HOME_STUDIO' ? 'text-white' : 'text-gray-300'">HomeStudio</span>
            </div>
            <p class="text-gray-400 text-xs">Espacio personal habilitado para clases o ensayos</p>
          </button>
        </div>
      </div>

      <!-- Información básica -->
      <div class="card space-y-4">
        <h2 class="text-lg font-semibold text-white border-b border-white/10 pb-3">Información básica</h2>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">
            Nombre de la {{ form.tipo === 'HOME_STUDIO' ? 'instancia' : 'sede' }} <span class="text-red-400">*</span>
          </label>
          <input v-model="form.name" required class="input-field"
            :placeholder="form.tipo === 'HOME_STUDIO' ? 'ej: HomeStudio La Reina' : 'ej: Academia Musical Crescendo'" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Descripción</label>
          <textarea v-model="form.description" rows="3" class="input-field"
            placeholder="Describe tu espacio: características, ambiente, equipamiento destacado..." />
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              Región <span class="text-red-400">*</span>
            </label>
            <select v-model="form.region" required class="input-field">
              <option value="">Selecciona una región</option>
              <option v-for="r in regiones" :key="r" :value="r">{{ r }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              Comuna <span class="text-red-400">*</span>
            </label>
            <input v-model="form.comuna" required class="input-field" placeholder="ej: Providencia" />
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">
            Dirección <span class="text-red-400">*</span>
          </label>
          <input ref="addressInput" v-model="form.address" required class="input-field" placeholder="ej: Av. Italia 1234" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Referencia o indicaciones de acceso</label>
          <input v-model="form.referencia" class="input-field"
            placeholder="ej: Segundo piso, timbre 3. Metro Santa Isabel." />
        </div>
      </div>

      <!-- Contacto -->
      <div class="card space-y-4">
        <h2 class="text-lg font-semibold text-white border-b border-white/10 pb-3">Contacto</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Teléfono de contacto</label>
            <input type="tel" v-model="form.phone" class="input-field" placeholder="+56 9 XXXX XXXX" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Correo de contacto</label>
            <input type="email" v-model="form.email" class="input-field" placeholder="contacto@misede.cl" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Instagram</label>
            <div class="flex">
              <span class="px-3 py-2 bg-white/5 border border-white/10 border-r-0 rounded-l-lg text-gray-500 text-sm flex items-center">@</span>
              <input type="text" v-model="form.instagram" class="input-field rounded-l-none" placeholder="mi_sede" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Sitio web</label>
            <input type="url" v-model="form.sitioWeb" class="input-field" placeholder="https://misede.cl" />
          </div>
        </div>
      </div>

      <!-- Disciplinas -->
      <div class="card space-y-4">
        <h2 class="text-lg font-semibold text-white border-b border-white/10 pb-3">Disciplinas disponibles</h2>
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
          <label v-for="d in disciplinas" :key="d.value" class="flex items-center gap-2 cursor-pointer group">
            <input type="checkbox" :value="d.value" v-model="form.disciplines"
              class="w-4 h-4 rounded border-gray-600 text-primary bg-gray-800" />
            <span class="text-gray-300 text-sm group-hover:text-white transition-colors">{{ d.label }}</span>
          </label>
        </div>
      </div>

      <!-- Características del espacio -->
      <div class="card space-y-4">
        <h2 class="text-lg font-semibold text-white border-b border-white/10 pb-3">Características del espacio</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Número de salas</label>
            <input type="number" v-model.number="form.cantidadSalas" min="1" class="input-field" placeholder="ej: 3" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Capacidad máxima por sala</label>
            <input type="number" v-model.number="form.capacidadMaxima" min="1" class="input-field" placeholder="ej: 20" />
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-2">Equipamiento disponible</label>
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
            <label v-for="eq in equipamiento" :key="eq.value" class="flex items-center gap-2 cursor-pointer group">
              <input type="checkbox" :value="eq.value" v-model="form.equipamiento"
                class="w-4 h-4 rounded border-gray-600 text-primary bg-gray-800" />
              <span class="text-gray-300 text-sm group-hover:text-white transition-colors">{{ eq.label }}</span>
            </label>
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-2">Características adicionales</label>
          <div class="grid grid-cols-2 gap-2">
            <label v-for="c in caracteristicas" :key="c.value" class="flex items-center gap-2 cursor-pointer group">
              <input type="checkbox" :value="c.value" v-model="form.caracteristicas"
                class="w-4 h-4 rounded border-gray-600 text-primary bg-gray-800" />
              <span class="text-gray-300 text-sm group-hover:text-white transition-colors">{{ c.label }}</span>
            </label>
          </div>
        </div>
      </div>

      <!-- Horario de la sede -->
      <div class="card space-y-4" :class="horarioConfirmado ? 'border border-green-500/40' : 'border border-primary/30'">
        <div class="flex items-center justify-between border-b border-white/10 pb-3">
          <h2 class="text-lg font-semibold text-white">Horario de la {{ form.tipo === 'HOME_STUDIO' ? 'instancia' : 'sede' }} <span class="text-red-400">*</span></h2>
          <span v-if="horarioConfirmado" class="text-xs font-medium text-green-400 flex items-center gap-1">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
            Horario creado
          </span>
        </div>
        <p class="text-xs text-gray-400">
          Marca los días de atención y su rango horario (desde / hasta). Este horario se hereda por <strong>cada sala</strong> que registres y genera automáticamente su calendario de disponibilidad para reservas.
        </p>

        <div class="space-y-2">
          <div v-for="d in scheduleDays" :key="d" class="flex items-center gap-3 flex-wrap">
            <label class="flex items-center gap-2 w-28 cursor-pointer shrink-0">
              <input type="checkbox" v-model="horario[d].enabled" class="w-4 h-4 rounded border-gray-600 text-primary bg-gray-800" />
              <span class="text-sm text-gray-300">{{ dayLabels[d] }}</span>
            </label>
            <div v-if="horario[d].enabled" class="flex items-center gap-2 flex-wrap">
              <span class="text-xs text-gray-500">Desde</span>
              <input type="time" v-model="horario[d].desde" class="input-field !py-1.5 w-28" />
              <span class="text-xs text-gray-500">Hasta</span>
              <input type="time" v-model="horario[d].hasta" class="input-field !py-1.5 w-28" />
            </div>
            <span v-else class="text-xs text-gray-600 italic">Cerrado</span>
          </div>
        </div>

        <div class="flex items-center gap-3 pt-2 border-t border-white/5">
          <label class="text-sm text-gray-300">Duración de cada bloque reservable</label>
          <select v-model.number="duracionBloque" class="input-field !py-1.5 w-32">
            <option :value="30">30 min</option>
            <option :value="45">45 min</option>
            <option :value="60">60 min</option>
            <option :value="90">90 min</option>
            <option :value="120">120 min</option>
          </select>
        </div>

        <p v-if="horarioError" class="text-red-400 text-xs">{{ horarioError }}</p>

        <button type="button" @click="abrirConfirmHorario" class="btn-primary text-sm">
          {{ horarioConfirmado ? 'Actualizar horario' : 'Crear horario' }}
        </button>

        <div v-if="horarioConfirmado" class="text-xs bg-green-500/5 border border-green-500/20 rounded-lg p-3 space-y-0.5">
          <p class="text-green-300 font-medium mb-1">Horario configurado:</p>
          <p v-for="r in horarioResumen" :key="r" class="text-gray-300">· {{ r }}</p>
          <p class="text-gray-500 mt-1">Bloques de {{ duracionBloque }} min.</p>
        </div>
      </div>

      <!-- Documentación requerida -->
      <div class="card space-y-4 border border-primary/20">
        <h2 class="text-lg font-semibold text-white border-b border-white/10 pb-3">Documentacion requerida</h2>
        <p class="text-xs text-gray-400">
          {{ form.tipo === 'SEDE' ? 'Las sedes comerciales requieren documentacion tributaria, permisos municipales y documentos legales.' : 'Para HomeStudio necesitas tu Inicio de Actividades del SII y un comprobante que acredite el domicilio (luz, agua, gas, internet, extracto bancario o ficha de proteccion social).' }}
        </p>

        <div v-for="doc in docsRequeridos" :key="doc.tipo" class="space-y-1.5">
          <!-- Label -->
          <div class="flex items-center gap-2">
            <span class="text-xs font-medium" :class="doc.requerido ? 'text-red-400' : 'text-gray-500'">
              {{ doc.label }}
              <span v-if="doc.requerido" class="text-red-400">*</span>
              <span v-else class="text-gray-600 text-[10px]">(opcional)</span>
            </span>
          </div>

          <!-- Ya guardado desde solicitud anterior -->
          <div v-if="docGuardados.includes(doc.tipo) && !docArchivos[doc.tipo]"
            class="flex items-center gap-2 text-xs text-green-400 bg-green-500/10 border border-green-500/20 rounded-lg px-3 py-2">
            <svg class="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>Archivo guardado de solicitud anterior</span>
            <label class="ml-auto cursor-pointer text-gray-400 hover:text-white underline text-[11px]">
              Reemplazar
              <input type="file" :accept="'.pdf,image/*'" @change="(e) => onDocFile(e, doc.tipo)" class="hidden" />
            </label>
          </div>

          <!-- Subir nuevo archivo -->
          <template v-else>
            <input type="file"
              :accept="'.pdf,image/*'"
              @change="(e) => onDocFile(e, doc.tipo)"
              class="block w-full text-sm text-gray-400
                file:mr-4 file:py-2 file:px-4 file:rounded file:border-0
                file:text-sm file:bg-primary file:text-white
                hover:file:bg-primary/80 file:cursor-pointer file:transition-colors" />
            <div v-if="docArchivos[doc.tipo]" class="text-xs text-green-400 flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              {{ docArchivos[doc.tipo].name }}
            </div>
          </template>
        </div>
      </div>

      <p v-if="error" class="text-red-400 text-sm text-center bg-red-500/10 rounded-lg p-3">{{ error }}</p>

      <button type="submit" :disabled="enviando" class="btn-primary w-full py-3 text-base font-semibold">
        {{ enviando ? 'Enviando solicitud...' : 'Enviar solicitud de registro' }}
      </button>
    </form>

    <!-- Confirmación del horario -->
    <ConfirmDialog
      :show="showHorarioConfirm"
      type="success"
      title="Confirmar horario"
      confirm-text="Sí, crear horario"
      cancel-text="No"
      :message="`¿Estás seguro del horario? ${horarioResumen.join('  ·  ')}. Bloques de ${duracionBloque} min. Se aplicará a todas las salas de tu ${form.tipo === 'HOME_STUDIO' ? 'instancia' : 'sede'}.`"
      @confirm="confirmarHorario"
      @close="showHorarioConfirm = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useAuth } from '@/stores/auth'
import venueService from '@/services/venueService'
import scheduleService from '@/services/scheduleService'
import { supabase } from '@/services/supabase'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { usePlacesAutocomplete } from '@/composables/usePlacesAutocomplete'

const { isAuthenticated } = useAuth()
const { attachAutocomplete } = usePlacesAutocomplete()

const addressInput = ref(null)

const enviando = ref(false)
const enviado = ref(false)
const cargando = ref(true)
const error = ref('')

const disciplinas = [
  { value: 'GUITARRA', label: 'Guitarra' },
  { value: 'BATERIA', label: 'Batería' },
  { value: 'BAJO', label: 'Bajo' },
  { value: 'CANTO', label: 'Canto' },
  { value: 'PIANO', label: 'Piano / Teclado' },
  { value: 'VIOLIN', label: 'Violín' },
  { value: 'SAXOFON', label: 'Saxofón' },
  { value: 'DANZA', label: 'Danza' },
  { value: 'TEATRO', label: 'Teatro' },
  { value: 'PERCUSION', label: 'Percusión' },
  { value: 'OTRO', label: 'Otro' },
]

const equipamiento = [
  { value: 'piano', label: 'Piano / Teclado' },
  { value: 'bateria', label: 'Batería acústica' },
  { value: 'bateria_electronica', label: 'Batería electrónica' },
  { value: 'amplificadores', label: 'Amplificadores' },
  { value: 'micros', label: 'Micrófonos' },
  { value: 'pa_system', label: 'Sistema PA' },
  { value: 'espejos', label: 'Espejos' },
  { value: 'barra_ballet', label: 'Barra de ballet' },
  { value: 'proyector', label: 'Proyector / Pantalla' },
]

const caracteristicas = [
  { value: 'insonorizacion', label: 'Insonorización' },
  { value: 'aire_acondicionado', label: 'Aire acondicionado' },
  { value: 'calefaccion', label: 'Calefacción' },
  { value: 'luz_natural', label: 'Luz natural' },
  { value: 'piso_madera', label: 'Piso de madera' },
  { value: 'piso_flotante', label: 'Piso flotante' },
  { value: 'estacionamiento', label: 'Estacionamiento' },
  { value: 'acceso_discapacitados', label: 'Acceso para discapacitados' },
]

const form = reactive({
  tipo: 'SEDE',
  name: '',
  description: '',
  region: '',
  comuna: '',
  city: '',
  address: '',
  referencia: '',
  phone: '',
  email: '',
  instagram: '',
  sitioWeb: '',
  disciplines: [],
  cantidadSalas: '',
  capacidadMaxima: '',
  equipamiento: [],
  caracteristicas: [],
})

const regiones = [
  'Arica y Parinacota',
  'Tarapacá',
  'Antofagasta',
  'Atacama',
  'Coquimbo',
  'Valparaíso',
  'Metropolitana de Santiago',
  "O'Higgins",
  'Maule',
  'Ñuble',
  'Biobío',
  'La Araucanía',
  'Los Ríos',
  'Los Lagos',
  'Aysén',
  'Magallanes y de la Antártica Chilena',
]

const docArchivos = ref({})
const docGuardados = ref([])  // tipos de documento ya guardados en el servidor

// ── Horario de la sede ──
const scheduleDays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const dayLabels = {
  MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves', FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo'
}
const horario = reactive({
  MONDAY:    { enabled: true,  desde: '08:00', hasta: '22:00' },
  TUESDAY:   { enabled: true,  desde: '08:00', hasta: '22:00' },
  WEDNESDAY: { enabled: true,  desde: '08:00', hasta: '22:00' },
  THURSDAY:  { enabled: true,  desde: '08:00', hasta: '22:00' },
  FRIDAY:    { enabled: true,  desde: '08:00', hasta: '22:00' },
  SATURDAY:  { enabled: false, desde: '09:00', hasta: '18:00' },
  SUNDAY:    { enabled: false, desde: '09:00', hasta: '18:00' },
})
const duracionBloque = ref(60)
const horarioConfirmado = ref(false)
const showHorarioConfirm = ref(false)
const horarioError = ref('')

const horarioResumen = computed(() =>
  scheduleDays.filter(d => horario[d].enabled).map(d => `${dayLabels[d]} ${horario[d].desde}–${horario[d].hasta}`)
)

// Si editan el horario o la duración tras confirmar, se invalida la confirmación.
watch([horario, duracionBloque], () => { horarioConfirmado.value = false }, { deep: true })

function abrirConfirmHorario() {
  horarioError.value = ''
  const activos = scheduleDays.filter(d => horario[d].enabled)
  if (activos.length === 0) { horarioError.value = 'Marca al menos un día de atención.'; return }
  for (const d of activos) {
    if (!horario[d].desde || !horario[d].hasta) { horarioError.value = `Completa el horario de ${dayLabels[d]}.`; return }
    if (horario[d].desde >= horario[d].hasta) { horarioError.value = `En ${dayLabels[d]}, "Desde" debe ser anterior a "Hasta".`; return }
  }
  showHorarioConfirm.value = true
}

function confirmarHorario() {
  showHorarioConfirm.value = false
  horarioConfirmado.value = true
}

// Documentos requeridos segun tipo de sede
const docsRequeridos = computed(() => {
  if (form.tipo === 'SEDE') {
    return [
      { tipo: 'RUT_EMPRESA', label: 'RUT de la Empresa', requerido: true },
      { tipo: 'INICIO_ACTIVIDADES_F4415', label: 'Inicio de Actividades (F4415)', requerido: true },
      { tipo: 'CERTIFICADO_SITUACION_TRIBUTARIA', label: 'Certificado Situación Tributaria', requerido: true },
      { tipo: 'PERMISO_MUNICIPAL', label: 'Permiso Municipal', requerido: true },
      { tipo: 'CONTRATO_ARRIENDO', label: 'Contrato de Arriendo', requerido: false },
      { tipo: 'OTRO', label: 'Otro documento', requerido: false }
    ]
  }
  return [
    { tipo: 'INICIO_ACTIVIDADES_F4415', label: 'Inicio de Actividades (SII)', requerido: true },
    { tipo: 'COMPROBANTE_DOMICILIO', label: 'Comprobante de Domicilio (luz, agua, gas, internet, extracto bancario o ficha proteccion social)', requerido: true },
    { tipo: 'OTRO', label: 'Otro documento', requerido: false }
  ]
})

function onDocFile(event, tipo) {
  const file = event.target.files?.[0]
  if (file) docArchivos.value[tipo] = file
}

onMounted(async () => {
  // Pre-llenar con datos de solicitud rechazada/pendiente si existe
  const solicitud = await venueService.getMiSolicitud()
  if (solicitud?.venue) {
    const v = solicitud.venue
    form.tipo        = v.tipo        || 'SEDE'
    form.name        = v.name        || ''
    form.description = v.description || ''
    form.region      = v.region      || ''
    form.comuna      = v.comuna      || ''
    form.city        = v.city        || ''
    form.address     = v.address     || ''
    form.phone       = v.phone       || ''
    form.email       = v.email       || ''
    form.instagram   = v.instagram   || ''
    form.sitioWeb    = v.sitioWeb    || ''
    docGuardados.value = solicitud.documentosGuardados || []

    // Precargar el horario existente de la sede (si reenvía una solicitud).
    try {
      const sched = await scheduleService.getSchedule(v.id)
      if (sched?.length) {
        for (const d of scheduleDays) horario[d].enabled = false
        for (const s of sched) {
          const dow = s.dayOfWeek || s.day_of_week
          if (horario[dow]) {
            horario[dow].enabled = true
            horario[dow].desde = String(s.openTime || s.open_time || '').slice(0, 5)
            horario[dow].hasta = String(s.closeTime || s.close_time || '').slice(0, 5)
          }
        }
        const cfg = await scheduleService.getBlockConfig(v.id)
        if (cfg?.blockDurationMin) duracionBloque.value = cfg.blockDurationMin
      }
    } catch { /* sin horario previo */ }
  }
  attachAutocomplete(addressInput.value, (place) => {
    form.address = place.formatted_address
    if (place.city && !form.city) form.city = place.city
  })
  cargando.value = false
})

// Sube los documentos adjuntos al bucket privado venue-documents y los indexa.
async function subirDocumentos(venueId) {
  for (const tipo of Object.keys(docArchivos.value)) {
    const file = docArchivos.value[tipo]
    if (!file) continue
    const ext = (file.name.split('.').pop() || 'bin').toLowerCase()
    const path = `${venueId}/${tipo}-${Date.now()}.${ext}`
    const { error: upErr } = await supabase.storage
      .from('venue-documents').upload(path, file, { upsert: true, contentType: file.type })
    if (upErr) throw upErr
    await venueService.addVenueDocument(venueId, {
      fileUrl: path, tipo, nombre: file.name, tipoArchivo: file.type
    })
  }
}

async function submit() {
  error.value = ''
  if (!horarioConfirmado.value) {
    error.value = 'Primero crea y confirma el horario de la sede.'
    return
  }
  enviando.value = true
  try {
    // 1. Crear (o reenviar) la sede vía Edge Function register-venue.
    const venue = await venueService.createVenue({
      name: form.name,
      description: form.description,
      region: form.region,
      comuna: form.comuna,
      // En Chile la ciudad coincide con la comuna; se completa para las búsquedas
      // que filtran/muestran por city.
      city: form.comuna,
      address: form.address,
      phone: form.phone,
      email: form.email,
      tipo: form.tipo,
      instagram: form.instagram,
      sitioWeb: form.sitioWeb,
    })
    if (!venue?.id) throw new Error('No se pudo crear la sede.')

    // 2. Guardar el horario base de la sede (heredado por todas sus salas).
    const schedules = scheduleDays
      .filter(d => horario[d].enabled)
      .map(d => ({ dayOfWeek: d, openTime: horario[d].desde, closeTime: horario[d].hasta }))
    await scheduleService.saveSchedule(venue.id, schedules)
    await scheduleService.saveBlockConfig(venue.id, {
      blockDurationMin: duracionBloque.value, gapBetweenBlocksMin: 0
    })

    // 3. Subir documentos adjuntos (la sede ya existe, así pasa la RLS de Storage).
    await subirDocumentos(venue.id)

    enviado.value = true
  } catch (e) {
    error.value = e.response?.data?.error || e.response?.data?.message || e.message ||
      'Error al enviar la solicitud. Intenta nuevamente.'
  } finally {
    enviando.value = false
  }
}
</script>
