<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Reseñas Pendientes</h1>
    <p class="text-gray-400 text-sm mb-8">Clases completadas que aun no has evaluado.</p>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <div v-else-if="elegibles.length === 0">
      <p class="text-gray-400">No tienes clases pendientes de reseña.</p>
      <router-link to="/classes" class="btn-primary mt-4 inline-block">Buscar Clases</router-link>
    </div>

    <div v-else class="space-y-4">
      <div v-for="item in elegibles" :key="item.classId" class="card space-y-4">
        <div class="flex items-start justify-between">
          <div>
            <h3 class="text-white font-semibold">{{ item.classTitle }}</h3>
            <p class="text-gray-400 text-sm">{{ item.targetLabel || item.targetType }}</p>
            <p class="text-gray-500 text-xs mt-1">{{ formatDate(item.classDate) }}</p>
          </div>
          <span class="text-xs text-yellow-400 bg-yellow-400/10 px-2 py-1 rounded-full flex-shrink-0">
            Sin reseña
          </span>
        </div>

        <!-- Formulario de reseña -->
        <div v-if="formActivo === item.classId" class="space-y-3 border-t border-dark-border pt-4">
          <!-- Estrellas -->
          <div>
            <p class="text-sm text-gray-300 mb-2">Calificacion</p>
            <div class="flex gap-1">
              <button v-for="n in 5" :key="n"
                      @click="formData[item.classId] = { ...formData[item.classId], rating: n }"
                      class="text-2xl transition-colors"
                      :class="(formData[item.classId]?.rating || 0) >= n ? 'text-yellow-400' : 'text-gray-600'">
                ★
              </button>
            </div>
          </div>

          <!-- Comentario -->
          <div>
            <label class="block text-sm text-gray-300 mb-1">Comentario (opcional)</label>
            <textarea
              v-model="formData[item.classId].comment"
              rows="3"
              class="input-field"
              placeholder="Cuéntanos tu experiencia..."
            ></textarea>
          </div>

          <p v-if="errores[item.classId]" class="text-red-400 text-sm">{{ errores[item.classId] }}</p>

          <div class="flex gap-3">
            <button @click="enviarReseña(item)"
                    :disabled="!formData[item.classId]?.rating || enviando[item.classId]"
                    class="btn-primary flex-1 text-sm">
              {{ enviando[item.classId] ? 'Enviando...' : 'Publicar reseña' }}
            </button>
            <button @click="formActivo = null" class="btn-secondary text-sm px-4">
              Cancelar
            </button>
          </div>
        </div>

        <button v-else @click="abrirForm(item)" class="btn-secondary text-sm w-full">
          Dejar reseña
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '@/services/api'

const elegibles = ref([])
const loading = ref(true)
const formActivo = ref(null)
const formData = reactive({})
const enviando = reactive({})
const errores = reactive({})

onMounted(async () => {
  try {
    const res = await api.get('/reviews/eligible/student')
    elegibles.value = Array.isArray(res.data) ? res.data : []
  } catch {
    elegibles.value = []
  } finally {
    loading.value = false
  }
})

function abrirForm(item) {
  formActivo.value = item.classId
  if (!formData[item.classId]) {
    formData[item.classId] = { rating: 0, comment: '' }
  }
}

async function enviarReseña(item) {
  const data = formData[item.classId]
  if (!data?.rating) return
  enviando[item.classId] = true
  errores[item.classId] = ''
  try {
    await api.post('/reviews', {
      classId: item.classId,
      targetId: item.targetId,
      targetType: item.targetType || 'CLASS',
      score: data.rating,
      comment: data.comment || null
    })
    elegibles.value = elegibles.value.filter(e => e.classId !== item.classId)
    formActivo.value = null
  } catch (e) {
    errores[item.classId] = e.response?.data?.message || 'Error al enviar la reseña'
  } finally {
    enviando[item.classId] = false
  }
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'long', year: 'numeric' })
}
</script>
