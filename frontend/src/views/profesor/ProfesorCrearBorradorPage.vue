<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="flex items-center gap-3 mb-2">
      <router-link to="/profesor/borradores" class="text-gray-400 hover:text-white transition-colors">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </router-link>
      <h1 class="text-3xl font-bold text-white">{{ esEdicion ? 'Editar Borrador' : 'Crear Borrador de Clase' }}</h1>
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
        <router-link to="/profile/identity" class="text-primary text-sm underline mt-1 inline-block">Validar ahora</router-link>
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
          <input v-model="form.discipline" required class="input-field" placeholder="Ej: Guitarra, Ballet, Karate..." list="discipline-list" autocomplete="off" @blur="form.discipline = normalizarDisciplina(form.discipline)" />
          <datalist id="discipline-list">
            <optgroup v-for="g in disciplineGroups" :key="g.category || 'otras'" :label="g.label">
              <option v-for="item in g.items" :key="item" :value="item" />
            </optgroup>
          </datalist>
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
      <div class="grid grid-cols-2 gap-4">
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
      <p class="text-xs text-gray-500 -mt-1">La capacidad se define automáticamente por la sala cuando reserves una.</p>

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
          {{ loading ? 'Guardando...' : (esEdicion ? 'Guardar Cambios' : 'Guardar Borrador') }}
        </button>
        <router-link to="/profesor/borradores" class="flex-1 text-center px-4 py-2 rounded-xl border border-white/10 text-gray-300 hover:bg-white/5 text-sm transition-colors">
          Cancelar
        </router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import classService from '@/services/classService'
import { useAuth } from '@/stores/auth'
import { normalizarDisciplina } from '@/utils/disciplina'

const router = useRouter()
const route = useRoute()
const { identidadValidada } = useAuth()

const loading = ref(false)
const error = ref('')
const disciplineGroups = ref([])
// Modo edición: ?edit=<id del borrador>.
const editId = ref(route.query.edit || null)
const esEdicion = computed(() => !!editId.value)

const form = ref({
  title: '',
  discipline: '',
  level: '',
  description: '',
  duration: 60,
  price: null,
  minAge: 0,
  maxAge: 99
})

onMounted(async () => {
  try {
    const data = await classService.getDisciplines()
    disciplineGroups.value = Array.isArray(data) ? data : []
  } catch { disciplineGroups.value = [] }

  // Si es edición, precargar el borrador existente.
  if (editId.value) {
    try {
      const c = await classService.getClassById(editId.value)
      if (c) {
        form.value.title = c.title || ''
        form.value.discipline = c.discipline || ''
        form.value.level = c.level || ''
        form.value.description = c.description || ''
        form.value.duration = c.duration || 60
        form.value.price = c.price ?? null
        form.value.minAge = c.minAge ?? 0
        form.value.maxAge = c.maxAge ?? 99
      }
    } catch (err) {
      console.error('Error al cargar el borrador', err)
    }
  }
})

async function handleSubmit() {
  error.value = ''
  form.value.discipline = normalizarDisciplina(form.value.discipline)
  loading.value = true
  try {
    const payload = {
      title: form.value.title,
      discipline: form.value.discipline,
      level: form.value.level,
      description: form.value.description,
      duration: form.value.duration,
      price: form.value.price,
      minAge: form.value.minAge,
      maxAge: form.value.maxAge
    }
    if (esEdicion.value) {
      await classService.updateDraft(editId.value, payload)
    } else {
      await classService.createBorrador(payload)
    }
    router.push('/profesor/borradores')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Error al guardar el borrador'
  }
  loading.value = false
}
</script>
