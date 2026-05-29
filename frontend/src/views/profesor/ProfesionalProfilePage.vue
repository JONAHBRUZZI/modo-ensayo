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

    <form @submit.prevent="save" class="card space-y-5">
      <!-- Especialidad y nivel -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Especialidad *</label>
          <select v-model="form.especialidad" required class="input-field">
            <option value="">Seleccionar</option>
            <option>CUECA</option><option>BALLET</option><option>DANZA</option>
            <option>TEATRO</option><option>CANTO</option><option>GUITARRA</option>
            <option>BATERIA</option><option>BAJO</option><option>PIANO</option>
            <option>VIOLIN</option><option>SAXOFON</option><option>OTRO</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Nivel de Ensenanza *</label>
          <select v-model="form.nivelEnsenanza" required class="input-field">
            <option value="">Seleccionar</option>
            <option>BASICO</option><option>INTERMEDIO</option><option>AVANZADO</option><option>TODOS</option>
          </select>
        </div>
      </div>

      <!-- Experiencia -->
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Anos de Experiencia</label>
        <input type="number" v-model.number="form.experienceYears" min="0" max="60" class="input-field" placeholder="Ej: 5" />
      </div>

      <!-- Formacion -->
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Formacion Academica</label>
        <textarea v-model="form.formacion" rows="3" class="input-field" placeholder="Estudios, titulos, certificaciones..."></textarea>
      </div>

      <!-- Descripcion personal -->
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Descripcion Personal</label>
        <textarea v-model="form.description" rows="3" class="input-field" placeholder="Cuentanos sobre ti y tu enfoque como maestro..."></textarea>
      </div>

      <!-- Redes sociales -->
      <div>
        <p class="text-sm font-medium text-gray-300 mb-3">Redes Sociales</p>
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

const form = reactive({
  fullName: '', socialName: '', email: '',
  especialidad: '', nivelEnsenanza: '', experienceYears: null,
  formacion: '', description: '',
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
    if (res.data) Object.assign(form, res.data)
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
      instagram: form.instagram,
      youtube: form.youtube,
      sitioWeb: form.sitioWeb,
      linkedin: form.linkedin
    })
    Object.assign(form, res.data)
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
