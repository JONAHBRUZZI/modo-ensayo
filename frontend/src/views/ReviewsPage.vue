<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-2">Reseñas</h1>
    <p class="text-gray-400 text-sm mb-6">Opiniones y valoraciones según lo que te corresponde ver y evaluar.</p>

    <!-- Pestañas -->
    <div class="flex flex-wrap gap-2 mb-8 border-b border-white/10 pb-3">
      <button v-for="t in tabsVisibles" :key="t.value"
              @click="tab = t.value"
              :class="[
                'px-4 py-2 rounded-lg text-sm transition-colors',
                tab === t.value ? 'bg-primary text-white' : 'bg-[#1a1d2e] text-gray-400 hover:text-white border border-white/10'
              ]">
        {{ t.label }}
      </button>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>

    <template v-else>
      <!-- ══════════ PESTAÑA: CLASES ══════════ -->
      <div v-show="tab === 'CLASS'" class="space-y-10">
        <!-- Pendientes -->
        <section>
          <h2 class="text-lg font-semibold text-white mb-1">Pendientes de evaluar</h2>
          <p class="text-gray-400 text-sm mb-4">Clases completadas que aún no has evaluado.</p>
          <div v-if="elegibles.length === 0" class="card text-center py-8">
            <p class="text-gray-400 text-sm">No tienes clases pendientes de reseña.</p>
            <router-link to="/classes" class="btn-primary mt-4 inline-block">Buscar Clases</router-link>
          </div>
          <div v-else class="space-y-4">
            <div v-for="item in elegibles" :key="item.classId" class="card space-y-4">
              <div class="flex items-start justify-between">
                <div>
                  <h3 class="text-white font-semibold">{{ item.classTitle }}</h3>
                  <p class="text-gray-500 text-xs mt-1">{{ formatDate(item.classDate) }}</p>
                </div>
                <span class="text-xs text-yellow-400 bg-yellow-400/10 px-2 py-1 rounded-full flex-shrink-0">Sin reseña</span>
              </div>
              <div v-if="formActivo === item.classId" class="space-y-3 border-t border-dark-border pt-4">
                <div>
                  <p class="text-sm text-gray-300 mb-2">Calificacion</p>
                  <div class="flex gap-1">
                    <button v-for="n in 5" :key="n"
                            @click="formData[item.classId] = { ...formData[item.classId], rating: n }"
                            class="text-2xl transition-colors"
                            :class="(formData[item.classId]?.rating || 0) >= n ? 'text-yellow-400' : 'text-gray-600'">★</button>
                  </div>
                </div>
                <div>
                  <label class="block text-sm text-gray-300 mb-1">Comentario (opcional)</label>
                  <textarea v-model="formData[item.classId].comment" rows="3" class="input-field" placeholder="Cuéntanos tu experiencia..."></textarea>
                </div>
                <p v-if="errores[item.classId]" class="text-red-400 text-sm">{{ errores[item.classId] }}</p>
                <div class="flex gap-3">
                  <button @click="enviarReseña(item)" :disabled="!formData[item.classId]?.rating || enviando[item.classId]" class="btn-primary flex-1 text-sm">
                    {{ enviando[item.classId] ? 'Enviando...' : 'Publicar reseña' }}
                  </button>
                  <button @click="formActivo = null" class="btn-secondary text-sm px-4">Cancelar</button>
                </div>
              </div>
              <button v-else @click="abrirForm(item)" class="btn-secondary text-sm w-full">Dejar reseña</button>
            </div>
          </div>
        </section>

        <ListaReseñas titulo="Mis reseñas" subtitulo="Las opiniones que has dejado en clases."
                      :items="misPorTipo('CLASS')" vacio="Aún no has reseñado ninguna clase." :mostrarAutor="false" />
        <ListaReseñas titulo="Recomendaciones de la comunidad" subtitulo="Opiniones de otros alumnos sobre distintas clases."
                      :items="comunidadPorTipo('CLASS')" vacio="Todavía no hay reseñas de otros alumnos." :mostrarAutor="true" />
      </div>

      <!-- ══════════ PESTAÑA: MAESTROS ══════════ -->
      <div v-show="tab === 'TEACHER'" class="space-y-10">
        <ListaReseñas titulo="Mis reseñas a maestros" subtitulo="Lo que has opinado sobre profesores."
                      :items="misPorTipo('TEACHER')" vacio="Aún no has reseñado a ningún maestro." :mostrarAutor="false" />
        <ListaReseñas titulo="Reputación de la comunidad" subtitulo="Opiniones de otros sobre los maestros."
                      :items="comunidadPorTipo('TEACHER')" vacio="Todavía no hay reseñas sobre maestros." :mostrarAutor="true" />
      </div>

      <!-- ══════════ PESTAÑA: SEDES ══════════ -->
      <div v-show="tab === 'VENUE'" class="space-y-10">
        <ListaReseñas titulo="Mis reseñas a sedes" subtitulo="Lo que has opinado sobre sedes y salas."
                      :items="misPorTipo('VENUE')" vacio="Aún no has reseñado ninguna sede." :mostrarAutor="false" />
        <ListaReseñas titulo="Reputación de la comunidad" subtitulo="Opiniones de otros sobre las sedes."
                      :items="comunidadPorTipo('VENUE')" vacio="Todavía no hay reseñas sobre sedes." :mostrarAutor="true" />
      </div>

      <!-- ══════════ PESTAÑA: ALUMNOS (solo maestro/sede) ══════════ -->
      <div v-show="tab === 'STUDENT'" class="space-y-10">
        <ListaReseñas titulo="Mis reseñas a alumnos" subtitulo="Valoraciones que has dejado sobre tus alumnos. Solo tú las ves."
                      :items="misPorTipo('STUDENT')" vacio="Aún no has reseñado a ningún alumno." :mostrarAutor="false" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, h } from 'vue'
import api from '@/services/api'
import { reviewService } from '@/services/reviewService'
import { useAuth } from '@/stores/auth'

const { user, hasRoleTeacher, puedeVerContextoSede } = useAuth()

const tab = ref('CLASS')
const elegibles = ref([])
const mias = ref([])
const comunidad = ref([])
const loading = ref(true)
const formActivo = ref(null)
const formData = reactive({})
const enviando = reactive({})
const errores = reactive({})

// La pestaña Alumnos solo es visible para maestros o administradores de sede.
const puedeVerAlumnos = computed(() => hasRoleTeacher.value || puedeVerContextoSede.value)

const tabsVisibles = computed(() => {
  const base = [
    { value: 'CLASS', label: 'Clases' },
    { value: 'TEACHER', label: 'Maestros' },
    { value: 'VENUE', label: 'Sedes' },
  ]
  if (puedeVerAlumnos.value) base.push({ value: 'STUDENT', label: 'Alumnos' })
  return base
})

const misPorTipo = (tipo) => mias.value.filter(r => r.targetType === tipo)
const comunidadPorTipo = (tipo) => comunidad.value.filter(r => r.targetType === tipo)

async function cargar() {
  const [elig, m, c] = await Promise.all([
    reviewService.getStudentEligible().then(r => r.data).catch(() => []),
    reviewService.getMine().then(r => r.data).catch(() => []),
    reviewService.getRecent().then(r => r.data).catch(() => [])
  ])
  elegibles.value = Array.isArray(elig) ? elig : []
  mias.value = Array.isArray(m) ? m : []
  comunidad.value = Array.isArray(c) ? c : []
}

onMounted(async () => {
  try { await cargar() } finally { loading.value = false }
})

function abrirForm(item) {
  formActivo.value = item.classId
  if (!formData[item.classId]) formData[item.classId] = { rating: 0, comment: '' }
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
    mias.value = await reviewService.getMine().then(r => r.data).catch(() => mias.value)
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

// Componente de lista de reseñas reutilizable (render inline)
const ListaReseñas = {
  props: { titulo: String, subtitulo: String, items: Array, vacio: String, mostrarAutor: Boolean },
  setup(props) {
    const estrellas = (s) => { const n = Math.round(s || 0); return '★'.repeat(n) + '☆'.repeat(5 - n) }
    const fecha = (d) => d ? new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'long', year: 'numeric' }) : ''
    return () => h('section', {}, [
      h('h2', { class: 'text-lg font-semibold text-white mb-1' }, props.titulo),
      h('p', { class: 'text-gray-400 text-sm mb-4' }, props.subtitulo),
      props.items.length === 0
        ? h('div', { class: 'card text-center py-8' }, [h('p', { class: 'text-gray-400 text-sm' }, props.vacio)])
        : h('div', { class: 'space-y-3' }, props.items.map(r => h('div', { key: r.id, class: 'card' }, [
            h('div', { class: 'flex items-start justify-between gap-3' }, [
              h('div', {}, [
                h('h3', { class: 'text-white font-medium' }, r.targetName || r.classTitle || 'Reseña'),
                props.mostrarAutor ? h('p', { class: 'text-gray-500 text-xs mt-0.5' }, 'por ' + (r.authorName || 'Usuario')) : null
              ]),
              h('span', { class: 'text-yellow-400 text-sm flex-shrink-0' }, estrellas(r.score))
            ]),
            r.comment ? h('p', { class: 'text-gray-400 text-sm mt-2' }, r.comment) : null,
            h('p', { class: 'text-gray-600 text-xs mt-2' }, fecha(r.createdAt))
          ])))
    ])
  }
}
</script>
