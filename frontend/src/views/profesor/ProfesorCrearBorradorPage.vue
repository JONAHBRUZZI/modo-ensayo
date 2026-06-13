<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="flex items-center gap-3 mb-2">
      <router-link to="/profesor/borradores" class="text-gray-400 hover:text-white transition-colors">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </router-link>
      <h1 class="text-3xl font-bold text-white">Crear Borrador de Clase</h1>
    </div>
    <p class="text-gray-400 text-sm mb-8 ml-8">
      Define los datos de tu clase. Despues puedes asignar una sala y publicarla.
    </p>

    <!-- Alerta identidad no validada -->
    <div v-if="!identidadValidada" class="bg-yellow-500/10 border border-yellow-500/30 rounded-xl px-4 py-4 mb-6 flex items-start gap-3">
      <svg class="w-5 h-5 text-yellow-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
      </svg>
      <div>
        <p class="text-yellow-300 text-sm font-medium">Identidad no validada</p>
        <p class="text-gray-400 text-sm">Necesitas validar tu identidad para crear clases.</p>
        <router-link to="/alumno/validacion-identidad" class="text-primary text-sm underline mt-1 inline-block">Validar ahora</router-link>
      </div>
    </div>

    <form @submit.prevent="handleSubmit" class="card space-y-4">
      <!-- Titulo -->
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Titulo *</label>
        <input v-model="form.title" required class="input-field" placeholder="Ej: Guitarra para principiantes" />
      </div>

      <!-- Disciplina y Nivel -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Disciplina *</label>
          <select v-model="form.discipline" required class="input-field">
            <option value="">Seleccionar</option>
            <option value="CUECA">Cueca</option><option value="BALLET">Ballet</option><option value="DANZA">Danza</option>
            <option>TEATRO</option><option>CANTO</option><option>GUITARRA</option>
            <option>BATERIA</option><option>BAJO</option><option>PIANO</option>
            <option>VIOLIN</option><option>SAXOFON</option><option>OTRO</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Nivel *</label>
          <select v-model="form.level" required class="input-field">
            <option value="">Seleccionar</option>
            <option value="BASICO">Básico</option>
            <option value="INTERMEDIO">Intermedio</option>
            <option value="AVANZADO">Avanzado</option>
          </select>
        </div>
      </div>

      <!-- Descripción -->
      <div>
        <label class="block text-sm font-medium text-gray-300 mb-1">Descripción</label>
        <textarea v-model="form.description" class="input-field" rows="3"
          placeholder="Describe que van a aprender los alumnos en esta clase..."></textarea>
      </div>

      <!-- Capacidad, Duracion, Precio -->
      <div class="grid grid-cols-3 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Capacidad *</label>
          <input v-model.number="form.capacity" type="number" min="1" max="100" required class="input-field"
            placeholder="Ej: 10" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Duracion (min) *</label>
          <input v-model.number="form.duration" type="number" min="30" max="480" step="15" required class="input-field"
            placeholder="Ej: 60" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Precio ($) *</label>
          <input v-model.number="form.price" type="number" min="0" required class="input-field"
            placeholder="Ej: 15000" />
        </div>
      </div>

      <!-- Rango de edad -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Edad minima</label>
          <input v-model.number="form.minAge" type="number" min="0" max="99" class="input-field" placeholder="0" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Edad maxima</label>
          <input v-model.number="form.maxAge" type="number" min="0" max="99" class="input-field" placeholder="99" />
        </div>
      </div>

      <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>

      <div class="flex gap-3 pt-2">
        <button type="submit" :disabled="loading || !identidadValidada" class="btn-primary flex-1">
          {{ loading ? 'Guardando...' : 'Guardar Borrador' }}
        </button>
        <router-link to="/profesor/borradores" class="flex-1 text-center px-4 py-2 rounded-xl border border-white/10 text-gray-300 hover:bg-white/5 text-sm transition-colors">
          Cancelar
        </router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import classService from '@/services/classService'
import { useAuth } from '@/stores/auth'

const router = useRouter()
// identidadValidada se valida UNA sola vez al registrar el usuario.
// Todos los contextos (profesor, sede, alumno) leen el mismo estado desde el store.
const { identidadValidada } = useAuth()

const loading = ref(false)
const error = ref('')

const form = ref({
  title: '',
  discipline: '',
  level: '',
  description: '',
  capacity: null,
  duration: 60,
  price: null,
  minAge: 0,
  maxAge: 99
})

async function handleSubmit() {
  error.value = ''
  loading.value = true
  try {
    await classService.createBorrador({
      title: form.value.title,
      discipline: form.value.discipline,
      level: form.value.level,
      description: form.value.description,
      capacity: form.value.capacity,
      duration: form.value.duration,
      price: form.value.price,
      minAge: form.value.minAge,
      maxAge: form.value.maxAge
    })
    router.push('/profesor/borradores')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Error al guardar el borrador'
  }
  loading.value = false
}
</script>
