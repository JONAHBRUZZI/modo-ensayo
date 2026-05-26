<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-white">Registrar Sede</h1>
      <p class="text-gray-400 mt-2">
        Tu solicitud será revisada por el equipo de Modo Ensayo.
        Al ser aprobada, tendrás acceso al panel de gestión de tu sede.
      </p>
    </div>

    <!-- Identidad no validada -->
    <div v-if="!identidadValidada && !identidadEnRevision" class="card space-y-4 text-center">
      <div class="w-16 h-16 bg-amber-500/20 rounded-2xl flex items-center justify-center mx-auto">
        <svg class="w-8 h-8 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>
      <h2 class="text-xl font-semibold text-white">Identidad no validada</h2>
      <p class="text-gray-400 text-sm">
        Para registrar una sede debes validar tu identidad subiendo tu cédula o pasaporte.
      </p>
      <router-link to="/profile/identity" class="btn-primary inline-block mt-2">
        Validar mi identidad
      </router-link>
    </div>

    <!-- Identidad en revisión -->
    <div v-else-if="identidadEnRevision" class="card space-y-4 text-center">
      <div class="w-16 h-16 bg-blue-500/20 rounded-2xl flex items-center justify-center mx-auto">
        <svg class="w-8 h-8 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <h2 class="text-xl font-semibold text-white">Verificación en revisión</h2>
      <p class="text-gray-400 text-sm">
        Tu documento está siendo revisado. Podrás registrar tu sede una vez aprobada la verificación.
      </p>
    </div>

    <!-- Solicitud enviada -->
    <div v-else-if="enviado" class="card space-y-4 text-center">
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
              Ciudad / Comuna <span class="text-red-400">*</span>
            </label>
            <input v-model="form.city" required class="input-field" placeholder="ej: Santiago, Providencia" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              Dirección <span class="text-red-400">*</span>
            </label>
            <input v-model="form.address" required class="input-field" placeholder="ej: Av. Italia 1234" />
          </div>
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

      <!-- Documentación requerida -->
      <div class="card space-y-3 border border-amber-500/20 bg-amber-500/5">
        <div class="flex items-start gap-3">
          <svg class="w-5 h-5 text-amber-400 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div>
            <h3 class="text-sm font-semibold text-amber-300">Documentación requerida para la aprobación</h3>
            <p class="text-xs text-amber-200/70 mt-1">
              El equipo de Modo Ensayo revisará tu solicitud y podrá contactarte para solicitar:
            </p>
            <ul class="text-xs text-amber-200/70 mt-2 space-y-1 list-disc list-inside">
              <li>Fotografías del espacio (salas, accesos, equipamiento)</li>
              <li v-if="form.tipo === 'SEDE'">Documentación legal del local o contrato de arriendo</li>
              <li>Confirmación de que el espacio cumple condiciones de habitabilidad</li>
            </ul>
          </div>
        </div>
      </div>

      <p v-if="error" class="text-red-400 text-sm text-center bg-red-500/10 rounded-lg p-3">{{ error }}</p>

      <button type="submit" :disabled="enviando" class="btn-primary w-full py-3 text-base font-semibold">
        {{ enviando ? 'Enviando solicitud...' : 'Enviar solicitud de registro' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuth } from '@/stores/auth'
import venueService from '@/services/venueService'

const { isAuthenticated, identidadValidada, identidadEnRevision, syncIdentityStatus } = useAuth()

const enviando = ref(false)
const enviado = ref(false)
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

onMounted(() => {
  if (isAuthenticated.value) syncIdentityStatus()
})

async function submit() {
  enviando.value = true
  error.value = ''
  try {
    await venueService.createVenue({
      tipo: form.tipo,
      name: form.name,
      description: form.description,
      city: form.city,
      address: form.address,
      referencia: form.referencia,
      phone: form.phone,
      email: form.email,
      instagram: form.instagram ? `@${form.instagram}` : null,
      sitioWeb: form.sitioWeb || null,
      disciplines: form.disciplines,
      cantidadSalas: form.cantidadSalas || null,
      capacidadMaxima: form.capacidadMaxima || null,
      equipamiento: form.equipamiento,
      caracteristicas: form.caracteristicas,
    })
    enviado.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al enviar la solicitud. Intenta nuevamente.'
  } finally {
    enviando.value = false
  }
}
</script>
