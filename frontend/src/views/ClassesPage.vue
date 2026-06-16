<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">

    <div
      v-motion
      :initial="{ opacity: 0, y: 16 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    >
      <h1 class="text-3xl font-bold text-white mb-1">Clases Disponibles</h1>
      <p class="text-gray-500 text-sm mb-6">Encuentra la clase perfecta para ti</p>
    </div>

    <div
      v-motion
      :initial="{ opacity: 0, y: 12 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 380, delay: 80 } }"
      class="card mb-8 grid grid-cols-2 md:grid-cols-4 gap-3"
    >
      <select v-model="filtros.disciplina" class="input-field text-sm py-2">
        <option value="">Todas las disciplinas</option>
        <optgroup v-for="g in disciplineGroups" :key="g.category || 'otras'" :label="g.label">
          <option v-for="item in g.items" :key="item" :value="item">{{ item }}</option>
        </optgroup>
      </select>
      <select v-model="filtros.nivel" class="input-field text-sm py-2">
        <option value="">Todos los niveles</option>
        <option v-for="n in niveles" :key="n" :value="n">{{ n }}</option>
      </select>
      <input v-model="filtros.comuna" class="input-field text-sm py-2" placeholder="Comuna" />
      <div class="col-span-2 md:col-span-1 flex flex-col gap-2">
        <div class="flex space-x-2">
          <input v-model.number="filtros.edadMin" type="number" class="input-field text-sm py-2" placeholder="Edad min." min="0" max="99" />
          <input v-model.number="filtros.edadMax" type="number" class="input-field text-sm py-2" placeholder="Edad max." min="0" max="99" />
        </div>
        <div class="flex space-x-2">
          <input v-model.number="filtros.precioMax" type="number" class="input-field text-sm py-2" placeholder="Precio max." />
          <button @click="buscar" class="btn-primary text-sm px-4">Buscar</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando clases...</p>
    </div>
    <div v-else-if="classes.length === 0" class="card text-center py-20">
      <p class="text-gray-400 mb-2">No hay clases disponibles en este momento.</p>
      <p class="text-gray-600 text-sm">Intenta ajustar los filtros de busqueda.</p>
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
      <div
        v-for="(c, i) in classes"
        :key="c.id"
        v-motion
        :initial="{ opacity: 0, y: 20 }"
        :visible="{ opacity: 1, y: 0, transition: { duration: 350, delay: Math.min(i * 50, 300) } }"
        class="card cursor-pointer group"
        @click="goToClass(c.id)"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex flex-wrap gap-1.5">
            <span class="badge badge-purple">{{ c.discipline }}</span>
            <span class="badge badge-green">{{ c.level }}</span>
          </div>
          <span class="text-primary font-bold text-lg">${{ c.price?.toLocaleString() }}</span>
        </div>
        <h3 class="text-base font-semibold text-white mb-2 group-hover:text-primary transition-colors">{{ c.title }}</h3>
        <p class="text-gray-500 text-sm mb-4 line-clamp-2">{{ c.description }}</p>
        <div class="flex items-center justify-between text-xs text-gray-600 border-t border-[#1e2130] pt-3 mt-auto">
          <span>{{ formatDate(c.startTime) }}</span>
          <span class="text-primary/70 group-hover:text-primary transition-colors">Ver detalle &rarr;</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import classService from '@/services/classService'

const router = useRouter()
const classes = ref([])
const loading = ref(true)
const disciplineGroups = ref([])
const niveles = ['BASICO', 'INTERMEDIO', 'AVANZADO']
const filtros = reactive({ disciplina: '', nivel: '', comuna: '', precioMax: null, edadMin: null, edadMax: null })

onMounted(async () => {
  await Promise.all([buscar(), loadDisciplines()])
})

async function loadDisciplines() {
  try {
    const data = await api.get('/classes/disciplines')
    disciplineGroups.value = Array.isArray(data) ? data : []
  } catch { disciplineGroups.value = [] }
}

async function buscar() {
  loading.value = true
  try {
    const params = {}
    if (filtros.disciplina) params.disciplina = filtros.disciplina
    if (filtros.nivel) params.nivel = filtros.nivel
    if (filtros.comuna) params.comuna = filtros.comuna
    if (filtros.precioMax) params.precioMax = filtros.precioMax
    if (filtros.edadMin) params.edadMin = filtros.edadMin
    if (filtros.edadMax) params.edadMax = filtros.edadMax
    const data = await classService.getClasses(params)
    classes.value = Array.isArray(data) ? data : data.content || []
  } catch {
    classes.value = []
  } finally {
    loading.value = false
  }
}

function goToClass(id) { router.push(`/alumno/clases/${id}`) }

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('es-CL', {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
  })
}
</script>
