<template>
  <div class="max-w-4xl mx-auto space-y-6" v-if="!loading">
    <!-- Breadcrumb -->
    <div class="flex items-center gap-2 text-sm text-gray-500">
      <router-link to="/classes" class="hover:text-gray-300 transition-colors">Clases</router-link>
      <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" /></svg>
      <span class="text-gray-300 truncate max-w-[200px]">{{ clase?.title }}</span>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Detalle -->
      <div class="lg:col-span-2 space-y-4">
        <!-- Header card -->
        <div class="bg-[#161824] rounded-xl border border-white/10 overflow-hidden">
          <div class="h-28 bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600"></div>
          <div class="p-6">
            <span class="inline-block px-2.5 py-0.5 bg-indigo-500/20 text-indigo-400 text-xs font-medium rounded-full border border-indigo-500/30 mb-3">
              {{ clase?.discipline || 'Arte General' }}
            </span>
            <h1 class="text-2xl font-bold text-white mb-2">{{ clase?.title }}</h1>
            <p v-if="clase?.description" class="text-gray-400 text-sm leading-relaxed">{{ clase.description }}</p>
          </div>
        </div>

        <!-- Info -->
        <div class="bg-[#161824] rounded-xl border border-white/10 p-5">
          <h2 class="font-semibold text-white text-sm mb-4">Detalles de la clase</h2>
          <div class="grid grid-cols-2 gap-4">
            <div v-for="info in infoItems" :key="info.label" class="flex items-start gap-3">
              <div class="w-8 h-8 bg-white/5 rounded-lg flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg class="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="info.icon" />
                </svg>
              </div>
              <div>
                <p class="text-xs text-gray-500 uppercase tracking-wider">{{ info.label }}</p>
                <p class="text-sm text-white font-medium mt-0.5">{{ info.value }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Maestro -->
        <div v-if="clase?.teacherName" class="bg-[#161824] rounded-xl border border-white/10 p-5">
          <h2 class="font-semibold text-white text-sm mb-4">Maestro</h2>
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-gradient-to-br from-purple-600 to-indigo-600 rounded-full flex items-center justify-center">
              <span class="text-sm font-bold text-white">{{ clase.teacherName.charAt(0) }}</span>
            </div>
            <div>
              <p class="text-white text-sm font-medium">{{ clase.teacherName }}</p>
              <p class="text-xs text-gray-500">Instructor</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Sidebar de acción -->
      <div class="space-y-4">
        <div class="bg-[#161824] rounded-xl border border-white/10 p-5 sticky top-20">
          <div class="mb-4">
            <p class="text-3xl font-bold text-white">${{ clase?.price?.toLocaleString('es-CL') }}</p>
            <p class="text-xs text-gray-500">por clase</p>
          </div>

          <!-- Cupos -->
          <div class="mb-4">
            <div class="flex justify-between text-xs text-gray-500 mb-1.5">
              <span>Cupos disponibles</span>
              <span :class="clase?.capacity <= 3 ? 'text-red-400 font-medium' : 'text-white'">{{ clase?.capacity }}</span>
            </div>
            <div class="h-1.5 bg-white/10 rounded-full overflow-hidden">
              <div class="h-full bg-indigo-500 rounded-full transition-all"
                   :style="`width: ${Math.min(100, ((clase?.totalCapacity - clase?.capacity) / (clase?.totalCapacity || 1)) * 100)}%`"></div>
            </div>
          </div>

          <button @click="openBeneficiaryModal" :disabled="!clase?.capacity"
                  class="w-full py-3 rounded-xl font-semibold text-sm transition-all"
                  :class="clase?.capacity ? 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-500/25' : 'bg-white/5 text-gray-500 cursor-not-allowed'">
            {{ clase?.capacity ? 'Agregar al Carrito' : 'Sin cupos disponibles' }}
          </button>

          <router-link to="/classes" class="mt-3 w-full flex justify-center py-2.5 border border-white/15 text-gray-300 rounded-xl text-sm hover:bg-white/5 transition-colors">
            Ver más clases
          </router-link>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="flex items-center justify-center min-h-[40vh]">
    <div class="animate-spin w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full"></div>
  </div>

  <!-- Modal beneficiario -->
  <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70" @click.self="showModal = false">
    <div class="bg-[#161824] rounded-2xl border border-white/10 p-6 w-full max-w-md mx-4 shadow-2xl">
      <h3 class="text-lg font-bold text-white mb-1">¿Para quién es la clase?</h3>
      <p class="text-sm text-gray-400 mb-4">Selecciona el beneficiario de la inscripción</p>
      <div class="space-y-2 mb-4">
        <label class="flex items-center gap-3 p-3 rounded-xl border cursor-pointer transition-colors"
               :class="selectedBeneficiary === 'self' ? 'border-indigo-500 bg-indigo-500/10' : 'border-white/10 hover:border-white/20'">
          <input type="radio" v-model="selectedBeneficiary" value="self" class="h-4 w-4 text-indigo-500" />
          <span class="text-sm text-white font-medium">Para mí</span>
        </label>
        <label v-for="a in associates" :key="a.id"
               class="flex items-center gap-3 p-3 rounded-xl border cursor-pointer transition-colors"
               :class="selectedBeneficiary === a.id ? 'border-indigo-500 bg-indigo-500/10' : 'border-white/10 hover:border-white/20'">
          <input type="radio" v-model="selectedBeneficiary" :value="a.id" class="h-4 w-4 text-indigo-500" />
          <div>
            <p class="text-sm text-white font-medium">{{ a.name }}</p>
            <p class="text-xs text-gray-500">{{ a.relation }}</p>
          </div>
        </label>
      </div>
      <div class="flex gap-2">
        <button @click="showModal = false" class="flex-1 py-2.5 border border-white/15 text-gray-300 rounded-lg text-sm hover:bg-white/5 transition-colors">Cancelar</button>
        <button @click="addToCart" :disabled="!selectedBeneficiary" class="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium disabled:opacity-50 transition-colors">Agregar</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { classService } from '../../services/classService'
import { cartService } from '../../services/cartService'
import { associateService } from '../../services/associateService'
import { useAuth } from '../../hooks/useAuth'

const route = useRoute()
const router = useRouter()
const { isAuthenticated, user } = useAuth()

const clase = ref(null)
const loading = ref(true)
const showModal = ref(false)
const selectedBeneficiary = ref('self')
const associates = ref([])

const infoItems = computed(() => clase.value ? [
  { label: 'Fecha', value: formatFecha(clase.value.startTime), icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' },
  { label: 'Hora', value: formatHora(clase.value.startTime), icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
  { label: 'Sede', value: clase.value.venueName || 'Por confirmar', icon: 'M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z' },
  { label: 'Duración', value: clase.value.duration ? `${clase.value.duration} min` : 'Por confirmar', icon: 'M13 10V3L4 14h7v7l9-11h-7z' },
] : [])

onMounted(async () => {
  const id = route.params.claseId
  try {
    const clases = await classService.listPublished()
    clase.value = clases.find(c => String(c.id) === String(id))
  } catch { /* ignore */ }
  finally { loading.value = false }
})

const openBeneficiaryModal = async () => {
  if (!isAuthenticated.value) { router.push({ name: 'Login', query: { redirect: route.fullPath } }); return }
  try { associates.value = await associateService.list() } catch { associates.value = [] }
  selectedBeneficiary.value = 'self'
  showModal.value = true
}

const addToCart = async () => {
  const isSelf = selectedBeneficiary.value === 'self'
  try {
    await cartService.addToCart({
      classId: clase.value.id,
      beneficiaryType: isSelf ? 'USER' : 'ASSOCIATE',
      beneficiaryId: isSelf ? user.value?.id : selectedBeneficiary.value,
    })
    showModal.value = false
    router.push('/cart')
  } catch (e) {
    alert(e?.response?.data?.message || 'Error al agregar al carrito')
  }
}

const formatFecha = (d) => d ? new Date(d).toLocaleDateString('es-CL', { weekday: 'long', day: 'numeric', month: 'long' }) : 'Por confirmar'
const formatHora = (d) => d ? new Date(d).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' }) : 'Por confirmar'
</script>
