<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-4">Clases Disponibles</h1>
    <div class="card mb-6 grid grid-cols-2 md:grid-cols-4 gap-3">
      <select v-model="filtros.disciplina" class="input-field text-sm py-2">
        <option value="">Todas</option>
        <option v-for="d in disciplinas" :key="d" :value="d">{{ d }}</option>
      </select>
      <select v-model="filtros.nivel" class="input-field text-sm py-2">
        <option value="">Todas</option>
        <option v-for="n in niveles" :key="n" :value="n">{{ n }}</option>
      </select>
      <input v-model="filtros.comuna" class="input-field text-sm py-2" placeholder="Comuna" />
      <div class="col-span-2 md:col-span-1 flex space-x-2">
        <input v-model="filtros.precioMax" type="number" class="input-field text-sm py-2" placeholder="Precio max" />
        <button @click="buscar" class="btn-primary text-sm px-4">Buscar</button>
      </div>
    </div>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando clases...</div>
    <div v-else-if="classes.length === 0" class="text-center text-gray-400 py-20">
      No hay clases disponibles en este momento.
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="c in classes" :key="c.id" class="card hover:border-primary/50 transition-colors cursor-pointer" @click="goToClass(c.id)">
        <h3 class="text-lg font-semibold text-white mb-2">{{ c.title }}</h3>
        <div class="flex flex-wrap gap-2 mb-3">
          <span class="badge badge-blue">{{ c.discipline }}</span>
          <span class="badge badge-green">{{ c.level }}</span>
        </div>
        <p class="text-gray-400 text-sm mb-4 line-clamp-2">{{ c.description }}</p>
        <div class="flex items-center justify-between text-sm text-gray-400">
          <span>{{ formatDate(c.startTime) }}</span>
          <span class="text-primary font-medium">${{ c.price?.toLocaleString() }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import classService from '@/services/classService'

const router = useRouter()
const classes = ref([])
const loading = ref(true)
const disciplinas = ['CUECA', 'BALLET', 'DANZA', 'TEATRO', 'MUSICA', 'OTRO']
const niveles = ['BASICO', 'INTERMEDIO', 'AVANZADO']
const filtros = reactive({ disciplina: '', nivel: '', comuna: '', precioMax: null })

onMounted(() => buscar())

async function buscar() {
  loading.value = true
  try {
    const params = {}
    if (filtros.disciplina) params.disciplina = filtros.disciplina
    if (filtros.nivel) params.nivel = filtros.nivel
    if (filtros.comuna) params.comuna = filtros.comuna
    if (filtros.precioMax) params.precioMax = filtros.precioMax
    const data = await classService.getClasses(params)
    classes.value = Array.isArray(data) ? data : data.content || []
  } catch {
    classes.value = []
  } finally {
    loading.value = false
  }
}

function goToClass(id) {
  router.push(`/alumno/clases/${id}`)
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('es-CL', {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
  })
}
</script>
