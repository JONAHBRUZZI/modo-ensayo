<template>
  <div class="max-w-4xl mx-auto">
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-gray-900 mb-2">Mi Carrito</h1>
      <p class="text-gray-600">Revisa tus clases seleccionadas antes de pagar</p>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="bg-white rounded-xl shadow-md p-8">
      <div class="animate-pulse space-y-4">
        <div class="h-4 bg-gray-200 rounded w-1/4"></div>
        <div class="h-20 bg-gray-200 rounded"></div>
        <div class="h-20 bg-gray-200 rounded"></div>
      </div>
    </div>

    <!-- Cart Items -->
    <div v-else-if="cartItems.length > 0" class="space-y-6">
      <!-- Items List -->
      <div class="bg-white rounded-xl shadow-md overflow-hidden border border-gray-100">
        <div class="px-6 py-4 border-b border-gray-100 bg-gray-50">
          <h2 class="font-semibold text-gray-900">
            {{ cartItems.length }} {{ cartItems.length === 1 ? 'clase' : 'clases' }} en el carrito
          </h2>
        </div>
        
        <div class="divide-y divide-gray-100">
          <div v-for="item in cartItems" :key="item.id" class="p-6 flex items-start space-x-4 hover:bg-gray-50 transition-colors">
            <!-- Icon -->
            <div class="flex-shrink-0">
              <div class="w-16 h-16 bg-indigo-100 rounded-lg flex items-center justify-center">
                <svg class="w-8 h-8 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>
              </div>
            </div>
            
            <!-- Content -->
            <div class="flex-1">
              <div class="flex items-start justify-between">
                <div>
                  <h3 class="text-lg font-semibold text-gray-900 mb-1">
                    {{ getClassDetails(item.classId)?.title || 'Clase' }}
                  </h3>
                  <p class="text-sm text-gray-500 mb-2">
                    {{ getClassDetails(item.classId)?.discipline || 'Arte General' }}
                  </p>
                  <div class="flex items-center space-x-4 text-sm text-gray-600">
                    <span class="flex items-center">
                      <svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      </svg>
                      {{ getClassDetails(item.classId)?.venueName || 'Sede Principal' }}
                    </span>
                    <span class="flex items-center">
                      <svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                      </svg>
                      {{ item.beneficiaryType === 'USER' ? 'Para mí' : 'Familiar' }}
                    </span>
                  </div>
                </div>
                
                <!-- Price -->
                <div class="text-right">
                  <p class="text-xl font-bold text-gray-900">
                    ${{ getClassDetails(item.classId)?.price?.toLocaleString() || '0' }}
                  </p>
                </div>
              </div>
            </div>
            
            <!-- Remove Button -->
            <button @click="removeFromCart(item.id)" 
                    :disabled="isRemoving === item.id"
                    class="flex-shrink-0 p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-all"
                    title="Eliminar del carrito">
              <svg v-if="isRemoving === item.id" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <svg v-else class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>
        
        <!-- Total -->
        <div class="px-6 py-4 bg-gray-50 border-t border-gray-100">
          <div class="flex items-center justify-between">
            <span class="text-gray-600">Total a pagar</span>
            <span class="text-2xl font-bold text-gray-900">${{ totalAmount.toLocaleString() }}</span>
          </div>
        </div>
      </div>

      <!-- Payment Section -->
      <div class="bg-white rounded-xl shadow-md p-6 border border-gray-100">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center space-x-3">
            <div class="p-2 bg-blue-50 rounded-lg">
              <svg class="w-6 h-6 text-blue-600" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
              </svg>
            </div>
            <div>
              <h3 class="font-semibold text-gray-900">Pago seguro con Mercado Pago</h3>
              <p class="text-sm text-gray-500">Paga con tarjeta de crédito, débito o efectivo</p>
            </div>
          </div>
        </div>
        
        <button @click="checkout" 
                :disabled="isProcessing"
                class="w-full py-4 px-6 bg-green-600 text-white rounded-lg font-semibold text-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 flex items-center justify-center space-x-3 shadow-md hover:shadow-lg">
          <svg v-if="isProcessing" class="animate-spin h-6 w-6" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <template v-else>
            <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a1 1 0 11-2 0 1 1 0 012 0z" />
            </svg>
            <span>Pagar ${{ totalAmount.toLocaleString() }} con Mercado Pago</span>
          </template>
        </button>
        
        <p class="text-center text-sm text-gray-500 mt-3">
          Al pagar, aceptas los términos y condiciones
        </p>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-16">
      <div class="inline-flex items-center justify-center w-24 h-24 bg-gray-100 rounded-full mb-6">
        <svg class="w-12 h-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
        </svg>
      </div>
      <h3 class="text-xl font-semibold text-gray-900 mb-2">Tu carrito está vacío</h3>
      <p class="text-gray-500 mb-6 max-w-md mx-auto">Explora nuestras clases artísticas y agrega las que más te interesen</p>
      <router-link to="/classes" class="inline-flex items-center px-6 py-3 bg-indigo-600 text-white rounded-lg font-medium hover:bg-indigo-700 transition-colors">
        <svg class="w-5 h-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
        </svg>
        Explorar Clases
      </router-link>
    </div>

    <!-- Confirmation Modal -->
    <Transition enter-active-class="ease-out duration-300" enter-from-class="opacity-0" enter-to-class="opacity-100" leave-active-class="ease-in duration-200" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="showConfirmModal" class="fixed inset-0 z-50 overflow-y-auto" aria-labelledby="modal-title" role="dialog" aria-modal="true">
        <div class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
          <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" @click="showConfirmModal = false"></div>
          <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">&#8203;</span>
          <div class="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full">
            <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
              <div class="sm:flex sm:items-start">
                <div class="mx-auto flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-red-100 sm:mx-0 sm:h-10 sm:w-10">
                  <svg class="h-6 w-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                </div>
                <div class="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left">
                  <h3 class="text-lg leading-6 font-medium text-gray-900" id="modal-title">¿Eliminar del carrito?</h3>
                  <div class="mt-2">
                    <p class="text-sm text-gray-500">¿Estás seguro de que deseas eliminar esta clase del carrito? Esta acción no se puede deshacer.</p>
                  </div>
                </div>
              </div>
            </div>
            <div class="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
              <button type="button" @click="confirmRemove" class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-red-600 text-base font-medium text-white hover:bg-red-700 focus:outline-none sm:ml-3 sm:w-auto sm:text-sm">
                Eliminar
              </button>
              <button type="button" @click="showConfirmModal = false" class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm">
                Cancelar
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Toast Notification -->
    <Transition enter-active-class="transform ease-out duration-300 transition" enter-from-class="translate-y-2 opacity-0" enter-to-class="translate-y-0 opacity-100" leave-active-class="transition ease-in duration-200" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="showToast" class="fixed bottom-4 right-4 z-50 bg-red-500 text-white px-6 py-4 rounded-lg shadow-lg flex items-center space-x-3">
        <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
        <p class="font-medium">Clase eliminada del carrito</p>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { cartService } from '../services/cartService'
import { classService } from '../services/classService'

const router = useRouter()
const cartItems = ref([])
const classDetails = ref({})
const isLoading = ref(true)
const isProcessing = ref(false)
const isRemoving = ref(null)
const showToast = ref(false)
const showConfirmModal = ref(false)
const itemToRemove = ref(null)

const totalAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => {
    const details = classDetails.value[item.classId]
    return sum + (details?.price || 0)
  }, 0)
})

onMounted(async () => {
  await loadCart()
})

const loadCart = async () => {
  isLoading.value = true
  try {
    cartItems.value = await cartService.getCart()
    // Fetch class details for each item
    for (const item of cartItems.value) {
      if (!classDetails.value[item.classId]) {
        try {
          const classes = await classService.listPublished()
          const cls = classes.find(c => c.id === item.classId)
          if (cls) {
            classDetails.value[item.classId] = cls
          }
        } catch (error) {
          console.error('Error loading class details:', error)
        }
      }
    }
  } catch (error) {
    console.error('Error loading cart:', error)
  } finally {
    isLoading.value = false
  }
}

const getClassDetails = (classId) => {
  return classDetails.value[classId]
}

const removeFromCart = (itemId) => {
  itemToRemove.value = itemId
  showConfirmModal.value = true
}

const confirmRemove = async () => {
  if (!itemToRemove.value) return
  
  showConfirmModal.value = false
  isRemoving.value = itemToRemove.value
  
  try {
    await cartService.removeFromCart(itemToRemove.value)
    cartItems.value = cartItems.value.filter(item => item.id !== itemToRemove.value)
    showToast.value = true
    setTimeout(() => {
      showToast.value = false
    }, 3000)
  } catch (error) {
    console.error('Error removing item:', error)
  } finally {
    isRemoving.value = null
    itemToRemove.value = null
  }
}

const checkout = async () => {
  if (isProcessing.value) return
  
  isProcessing.value = true
  try {
    const response = await cartService.createMercadoPagoPreference()
    const paymentUrl = response.sandboxInitPoint || response.initPoint
    window.location.href = paymentUrl
  } catch (error) {
    console.error('Error al crear preferencia:', error)
    alert('Error al iniciar el pago. Intenta nuevamente.')
  } finally {
    isProcessing.value = false
  }
}
</script>