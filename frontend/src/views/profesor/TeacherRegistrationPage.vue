<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-white">Registro de Perfil Profesional</h1>
      <p class="text-gray-400 mt-2">
        Este perfil identifica tu trayectoria como maestro en la plataforma.
        Una vez registrado podrás reservar salas y crear clases.
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
        Para crear un perfil profesional primero debes validar tu identidad.
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
        Tu documento está siendo revisado. Te notificaremos cuando sea aprobado.
      </p>
    </div>

    <!-- Perfil guardado -->
    <div v-else-if="guardado" class="card space-y-4 text-center">
      <div class="w-16 h-16 bg-green-500/20 rounded-2xl flex items-center justify-center mx-auto">
        <svg class="w-8 h-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <h2 class="text-xl font-semibold text-white">Perfil profesional registrado</h2>
      <p class="text-gray-400 text-sm">
        Tu perfil está listo. Ahora puedes buscar salas y crear tu primera clase.
      </p>
      <router-link to="/profesor/buscar-salas" class="btn-primary inline-block mt-2">
        Buscar salas disponibles
      </router-link>
    </div>

    <!-- Formulario -->
    <form v-else @submit.prevent="submit" class="space-y-6">

      <!-- Disciplinas -->
      <div class="card space-y-4">
        <h2 class="text-lg font-semibold text-white border-b border-white/10 pb-3">Disciplinas</h2>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-2">
            Disciplina principal <span class="text-red-400">*</span>
          </label>
          <select v-model="form.disciplinaPrincipal" required class="input-field">
            <option value="">Seleccionar disciplina</option>
            <option v-for="d in disciplinas" :key="d.value" :value="d.value">{{ d.label }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-2">Disciplinas secundarias</label>
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
            <label v-for="d in disciplinas" :key="d.value" class="flex items-center gap-2 cursor-pointer group">
              <input type="checkbox" :value="d.value" v-model="form.disciplinasSecundarias"
                class="w-4 h-4 rounded border-gray-600 text-primary bg-gray-800" />
              <span class="text-gray-300 text-sm group-hover:text-white transition-colors">{{ d.label }}</span>
            </label>
          </div>
        </div>
      </div>

      <!-- Experiencia -->
      <div class="card space-y-4">
        <h2 class="text-lg font-semibold text-white border-b border-white/10 pb-3">Experiencia</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              Años de experiencia <span class="text-red-400">*</span>
            </label>
            <input type="number" v-model.number="form.anosExperiencia" min="0" max="60" required
              class="input-field" placeholder="ej: 5" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Nivel de enseñanza</label>
            <select v-model="form.nivelEnsenanza" class="input-field">
              <option value="">Seleccionar</option>
              <option value="BASICO">Básico</option>
              <option value="INTERMEDIO">Intermedio</option>
              <option value="AVANZADO">Avanzado</option>
              <option value="TODOS">Todos los niveles</option>
            </select>
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Formación y certificaciones</label>
          <textarea v-model="form.formacion" rows="3" class="input-field"
            placeholder="ej: Licenciatura en Música, U. de Chile. Certificación ABRSM grado 8 piano." />
        </div>
      </div>

      <!-- Presentación -->
      <div class="card space-y-4">
        <h2 class="text-lg font-semibold text-white border-b border-white/10 pb-3">Presentación</h2>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">
            Biografía profesional <span class="text-red-400">*</span>
          </label>
          <textarea v-model="form.bio" rows="5" required maxlength="500" class="input-field"
            placeholder="Cuéntanos sobre tu trayectoria, estilo de enseñanza y lo que hace única tu clase." />
          <p class="text-xs mt-1" :class="form.bio.length > 450 ? 'text-amber-400' : 'text-gray-500'">
            {{ form.bio.length }}/500 caracteres
          </p>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Especialidad o enfoque</label>
          <input type="text" v-model="form.especialidad" class="input-field"
            placeholder="ej: Técnica Suzuki, Jazz contemporáneo, Danza clásica para adultos" />
        </div>
      </div>

      <!-- Redes profesionales -->
      <div class="card space-y-4">
        <h2 class="text-lg font-semibold text-white border-b border-white/10 pb-3">
          Redes profesionales
          <span class="text-gray-500 text-sm font-normal ml-1">(opcional)</span>
        </h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Instagram</label>
            <div class="flex">
              <span class="px-3 py-2 bg-white/5 border border-white/10 border-r-0 rounded-l-lg text-gray-500 text-sm flex items-center">@</span>
              <input type="text" v-model="form.instagram" class="input-field rounded-l-none" placeholder="tu_usuario" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">YouTube</label>
            <input type="url" v-model="form.youtube" class="input-field" placeholder="https://youtube.com/@canal" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Sitio web / Portfolio</label>
            <input type="url" v-model="form.sitioWeb" class="input-field" placeholder="https://mipagina.com" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">LinkedIn</label>
            <input type="url" v-model="form.linkedin" class="input-field" placeholder="https://linkedin.com/in/perfil" />
          </div>
        </div>
      </div>

      <p v-if="error" class="text-red-400 text-sm text-center bg-red-500/10 rounded-lg p-3">{{ error }}</p>

      <button type="submit" :disabled="enviando" class="btn-primary w-full py-3 text-base font-semibold">
        {{ enviando ? 'Guardando perfil...' : 'Registrar perfil profesional' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuth } from '@/stores/auth'
import professionalProfileService from '@/services/professionalProfileService'

const { identidadValidada, identidadEnRevision, syncIdentityStatus } = useAuth()

const enviando = ref(false)
const guardado = ref(false)
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

const form = reactive({
  disciplinaPrincipal: '',
  disciplinasSecundarias: [],
  anosExperiencia: '',
  nivelEnsenanza: '',
  formacion: '',
  bio: '',
  especialidad: '',
  instagram: '',
  youtube: '',
  sitioWeb: '',
  linkedin: '',
})

onMounted(() => syncIdentityStatus())

async function submit() {
  enviando.value = true
  error.value = ''
  try {
    await professionalProfileService.save({
      disciplinaPrincipal: form.disciplinaPrincipal,
      disciplinasSecundarias: form.disciplinasSecundarias,
      experienceYears: form.anosExperiencia,
      nivelEnsenanza: form.nivelEnsenanza,
      formacion: form.formacion,
      description: form.bio,
      especialidad: form.especialidad,
      instagram: form.instagram ? `@${form.instagram}` : null,
      youtube: form.youtube || null,
      sitioWeb: form.sitioWeb || null,
      linkedin: form.linkedin || null,
    })
    guardado.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Error al guardar el perfil. Intenta nuevamente.'
  } finally {
    enviando.value = false
  }
}
</script>
