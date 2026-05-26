<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="clase" class="card space-y-6">
      <div>
        <h1 class="text-2xl font-bold text-white mb-2">{{ clase.title }}</h1>
        <div class="flex flex-wrap gap-2">
          <span class="badge badge-blue">{{ clase.discipline }}</span>
          <span class="badge badge-green">{{ clase.level }}</span>
          <span class="badge badge-yellow">{{ clase.status }}</span>
        </div>
      </div>
      <p class="text-gray-300">{{ clase.description }}</p>
      <div class="grid grid-cols-2 gap-4 text-sm">
        <div><span class="text-gray-400">Fecha:</span> <span class="text-white">{{ formatDate(clase.startTime) }}</span></div>
        <div><span class="text-gray-400">Duracion:</span> <span class="text-white">{{ clase.duration }} min</span></div>
        <div><span class="text-gray-400">Capacidad:</span> <span class="text-white">{{ clase.capacity }} personas</span></div>
        <div><span class="text-gray-400">Precio:</span> <span class="text-primary font-semibold">${{ clase.price?.toLocaleString() }}</span></div>
      </div>
      <div class="space-y-3 pt-2">
        <div v-if="beneficiaries.length > 0">
          <label class="block text-sm font-medium text-gray-300 mb-1">Inscribir a:</label>
          <select v-model="selectedBeneficiary" class="input-field">
            <option :value="null">Yo ({{ user?.fullName }})</option>
            <option v-for="b in beneficiaries" :key="b.id" :value="b">{{ b.email || b.name || 'Asociado' }}</option>
          </select>
        </div>
        <button @click="addToCart" :disabled="adding" class="btn-primary w-full">
          {{ adding ? 'Agregando...' : 'Agregar al Carrito' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import classService from '@/services/classService'
import paymentService from '@/services/paymentService'
import associateService from '@/services/associateService'
import { useAuth } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const { user } = useAuth()
const clase = ref(null)
const loading = ref(true)
const adding = ref(false)
const beneficiaries = ref([])
const selectedBeneficiary = ref(null)

onMounted(async () => {
  try {
    const data = await classService.getClasses()
    const list = Array.isArray(data) ? data : data.content || []
    clase.value = list.find(c => c.id == route.params.claseId)
    const assoc = await associateService.getAssociates()
    beneficiaries.value = Array.isArray(assoc) ? assoc : assoc?.content || []
  } catch {} finally {
    loading.value = false
  }
})

async function addToCart() {
  adding.value = true
  try {
    const beneficiaryType = selectedBeneficiary.value ? 'ASSOCIATE' : 'USER'
    const beneficiaryId = selectedBeneficiary.value?.id || null
    await paymentService.addToCart(clase.value.id, beneficiaryType, beneficiaryId)
    router.push('/cart')
  } catch {} finally {
    adding.value = false
  }
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' })
}
</script>
