<template>
  <div>
    <div class="mb-6">
      <h1 class="text-3xl font-bold text-white mb-1">Clases Disponibles</h1>
      <p class="text-gray-400">Explora y reserva las mejores clases artísticas</p>
    </div>

    <!-- Filtros -->
    <div class="bg-[#161824] rounded-xl border border-white/10 p-4 mb-6">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        <input v-model="filters.q" @input="onFilterChange" placeholder="Buscar por nombre..."
               class="px-3 py-2 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />

        <select v-model="filters.disciplina" @change="onFilterChange"
                class="px-3 py-2 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all [color-scheme:dark] text-white">
          <option value="">Todas las disciplinas</option>
          <option v-for="d in disciplinas" :key="d" :value="d">{{ d }}</option>
        </select>

        <select v-model="filters.precioMax" @change="onFilterChange"
                class="px-3 py-2 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all [color-scheme:dark] text-white">
          <option value="">Cualquier precio</option>
          <option value="10000">Hasta $10.000</option>
          <option value="25000">Hasta $25.000</option>
          <option value="50000">Hasta $50.000</option>
        </select>

        <select v-model="filters.horario" @change="onFilterChange"
                class="px-3 py-2 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all [color-scheme:dark] text-white">
          <option value="">Cualquier horario</option>
          <option value="manana">Mañana (08:00 – 12:00)</option>
          <option value="tarde">Tarde (12:00 – 18:00)</option>
          <option value="noche">Noche (18:00 – 22:00)</option>
        </select>
      </div>

      <div v-if="hasFilters" class="flex items-center justify-between mt-3 pt-3 border-t border-white/5">
        <p class="text-xs text-gray-500">{{ filteredClasses.length }} resultado{{ filteredClasses.length !== 1 ? 's' : '' }}</p>
        <button @click="clearFilters" class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">Limpiar filtros</button>
      </div>
    </div>

    <!-- Skeleton -->
    <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="n in 6" :key="n" class="bg-[#161824] rounded-xl border border-white/10 p-6 animate-pulse">
        <div class="h-4 bg-white/10 rounded w-3/4 mb-4"></div>
        <div class="h-3 bg-white/10 rounded w-1/2 mb-2"></div>
        <div class="h-3 bg-white/10 rounded w-2/3 mb-4"></div>
        <div class="h-10 bg-white/10 rounded"></div>
      </div>
    </div>

    <!-- Grid -->
    <div v-else-if="paginatedClasses.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="cls in paginatedClasses" :key="cls.id"
           class="bg-[#161824] rounded-xl border border-white/10 hover:border-white/20 overflow-hidden group transition-all duration-300 hover:shadow-xl hover:shadow-black/30">
        <div class="h-20 bg-gradient-to-r from-indigo-600 to-purple-600 relative">
          <div class="absolute -bottom-5 left-5">
            <div class="w-10 h-10 bg-[#161824] rounded-xl border border-white/10 flex items-center justify-center">
              <svg class="w-5 h-5 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
          </div>
        </div>

        <div class="pt-7 pb-5 px-5">
          <router-link :to="`/alumno/clases/${cls.id}`">
            <h3 class="text-lg font-bold text-white mb-1.5 group-hover:text-indigo-400 transition-colors leading-tight">{{ cls.title }}</h3>
          </router-link>

          <span class="inline-block px-2.5 py-0.5 bg-indigo-500/20 text-indigo-400 text-xs font-medium rounded-full border border-indigo-500/30 mb-3">
            {{ cls.discipline || 'Arte General' }}
          </span>

          <div class="space-y-1.5 mb-4 text-sm text-gray-400">
            <div class="flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              </svg>
              {{ cls.venueName || 'Sede Principal' }}
            </div>
            <div class="flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {{ formatDate(cls.startTime) }}
            </div>
            <div class="flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <span :class="cls.capacity <= 3 ? 'text-red-400 font-medium' : ''">
                {{ cls.capacity === 0 ? 'Sin cupos' : `${cls.capacity} cupos` }}
              </span>
            </div>
          </div>

          <div class="flex items-center justify-between mb-4">
            <div>
              <span class="text-xl font-bold text-white">${{ cls.price?.toLocaleString('es-CL') }}</span>
              <span class="text-gray-500 text-xs">/clase</span>
            </div>
          </div>

          <button @click="openBeneficiaryModal(cls)" :disabled="cls.capacity === 0"
                  class="w-full py-2.5 px-4 rounded-lg font-medium transition-all duration-200 text-sm"
                  :class="cls.capacity === 0 ? 'bg-white/5 text-gray-500 cursor-not-allowed' : 'bg-indigo-600 text-white hover:bg-indigo-500 hover:shadow-lg hover:shadow-indigo-500/25'">
            {{ cls.capacity === 0 ? 'Sin cupos' : 'Agregar al Carrito' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else class="text-center py-20">
      <div class="inline-flex items-center justify-center w-16 h-16 bg-white/5 rounded-full mb-4 border border-white/10">
        <svg class="w-8 h-8 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <h3 class="text-base font-semibold text-white mb-1">No hay clases disponibles</h3>
      <p class="text-gray-500 text-sm">{{ hasFilters ? 'Prueba con otros filtros' : 'Vuelve pronto para ver nuevas clases' }}</p>
    </div>

    <!-- Paginación -->
    <div class="mt-8">
      <PaginacionControls :current-page="currentPage" :total-pages="totalPages" :total="filteredClasses.length" :per-page="perPage" @change="goToPage" />
    </div>

    <!-- Modal seleccionar beneficiario -->
    <div v-if="showBeneficiaryModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70" @click.self="showBeneficiaryModal = false">
      <div class="w-full max-w-md bg-[#161824] rounded-2xl border border-white/10 p-6 shadow-2xl mx-4">
        <h3 class="mb-1 text-lg font-bold text-white">Inscribir en "{{ selectedClass?.title }}"</h3>
        <p class="mb-4 text-sm text-gray-400">¿Para quién es esta inscripción?</p>

        <div class="space-y-2 mb-4">
          <label class="flex cursor-pointer items-center gap-3 rounded-xl border p-3 transition-colors"
                 :class="selectedBeneficiary === 'self' ? 'border-indigo-500 bg-indigo-500/10' : 'border-white/10 hover:border-white/20'">
            <input type="radio" v-model="selectedBeneficiary" value="self" class="h-4 w-4 text-indigo-500" />
            <div>
              <p class="font-medium text-white text-sm">Para mí</p>
              <p class="text-xs text-gray-400">{{ currentUser?.fullName || currentUser?.email }}</p>
            </div>
          </label>
          <label v-for="assoc in associates" :key="assoc.id"
                 class="flex cursor-pointer items-center gap-3 rounded-xl border p-3 transition-colors"
                 :class="selectedBeneficiary === assoc.id ? 'border-indigo-500 bg-indigo-500/10' : 'border-white/10 hover:border-white/20'">
            <input type="radio" v-model="selectedBeneficiary" :value="assoc.id" class="h-4 w-4 text-indigo-500" />
            <div>
              <p class="font-medium text-white text-sm">{{ assoc.name }}</p>
              <p class="text-xs text-gray-400">{{ assoc.relation || 'Asociado' }}</p>
            </div>
          </label>
        </div>

        <div class="flex gap-2">
          <button @click="showBeneficiaryModal = false" class="flex-1 py-2.5 border border-white/15 text-gray-300 rounded-lg text-sm hover:bg-white/5 transition-colors">Cancelar</button>
          <button @click="confirmAddToCart" :disabled="!selectedBeneficiary" class="flex-1 py-2.5 rounded-lg bg-indigo-600 text-sm text-white font-medium hover:bg-indigo-500 disabled:opacity-50 transition-colors">Agregar</button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <Transition enter-active-class="transition ease-out duration-300" enter-from-class="translate-y-2 opacity-0" enter-to-class="translate-y-0 opacity-100" leave-active-class="transition ease-in duration-200" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="showToast" class="fixed bottom-4 right-4 z-50 bg-emerald-600 text-white px-5 py-3.5 rounded-xl shadow-lg flex items-center gap-2.5">
        <svg class="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        <p class="text-sm font-medium">{{ toastMessage }}</p>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { classService } from '../services/classService'
import { cartService } from '../services/cartService'
import { associateService } from '../services/associateService'
import { useAuth } from '../hooks/useAuth'
import PaginacionControls from '../components/PaginacionControls.vue'

const router = useRouter()
const route = useRoute()
const { isAuthenticated, user: currentUser } = useAuth()

const classes = ref([])
const isLoading = ref(true)
const showToast = ref(false)
const toastMessage = ref('')
const showBeneficiaryModal = ref(false)
const selectedClass = ref(null)
const selectedBeneficiary = ref('self')
const associates = ref([])
const currentPage = ref(1)
const perPage = 9

const filters = ref({ q: '', disciplina: '', precioMax: '', horario: '' })

const hasFilters = computed(() => filters.value.q || filters.value.disciplina || filters.value.precioMax || filters.value.horario)

const disciplinas = computed(() => [...new Set(classes.value.map(c => c.discipline).filter(Boolean))])

const filteredClasses = computed(() => {
  return classes.value.filter(c => {
    if (filters.value.q && !c.title?.toLowerCase().includes(filters.value.q.toLowerCase())) return false
    if (filters.value.disciplina && c.discipline !== filters.value.disciplina) return false
    if (filters.value.precioMax && c.price > Number(filters.value.precioMax)) return false
    if (filters.value.horario && c.startTime) {
      const h = new Date(c.startTime).getHours()
      if (filters.value.horario === 'manana' && !(h >= 8 && h < 12)) return false
      if (filters.value.horario === 'tarde' && !(h >= 12 && h < 18)) return false
      if (filters.value.horario === 'noche' && !(h >= 18 && h < 22)) return false
    }
    return true
  })
})

const totalPages = computed(() => Math.ceil(filteredClasses.value.length / perPage))
const paginatedClasses = computed(() => {
  const start = (currentPage.value - 1) * perPage
  return filteredClasses.value.slice(start, start + perPage)
})

const onFilterChange = () => {
  currentPage.value = 1
  syncUrlParams()
}

const clearFilters = () => {
  filters.value = { q: '', disciplina: '', precioMax: '', horario: '' }
  currentPage.value = 1
  syncUrlParams()
}

const goToPage = (p) => {
  currentPage.value = p
  syncUrlParams()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const syncUrlParams = () => {
  const q = {}
  if (filters.value.q) q.q = filters.value.q
  if (filters.value.disciplina) q.disciplina = filters.value.disciplina
  if (filters.value.precioMax) q.precio = filters.value.precioMax
  if (filters.value.horario) q.horario = filters.value.horario
  if (currentPage.value > 1) q.page = currentPage.value
  router.replace({ query: q })
}

onMounted(async () => {
  // Leer params de URL
  if (route.query.q) filters.value.q = route.query.q
  if (route.query.disciplina) filters.value.disciplina = route.query.disciplina
  if (route.query.precio) filters.value.precioMax = route.query.precio
  if (route.query.horario) filters.value.horario = route.query.horario
  if (route.query.page) currentPage.value = Number(route.query.page) || 1

  try { classes.value = await classService.listPublished() } catch { /* ignore */ }
  finally { isLoading.value = false }
})

const formatDate = (d) => {
  if (!d) return 'Fecha por confirmar'
  return new Date(d).toLocaleDateString('es-CL', { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
}

const openBeneficiaryModal = async (cls) => {
  if (!isAuthenticated.value) { router.push({ name: 'Login', query: { redirect: '/classes' } }); return }
  selectedClass.value = cls
  selectedBeneficiary.value = 'self'
  try { associates.value = await associateService.listAssociates() } catch { associates.value = [] }
  showBeneficiaryModal.value = true
}

const confirmAddToCart = async () => {
  const isSelf = selectedBeneficiary.value === 'self'
  try {
    await cartService.addToCart({
      classId: selectedClass.value.id,
      beneficiaryType: isSelf ? 'USER' : 'ASSOCIATE',
      beneficiaryId: isSelf ? currentUser.value?.id : selectedBeneficiary.value,
    })
    toastMessage.value = `¡Agregado al carrito!`
    showToast.value = true
    setTimeout(() => { showToast.value = false }, 3000)
  } catch (e) {
    toastMessage.value = e?.response?.data?.message || 'Error al agregar'
    showToast.value = true
    setTimeout(() => { showToast.value = false }, 3000)
  } finally {
    showBeneficiaryModal.value = false
    selectedClass.value = null
  }
}
</script>
