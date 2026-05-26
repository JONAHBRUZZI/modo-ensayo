<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-2">Registro de Perfil Profesional</h1>
    <p class="text-gray-400 mb-8">Completa tu perfil como Maestro para que alumnos y sedes te conozcan.</p>

    <form @submit.prevent="save" class="card space-y-4">
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
          <option>BASICO</option><option>INTERMEDIO</option><option>AVANZADO</option>
          <option>TODOS</option>
        </select>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Anos de Experiencia</label>
        <input type="number" v-model="form.experienceYears" min="0" class="input-field" placeholder="Ej: 5" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Formacion Academica</label>
        <textarea v-model="form.formacion" rows="3" class="input-field" placeholder="Estudios, titulos, certificaciones..."></textarea>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Descripcion Personal</label>
        <textarea v-model="form.description" rows="3" class="input-field" placeholder="Cuentanos sobre ti y tu enfoque como maestro..."></textarea>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Instagram</label>
          <input v-model="form.instagram" class="input-field" placeholder="@usuario" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">YouTube</label>
          <input v-model="form.youtube" class="input-field" placeholder="URL del canal" />
        </div>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Sitio Web</label>
          <input v-model="form.sitioWeb" class="input-field" placeholder="https://" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">LinkedIn</label>
          <input v-model="form.linkedin" class="input-field" placeholder="URL del perfil" />
        </div>
      </div>
      <p v-if="msg" :class="msgType === 'success' ? 'text-green-400' : 'text-red-400'" class="text-sm">{{ msg }}</p>
      <button type="submit" :disabled="saving" class="btn-primary w-full">{{ saving ? 'Guardando...' : 'Completar Registro Profesional' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'

const router = useRouter()
const form = reactive({ especialidad: '', nivelEnsenanza: '', experienceYears: null, formacion: '', description: '', instagram: '', youtube: '', sitioWeb: '', linkedin: '' })
const saving = ref(false)
const msg = ref('')
const msgType = ref('')

onMounted(async () => {
  try {
    const res = await api.get('/users/me/professional-profile')
    if (res.data && res.status !== 204) {
      Object.assign(form, res.data)
    }
  } catch {}
})

async function save() {
  saving.value = true
  msg.value = ''
  try {
    await api.put('/users/me/professional-profile', form)
    msg.value = 'Perfil profesional guardado correctamente'
    msgType.value = 'success'
    setTimeout(() => router.push('/profesor/dashboard'), 1500)
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al guardar'
    msgType.value = 'error'
  }
  saving.value = false
}
</script>
