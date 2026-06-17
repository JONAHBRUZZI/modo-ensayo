<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-12"
  >
    <h1 class="text-3xl font-bold text-white">Reseñas</h1>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <template v-else>
      <!-- ── Seccion 1: Pendientes ───────────────────────────────── -->
      <section>
        <h2 class="text-xl font-semibold text-white mb-1">Reseñas Pendientes</h2>
        <p class="text-gray-400 text-sm mb-5">Clases completadas que aún no has evaluado.</p>

        <div v-if="elegibles.length === 0" class="card text-center py-8">
          <p class="text-gray-400 text-sm">No tienes clases pendientes de reseña.</p>
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
      </section>

      <!-- ── Seccion 2: Mis reseñas ──────────────────────────────── -->
      <section>
        <h2 class="text-xl font-semibold text-white mb-1">Mis Reseñas</h2>
        <p class="text-gray-400 text-sm mb-5">Las opiniones que has dejado en tus clases.</p>

        <div v-if="misReseñas.length === 0" class="card text-center py-8">
          <p class="text-gray-400 text-sm">Aún no has escrito ninguna reseña.</p>
        </div>

        <div v-else class="space-y-3">
          <div v-for="r in misReseñas" :key="r.id" class="card">
            <div class="flex items-start justify-between gap-3">
              <h3 class="text-white font-medium">{{ r.classTitle }}</h3>
              <span class="text-yellow-400 text-sm flex-shrink-0">{{ estrellas(r.score) }}</span>
            </div>
            <p v-if="r.comment" class="text-gray-400 text-sm mt-1">{{ r.comment }}</p>
            <p class="text-gray-600 text-xs mt-2">{{ formatDate(r.createdAt) }}</p>
          </div>
        </div>
      </section>

      <!-- ── Seccion 3: Recomendaciones de la comunidad ──────────── -->
      <section>
        <h2 class="text-xl font-semibold text-white mb-1">Recomendaciones de la comunidad</h2>
        <p class="text-gray-400 text-sm mb-5">Opiniones de otros alumnos sobre distintas clases.</p>

        <div v-if="recomendaciones.length === 0" class="card text-center py-8">
          <p class="text-gray-400 text-sm">Todavía no hay reseñas de otros alumnos.</p>
        </div>

        <div v-else class="space-y-3">
          <div v-for="r in recomendaciones" :key="r.id" class="card">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h3 class="text-white font-medium">{{ r.classTitle }}</h3>
                <p class="text-gray-500 text-xs mt-0.5">por {{ r.authorName }}</p>
              </div>
              <span class="text-yellow-400 text-sm flex-shrink-0">{{ estrellas(r.score) }}</span>
            </div>
            <p v-if="r.comment" class="text-gray-400 text-sm mt-2">{{ r.comment }}</p>
            <p class="text-gray-600 text-xs mt-2">{{ formatDate(r.createdAt) }}</p>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '@/services/api'
import { reviewService } from '@/services/reviewService'

const elegibles = ref([])
const misReseñas = ref([])
const recomendaciones = ref([])
const loading = ref(true)
const formActivo = ref(null)
const formData = reactive({})
const enviando = reactive({})
const errores = reactive({})

async function cargar() {
  const [elig, mias, recientes] = await Promise.all([
    reviewService.getStudentEligible().then(r => r.data).catch(() => []),
    reviewService.getMine().then(r => r.data).catch(() => []),
    reviewService.getRecent().then(r => r.data).catch(() => [])
  ])
  elegibles.value = Array.isArray(elig) ? elig : []
  misReseñas.value = Array.isArray(mias) ? mias : []
  recomendaciones.value = Array.isArray(recientes) ? recientes : []
}

onMounted(async () => {
  try {
    await cargar()
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
    // Refrescar "Mis Reseñas" para que aparezca la recién creada
    misReseñas.value = await reviewService.getMine().then(r => r.data).catch(() => misReseñas.value)
  } catch (e) {
    errores[item.classId] = e.response?.data?.message || 'Error al enviar la reseña'
  } finally {
    enviando[item.classId] = false
  }
}

function estrellas(score) {
  const n = Math.round(score || 0)
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'long', year: 'numeric' })
}
</script>
