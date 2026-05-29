<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">

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
              <option>CUECA</option><option>BALLET</option><option>DANZA</option>
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
            <option>BASICO</option><option>INTERMEDIO</option><option>AVANZADO</option><option>TODOS</option>
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
          <label class="block text-sm font-medium text-gray-300 mb-1">Descripcion Personal (breve)</label>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'

const router = useRouter()

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

const iniciales = computed(() => {
  const nombre = form.socialName || form.fullName || ''
  return nombre.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || 'M'
})

onMounted(async () => {
  try {
    const res = await api.get('/profesor/perfil')
    if (res.data) {
      Object.assign(form, res.data)
      // Garantizar que los arrays siempre sean arrays
      if (!Array.isArray(form.disciplinasSecundarias)) form.disciplinasSecundarias = []
      if (!Array.isArray(form.tipoFormacion)) form.tipoFormacion = []
    }
  } catch {}
})

async function save() {
  saving.value = true
  msg.value = ''
  try {
    const res = await api.put('/profesor/perfil', {
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
    Object.assign(form, res.data)
    if (!Array.isArray(form.disciplinasSecundarias)) form.disciplinasSecundarias = []
    if (!Array.isArray(form.tipoFormacion)) form.tipoFormacion = []
    msg.value = 'Perfil guardado correctamente'
    msgType.value = 'success'
    setTimeout(() => router.push('/profesor/dashboard'), 1500)
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al guardar'
    msgType.value = 'error'
  }
  saving.value = false
}
</script>
