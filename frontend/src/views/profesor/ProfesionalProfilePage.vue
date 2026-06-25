<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">

    <!-- Banner de bienvenida cuando llega desde reserva (primera vez como Maestro) -->
    <div v-if="primeraVez" class="mb-8 rounded-2xl border border-yellow-500/40 bg-gradient-to-r from-yellow-500/10 to-primary/10 p-6">
      <div class="flex items-start gap-4">
        <div class="w-12 h-12 bg-yellow-500/20 rounded-full flex items-center justify-center flex-shrink-0">
          <svg class="w-6 h-6 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
          </svg>
        </div>
        <div class="flex-1">
          <h2 class="text-white font-bold text-lg">¡Felicitaciones, ya eres Maestro!</h2>
          <p class="text-gray-300 text-sm mt-1">
            Tu sala está reservada. Antes de configurar tu clase, completa tu <strong class="text-white">perfil profesional</strong>
            para que los alumnos te conozcan y puedan inscribirse.
          </p>
          <p class="text-yellow-300 text-xs mt-2 font-medium">
            Mínimo requerido: tu biografía y disciplina principal.
          </p>
        </div>
      </div>
    </div>

    <!-- Banner persistente: perfil incompleto (no es primera vez) -->
    <div v-else-if="!perfilProfesionalCompleto && hasTeacherRole" class="mb-6 rounded-xl border border-blue-500/30 bg-blue-500/5 p-4 flex items-start gap-3">
      <svg class="w-5 h-5 text-blue-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      <p class="text-blue-200 text-sm">
        Tu perfil aún no esta completo. Completa al menos tu biografía y disciplina principal para que tus clases aparezcan a los alumnos.
      </p>
    </div>

    <!-- Cabecera con nombre del maestro -->
    <div class="flex items-center space-x-4 mb-8">
      <div class="w-16 h-16 bg-primary rounded-full flex items-center justify-center text-2xl font-bold text-white flex-shrink-0">
        {{ iniciales }}
      </div>
      <div>
        <h1 class="text-2xl font-bold text-white">{{ form.socialName || form.fullName || 'Perfil Profesional' }}</h1>
        <p class="text-gray-400 text-sm">{{ form.email }}</p>
        <div v-if="form.averageRating" class="flex items-center mt-1">
          <span class="text-yellow-400 text-sm">★ {{ form.averageRating?.toFixed(1) }}</span>
        </div>
      </div>
    </div>

    <!-- Conexión MercadoPago (requerida para publicar clases y cobrar) -->
    <div class="card mb-8">
      <h3 class="text-sm font-semibold text-primary uppercase tracking-wider mb-3">Cobros — MercadoPago</h3>
      <div v-if="mp.connected" class="flex items-center gap-3 flex-wrap">
        <span class="inline-flex items-center gap-2 text-green-400 text-sm font-medium">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
          Cuenta conectada
        </span>
        <span class="text-gray-500 text-xs">Recibirás tus pagos automáticamente al confirmarse cada clase.</span>
      </div>
      <div v-else class="space-y-3">
        <p class="text-gray-300 text-sm">
          Conecta tu cuenta de MercadoPago para poder <strong class="text-white">publicar clases</strong> y recibir tus pagos.
        </p>
        <button type="button" @click="conectarMp" :disabled="mpLoading" class="btn-primary">
          {{ mpLoading ? 'Redirigiendo...' : 'Conectar con MercadoPago' }}
        </button>
      </div>
      <p v-if="mpMsg" :class="mpMsgType === 'success' ? 'text-green-400' : 'text-red-400'" class="text-xs mt-2">{{ mpMsg }}</p>
    </div>

    <h2 class="text-xl font-semibold text-white mb-2">Datos Profesionales</h2>
    <p class="text-gray-400 text-sm mb-6">Esta informacion es visible para alumnos y sedes que buscan maestros.</p>

    <form @submit.prevent="save" class="space-y-6">

      <!-- Sección: Disciplina -->
      <div class="card space-y-5">
        <h3 class="text-sm font-semibold text-primary uppercase tracking-wider">Disciplina</h3>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Especialidad Principal *</label>
            <select v-model="form.especialidad" required class="input-field">
              <option value="">Seleccionar</option>
              <option value="CUECA">Cueca</option><option value="BALLET">Ballet</option><option value="DANZA">Danza</option>
              <option>TEATRO</option><option>CANTO</option><option>GUITARRA</option>
              <option>BATERIA</option><option>BAJO</option><option>PIANO</option>
              <option>VIOLIN</option><option>SAXOFON</option><option>OTRO</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Disciplina Principal (libre)</label>
            <input v-model="form.disciplinaPrincipal" class="input-field" placeholder="Ej: Danza Contemporánea" />
          </div>
        </div>

        <!-- Disciplinas secundarias -->
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-2">Disciplinas Secundarias</label>
          <div class="grid grid-cols-3 gap-2">
            <label v-for="d in disciplinasOpciones" :key="d"
                   class="flex items-center space-x-2 cursor-pointer text-sm text-gray-300">
              <input type="checkbox" :value="d" v-model="form.disciplinasSecundarias"
                     class="rounded border-gray-600 bg-dark-card text-primary" />
              <span>{{ d }}</span>
            </label>
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Nivel de Ensenanza *</label>
          <select v-model="form.nivelEnsenanza" required class="input-field">
            <option value="">Seleccionar</option>
            <option value="BASICO">Básico</option><option value="INTERMEDIO">Intermedio</option><option value="AVANZADO">Avanzado</option><option value="TODOS">Todos los niveles</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Anos de Experiencia</label>
          <input type="number" v-model.number="form.experienceYears" min="0" max="60" class="input-field" placeholder="Ej: 5" />
        </div>
      </div>

      <!-- Sección: Presentación -->
      <div class="card space-y-5">
        <h3 class="text-sm font-semibold text-primary uppercase tracking-wider">Presentacion</h3>

        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Biografia</label>
          <textarea v-model="form.biografia" rows="4" class="input-field"
            placeholder="Presentate: quién eres, tu enfoque como maestro, lo que te hace único..."></textarea>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Descripción personal (breve)</label>
          <textarea v-model="form.description" rows="2" class="input-field"
            placeholder="Resumen corto para listados de búsqueda..."></textarea>
        </div>
      </div>

      <!-- Sección: Formacion -->
      <div class="card space-y-5">
        <h3 class="text-sm font-semibold text-primary uppercase tracking-wider">Formacion</h3>

        <!-- Tipo de formación -->
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-2">Tipo de Formacion</label>
          <div class="grid grid-cols-2 gap-2">
            <label v-for="t in tipoFormacionOpciones" :key="t"
                   class="flex items-center space-x-2 cursor-pointer text-sm text-gray-300">
              <input type="checkbox" :value="t" v-model="form.tipoFormacion"
                     class="rounded border-gray-600 bg-dark-card text-primary" />
              <span>{{ t }}</span>
            </label>
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Formacion Academica (resumen)</label>
          <textarea v-model="form.formacion" rows="2" class="input-field"
            placeholder="Títulos, instituciones, certificaciones..."></textarea>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Detalle de Formacion</label>
          <textarea v-model="form.detalleFormacion" rows="3" class="input-field"
            placeholder="Describe en detalle tu trayectoria académica y profesional..."></textarea>
        </div>
      </div>

      <!-- Sección: Redes sociales -->
      <div class="card space-y-4">
        <h3 class="text-sm font-semibold text-primary uppercase tracking-wider">Redes Sociales</h3>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-xs text-gray-400 mb-1">Instagram</label>
            <input v-model="form.instagram" class="input-field" placeholder="@usuario" />
          </div>
          <div>
            <label class="block text-xs text-gray-400 mb-1">YouTube</label>
            <input v-model="form.youtube" class="input-field" placeholder="URL del canal" />
          </div>
          <div>
            <label class="block text-xs text-gray-400 mb-1">Sitio Web</label>
            <input v-model="form.sitioWeb" class="input-field" placeholder="https://" />
          </div>
          <div>
            <label class="block text-xs text-gray-400 mb-1">LinkedIn</label>
            <input v-model="form.linkedin" class="input-field" placeholder="URL del perfil" />
          </div>
        </div>
      </div>

      <p v-if="msg" :class="msgType === 'success' ? 'text-green-400' : 'text-red-400'" class="text-sm">{{ msg }}</p>

      <div class="flex space-x-3 pt-2">
        <button type="submit" :disabled="saving" class="btn-primary flex-1">
          {{ saving ? 'Guardando...' : 'Guardar Perfil' }}
        </button>
        <router-link to="/profesor/dashboard" class="btn-secondary flex-1 text-center">
          Cancelar
        </router-link>
      </div>
    </form>

    <!-- Reseñas recibidas -->
    <div class="card space-y-4 mt-8">
      <div class="flex items-center justify-between">
        <h2 class="text-sm font-medium text-gray-500 uppercase tracking-wider">Reseñas recibidas</h2>
        <span v-if="reviews.length" class="text-yellow-400 text-sm font-semibold">
          ★ {{ promedioReviews.toFixed(1) }} <span class="text-gray-500 font-normal">({{ reviews.length }})</span>
        </span>
      </div>
      <div v-if="reviews.length === 0" class="text-gray-500 text-sm">
        Aún no tienes reseñas. Aparecerán aquí cuando tus alumnos evalúen tus clases.
      </div>
      <div v-else class="space-y-3">
        <div v-for="r in reviews" :key="r.id" class="border-t border-white/5 pt-3">
          <div class="flex items-center justify-between mb-1">
            <span class="text-white text-sm font-medium">{{ r.authorName || 'Alumno' }}</span>
            <span class="text-yellow-400 text-sm">{{ '★'.repeat(r.score) }}{{ '☆'.repeat(5 - r.score) }}</span>
          </div>
          <p v-if="r.comment" class="text-gray-400 text-sm">{{ r.comment }}</p>
          <p class="text-gray-600 text-xs mt-1">{{ formatDate(r.createdAt) }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import professionalProfileService from '@/services/professionalProfileService'
import sellerService from '@/services/sellerService'
import { useAuth } from '@/stores/auth'
import { reviewService } from '@/services/reviewService'
import { formatDate } from '@/utils/dateFormatter'

const router = useRouter()
const route = useRoute()
const { user, syncAtributos, perfilProfesionalCompleto, puedeVerContextoProfesor } = useAuth()

const primeraVez = computed(() => route.query.primeraVez === 'true')
const hasTeacherRole = computed(() => puedeVerContextoProfesor.value)

const disciplinasOpciones = [
  'CUECA', 'BALLET', 'DANZA', 'TEATRO', 'CANTO',
  'GUITARRA', 'BATERIA', 'BAJO', 'PIANO', 'VIOLIN', 'SAXOFON', 'OTRO'
]

const tipoFormacionOpciones = [
  'Autodidacta', 'Taller', 'Curso', 'Tecnico', 'Universitario', 'Posgrado', 'Conservatorio'
]

const form = reactive({
  fullName: '', socialName: '', email: '',
  especialidad: '', nivelEnsenanza: '', experienceYears: null,
  formacion: '', description: '',
  biografia: '', disciplinaPrincipal: '',
  disciplinasSecundarias: [],
  tipoFormacion: [],
  detalleFormacion: '',
  instagram: '', youtube: '', sitioWeb: '', linkedin: '',
  photoUrl: null, averageRating: null
})

const saving = ref(false)
const msg = ref('')
const msgType = ref('')
const reviews = ref([])

// Conexión MercadoPago (vendedor)
const mp = ref({ connected: false, status: 'DISCONNECTED' })
const mpLoading = ref(false)
const mpMsg = ref('')
const mpMsgType = ref('')

async function conectarMp() {
  mpLoading.value = true
  mpMsg.value = ''
  try {
    await sellerService.connect() // redirige a MercadoPago
  } catch (e) {
    mpLoading.value = false
    mpMsg.value = e?.response?.data?.error || e?.message || 'No se pudo iniciar la conexión'
    mpMsgType.value = 'error'
  }
}

const promedioReviews = computed(() => {
  if (!reviews.value.length) return 0
  return reviews.value.reduce((sum, r) => sum + r.score, 0) / reviews.value.length
})


const iniciales = computed(() => {
  const nombre = form.socialName || form.fullName || ''
  return nombre.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || 'M'
})

onMounted(async () => {
  // Resultado del retorno desde el OAuth de MercadoPago (?mp=connected|error|expired)
  const mpResult = route.query.mp
  if (mpResult === 'connected') {
    mpMsg.value = '¡Cuenta de MercadoPago conectada!'
    mpMsgType.value = 'success'
  } else if (mpResult === 'error' || mpResult === 'expired') {
    mpMsg.value = 'No se pudo conectar MercadoPago. Inténtalo de nuevo.'
    mpMsgType.value = 'error'
  }
  try {
    mp.value = await sellerService.getStatus()
  } catch (err) {
    console.error('Error al cargar estado de MercadoPago', err)
  }

  try {
    const data = await professionalProfileService.getMine()
    if (data) {
      Object.assign(form, data)
      if (!Array.isArray(form.disciplinasSecundarias)) form.disciplinasSecundarias = []
      if (!Array.isArray(form.tipoFormacion)) form.tipoFormacion = []
      // Cargar reseñas del profesor usando su ID de usuario (= teacherId de las clases).
      // No usar res.data.id: ese es el id del perfil profesional, no el del usuario.
      const teacherId = user.value?.id
      if (teacherId) {
        const rev = await reviewService.getByTeacher(teacherId).then(r => r.data).catch(() => [])
        reviews.value = Array.isArray(rev) ? rev : []
      }
    }
  } catch (err) {
    console.error('Error al cargar perfil profesional', err)
  }
})

async function save() {
  saving.value = true
  msg.value = ''
  try {
    const data = await professionalProfileService.save({
      especialidad: form.especialidad,
      nivelEnsenanza: form.nivelEnsenanza,
      experienceYears: form.experienceYears,
      formacion: form.formacion,
      description: form.description,
      biografia: form.biografia,
      disciplinaPrincipal: form.disciplinaPrincipal,
      disciplinasSecundarias: form.disciplinasSecundarias,
      tipoFormacion: form.tipoFormacion,
      detalleFormacion: form.detalleFormacion,
      instagram: form.instagram,
      youtube: form.youtube,
      sitioWeb: form.sitioWeb,
      linkedin: form.linkedin
    })
    Object.assign(form, data)
    if (!Array.isArray(form.disciplinasSecundarias)) form.disciplinasSecundarias = []
    if (!Array.isArray(form.tipoFormacion)) form.tipoFormacion = []
    // Refrescar atributos (perfilProfesionalCompleto puede haber cambiado)
    try { await syncAtributos() } catch (err) {
      console.error('Error al sincronizar atributos del perfil', err)
    }
    msg.value = primeraVez.value
      ? 'Perfil guardado. Te llevamos a configurar tu clase...'
      : 'Perfil guardado correctamente'
    msgType.value = 'success'
    // Si vino desde la reserva (primera vez), llevarlo a configurar la clase
    const destino = primeraVez.value ? '/profesor/clases-por-asignar' : '/profesor/dashboard'
    setTimeout(() => router.push(destino), 1500)
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al guardar'
    msgType.value = 'error'
  }
  saving.value = false
}
</script>
