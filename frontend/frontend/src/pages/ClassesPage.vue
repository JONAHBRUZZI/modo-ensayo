<template>
  <div>
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-gray-900 mb-2">Clases Disponibles</h1>
      <p class="text-gray-600">Explora y reserva las mejores clases artisticas</p>
    </div>

    <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="n in 6" :key="n" class="bg-white rounded-xl shadow-md p-6 animate-pulse">
        <div class="h-4 bg-gray-200 rounded w-3/4 mb-4"></div>
        <div class="h-3 bg-gray-200 rounded w-1/2 mb-2"></div>
        <div class="h-3 bg-gray-200 rounded w-2/3 mb-4"></div>
        <div class="h-10 bg-gray-200 rounded"></div>
      </div>
    </div>

    <div v-else-if="classes.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="cls in classes" :key="cls.id" 
           class="bg-white rounded-xl shadow-md hover:shadow-xl transition-all duration-300 border border-gray-100 overflow-hidden group">
        <div class="h-24 bg-gradient-to-r from-indigo-500 to-purple-600 relative">
          <div class="absolute -bottom-6 left-6">
            <div class="w-12 h-12 bg-white rounded-xl shadow-md flex items-center justify-center">
              <svg class="w-6 h-6 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
          </div>
        </div>
        
        <div class="pt-8 pb-6 px-6">
          <h3 class="text-xl font-bold text-gray-900 mb-2 group-hover:text-indigo-600 transition-colors">
            {{ cls.title }}
          </h3>
          
          <span class="inline-block px-3 py-1 bg-indigo-50 text-indigo-700 text-sm font-medium rounded-full mb-4">
            {{ cls.discipline || 'Arte General' }}
          </span>
          
          <div class="space-y-2 mb-4">
            <div class="flex items-center text-gray-600 text-sm">
              <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              {{ cls.venueName || 'Sede Principal' }}
            </div>
            
            <div class="flex items-center text-gray-600 text-sm">
              <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {{ formatDate(cls.startTime) }}
            </div>
            
            <div class="flex items-center text-gray-600 text-sm">
              <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              {{ cls.capacity }} cupos disponibles
            </div>
          </div>
          
          <div class="flex items-center justify-between mb-4">
            <div>
              <span class="text-2xl font-bold text-gray-900">${{ cls.price?.toLocaleString() }}</span>
              <span class="text-gray-500 text-sm">/clase</span>
            </div>
            <span v-if="cls.capacity <= 3" class="text-red-500 text-sm font-medium">
              ¡Solo {{ cls.capacity }} cupos!
            </span>
          </div>
          
          <button @click="openBeneficiaryModal(cls)" 
                  :disabled="cls.capacity === 0"
                  class="w-full py-3 px-4 rounded-lg font-medium transition-all duration-200 flex items-center justify-center space-x-2"
                  :class="cls.capacity === 0 
                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed' 
                    : 'bg-indigo-600 text-white hover:bg-indigo-700 hover:shadow-md'">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
            <span>{{ cls.capacity === 0 ? 'Sin cupos' : 'Agregar al Carrito' }}</span>
          </button>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-16">
      <div class="inline-flex items-center justify-center w-20 h-20 bg-gray-100 rounded-full mb-4">
        <svg class="w-10 h-10 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <h3 class="text-lg font-medium text-gray-900 mb-2">No hay clases disponibles</h3>
      <p class="text-gray-500 mb-4">Vuelve pronto para ver nuevas clases</p>
    </div>

    <div v-if="showBeneficiaryModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="w-full max-w-md rounded-xl bg-white p-6">
        <h3 class="mb-4 text-lg font-semibold">Inscribir en "{{ selectedClass?.title }}"</h3>
        <p class="mb-3 text-sm text-gray-600">Para quien es esta inscripcion?</p>
        
        <div class="space-y-2 mb-4">
          <label class="flex cursor-pointer items-center gap-3 rounded-lg border p-3" :class="selectedBeneficiary === 'self' ? 'border-indigo-600 bg-indigo-50' : ''">
            <input type="radio" v-model="selectedBeneficiary" value="self" class="h-4 w-4 text-indigo-600" />
            <div>
              <p class="font-medium text-gray-900">Para mi</p>
              <p class="text-xs text-gray-500">{{ currentUser?.fullName || currentUser?.email }}</p>
            </div>
          </label>
          <label v-for="assoc in associates" :key="assoc.id" class="flex cursor-pointer items-center gap-3 rounded-lg border p-3" :class="selectedBeneficiary === assoc.id ? 'border-indigo-600 bg-indigo-50' : ''">
            <input type="radio" v-model="selectedBeneficiary" :value="assoc.id" class="h-4 w-4 text-indigo-600" />
            <div>
              <p class="font-medium text-gray-900">{{ assoc.name }}</p>
              <p class="text-xs text-gray-500">{{ assoc.relation || 'Asociado' }}</p>
            </div>
          </label>
        </div>

        <div v-if="associates.length === 0" class="mb-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-700">
          No tienes asociados vinculados. <router-link to="/associates" class="font-medium text-indigo-600 hover:underline">Vincular asociados</router-link>
        </div>

        <div class="flex justify-end gap-2">
          <button @click="showBeneficiaryModal = false" class="rounded-md border px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">Cancelar</button>
          <button @click="confirmAddToCart" :disabled="!selectedBeneficiary" class="rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50">Agregar</button>
        </div>
      </div>
    </div>

    <Transition enter-active-class="transform ease-out duration-300 transition"
                enter-from-class="translate-y-2 opacity-0"
                enter-to-class="translate-y-0 opacity-100"
                leave-active-class="transition ease-in duration-200"
                leave-from-class="opacity-100"
                leave-to-class="opacity-0">
      <div v-if="showToast" 
           class="fixed bottom-4 right-4 z-50 bg-green-500 text-white px-6 py-4 rounded-lg shadow-lg flex items-center space-x-3">
        <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        <div>
          <p class="font-semibold">¡Agregado al carrito!</p>
          <p class="text-sm text-green-100">{{ toastMessage }}</p>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { classService } from '../services/classService'
import { cartService } from '../services/cartService'
import { associateService } from '../services/associateService'
import { useAuth } from '../hooks/useAuth'

const router = useRouter()
const { isAuthenticated, user: currentUser } = useAuth()
const classes = ref([])
const isLoading = ref(true)
const showToast = ref(false)
const toastMessage = ref('')

const showBeneficiaryModal = ref(false)
const selectedClass = ref(null)
const selectedBeneficiary = ref('self')
const associates = ref([])

onMounted(async () => {
  try {
    classes.value = await classService.listPublished()
  } catch (error) {
    console.error('Error loading classes:', error)
  } finally {
    isLoading.value = false
  }
})

const formatDate = (dateString) => {
  if (!dateString) return 'Fecha por confirmar'
  const date = new Date(dateString)
  return date.toLocaleDateString('es-CL', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const openBeneficiaryModal = async (cls) => {
  if (!isAuthenticated.value) {
    router.push({ name: 'Login', query: { redirect: '/classes' } })
    return
  }
  selectedClass.value = cls
  selectedBeneficiary.value = 'self'
  try {
    associates.value = await associateService.listAssociates()
  } catch (e) {
    associates.value = []
  }
  showBeneficiaryModal.value = true
}

const confirmAddToCart = async () => {
  if (!selectedClass.value || !selectedBeneficiary.value) return

  const isSelf = selectedBeneficiary.value === 'self'
  const beneficiaryType = isSelf ? 'USER' : 'ASSOCIATE'
  const beneficiaryId = isSelf ? currentUser.value?.id : selectedBeneficiary.value

  try {
    await cartService.addToCart({
      classId: selectedClass.value.id,
      beneficiaryType,
      beneficiaryId
    })
    
    toastMessage.value = `${selectedClass.value.title} para ${isSelf ? 'ti' : associates.value.find(a => a.id === selectedBeneficiary.value)?.name || 'asociado'}`
    showToast.value = true
    setTimeout(() => { showToast.value = false }, 3000)
  } catch (error) {
    toastMessage.value = error.response?.data?.message || 'Error al agregar al carrito'
    showToast.value = true
    setTimeout(() => { showToast.value = false }, 3000)
  } finally {
    showBeneficiaryModal.value = false
    selectedClass.value = null
  }
}
</script>
