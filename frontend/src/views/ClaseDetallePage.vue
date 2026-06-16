<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando clase...</p>
    </div>
    <div
      v-else-if="clase"
      v-motion
      :initial="{ opacity: 0, y: 20 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
      class="space-y-6"
    >
      <div class="card">
        <div class="flex flex-wrap gap-2 mb-4">
          <span class="badge badge-purple">{{ clase.discipline }}</span>
          <span class="badge badge-green">{{ clase.level }}</span>
          <span class="badge badge-yellow">{{ clase.status }}</span>
        </div>
        <h1 class="text-2xl font-bold text-white mb-3">{{ clase.title }}</h1>
        <p class="text-gray-400 leading-relaxed">{{ clase.description }}</p>
      </div>

      <div class="card">
        <h2 class="text-sm font-medium text-gray-500 uppercase tracking-wider mb-4">Detalles</h2>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div class="flex flex-col gap-1">
            <span class="text-gray-500 text-xs">Fecha</span>
            <span class="text-white font-medium">{{ formatDate(clase.startTime) }}</span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="text-gray-500 text-xs">Duración</span>
            <span class="text-white font-medium">{{ clase.duration }} min</span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="text-gray-500 text-xs">Capacidad</span>
            <span class="text-white font-medium">{{ clase.capacity }} personas</span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="text-gray-500 text-xs">Precio</span>
            <span class="text-primary font-bold text-lg">${{ clase.price?.toLocaleString() }}</span>
          </div>
        </div>
      </div>

      <div class="card space-y-4">
        <div v-if="beneficiaries.length > 0">
          <label class="block text-sm font-medium text-gray-300 mb-2">Inscribir a:</label>
          <select v-model="selectedBeneficiary" class="input-field">
            <option :value="null">Yo ({{ user?.fullName }})</option>
            <option v-for="b in beneficiaries" :key="b.id" :value="b">{{ b.email || b.name || 'Asociado' }}</option>
          </select>
        </div>
        <button @click="addToCart" :disabled="adding" class="btn-primary w-full">
          {{ adding ? 'Agregando...' : 'Agregar al Carrito' }}
        </button>
        <p v-if="msg" :class="msg.includes('Error') ? 'text-red-400' : 'text-green-400'" class="text-sm text-center">{{ msg }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '@/stores/auth'
import classService from '@/services/classService'
import paymentService from '@/services/paymentService'
import userService from '@/services/userService'

const route = useRoute()
const { user } = useAuth()
const clase = ref(null)
const loading = ref(true)
const adding = ref(false)
const msg = ref('')
const beneficiaries = ref([])
const selectedBeneficiary = ref(null)

onMounted(async () => {
  try {
    clase.value = await classService.getClass(route.params.id)
    const assoc = await userService.getAssociates?.()
    beneficiaries.value = Array.isArray(assoc) ? assoc : []
  } catch {} finally { loading.value = false }
})

async function addToCart() {
  adding.value = true; msg.value = ''
  try {
    await paymentService.addToCart(clase.value.id, selectedBeneficiary.value?.id || null)
    msg.value = '¡Clase agregada al carrito!'
  } catch (e) {
    msg.value = e.response?.data?.message || 'Error al agregar'
  } finally { adding.value = false }
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleDateString('es-CL', { weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' })
}
</script>
