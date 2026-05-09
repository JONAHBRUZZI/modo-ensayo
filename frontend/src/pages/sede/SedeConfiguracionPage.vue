<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Configuración de Sede</h1>
      <p class="text-gray-400 text-sm mt-0.5">Edita la información pública de tu espacio</p>
    </div>

    <!-- Estado de validación -->
    <div class="flex items-center gap-3 p-4 rounded-xl border"
      :class="estadoClase">
      <svg class="w-5 h-5 flex-shrink-0" :class="estadoIconColor" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="estadoIconPath" />
      </svg>
      <div>
        <p class="text-sm font-medium text-white">{{ estadoLabel }}</p>
        <p class="text-xs text-gray-500 mt-0.5">{{ estadoDesc }}</p>
      </div>
      <span class="ml-auto text-xs px-2.5 py-1 rounded-full font-medium flex-shrink-0" :class="estadoBadge">
        {{ estadoTag }}
      </span>
    </div>

    <!-- Información general -->
    <div class="bg-[#161824] rounded-xl border border-white/10 p-5 space-y-4">
      <h2 class="font-semibold text-white text-sm border-b border-white/5 pb-3">Información General</h2>

      <div>
        <label class="block text-xs font-medium text-gray-400 mb-1.5">Nombre de la sede</label>
        <input v-model="form.nombre" placeholder="Nombre público de tu sede"
          class="campo" />
      </div>

      <div>
        <label class="block text-xs font-medium text-gray-400 mb-1.5">Descripción</label>
        <textarea v-model="form.descripcion" rows="4" placeholder="Describe tu espacio, propuesta artística, disciplinas..."
          class="campo resize-none"></textarea>
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Teléfono</label>
          <input v-model="form.telefono" placeholder="+56 9 1234 5678" class="campo" />
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Email de contacto</label>
          <input v-model="form.email" type="email" placeholder="contacto@tusede.cl" class="campo" />
        </div>
      </div>
    </div>

    <!-- Redes sociales -->
    <div class="bg-[#161824] rounded-xl border border-white/10 p-5 space-y-4">
      <h2 class="font-semibold text-white text-sm border-b border-white/5 pb-3">Presencia Pública</h2>

      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Instagram</label>
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 text-sm">@</span>
            <input v-model="form.instagram" placeholder="tusede" class="campo pl-7" />
          </div>
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Facebook</label>
          <input v-model="form.facebook" placeholder="facebook.com/tusede" class="campo" />
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">TikTok</label>
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 text-sm">@</span>
            <input v-model="form.tiktok" placeholder="tusede" class="campo pl-7" />
          </div>
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Sitio web</label>
          <input v-model="form.sitioWeb" placeholder="https://tusede.cl" class="campo" />
        </div>
      </div>
    </div>

    <!-- Error / Éxito -->
    <div v-if="error" class="flex items-center gap-2 px-4 py-3 bg-red-500/10 border border-red-500/20 rounded-xl text-sm text-red-400">
      <svg class="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      {{ error }}
    </div>
    <div v-if="saved" class="flex items-center gap-2 px-4 py-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-sm text-emerald-400">
      <svg class="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
      </svg>
      Cambios guardados correctamente
    </div>

    <button @click="guardar" :disabled="saving"
      class="w-full py-3 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white rounded-xl text-sm font-semibold transition-colors">
      {{ saving ? 'Guardando...' : 'Guardar cambios' }}
    </button>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { venueService } from '../../services/venueService'

const saving = ref(false)
const error = ref('')
const saved = ref(false)

const form = ref({
  nombre: '',
  descripcion: '',
  telefono: '',
  email: '',
  instagram: '',
  facebook: '',
  tiktok: '',
  sitioWeb: '',
})

// Estado de la sede — en producción esto vendría del perfil del usuario / API
const estado = ref('APPROVED') // APPROVED | PENDING | OBSERVED

const estadoClase = computed(() => ({
  'bg-emerald-500/8 border-emerald-500/20': estado.value === 'APPROVED',
  'bg-amber-500/8 border-amber-500/20': estado.value === 'PENDING',
  'bg-red-500/8 border-red-500/20': estado.value === 'OBSERVED',
}))
const estadoIconColor = computed(() => ({
  'text-emerald-400': estado.value === 'APPROVED',
  'text-amber-400': estado.value === 'PENDING',
  'text-red-400': estado.value === 'OBSERVED',
}))
const estadoIconPath = computed(() => {
  if (estado.value === 'APPROVED') return 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'
  if (estado.value === 'PENDING') return 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z'
  return 'M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
})
const estadoLabel = computed(() => ({
  APPROVED: 'Sede aprobada',
  PENDING: 'Sede en revisión',
  OBSERVED: 'Sede observada',
}[estado.value]))
const estadoDesc = computed(() => ({
  APPROVED: 'Tu sede está activa y visible en la plataforma',
  PENDING: 'El equipo de Modo Ensayo está revisando tu solicitud (2-5 días hábiles)',
  OBSERVED: 'Hay observaciones que debes corregir para aprobar tu sede',
}[estado.value]))
const estadoBadge = computed(() => ({
  APPROVED: 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400',
  PENDING: 'bg-amber-500/10 border border-amber-500/20 text-amber-400',
  OBSERVED: 'bg-red-500/10 border border-red-500/20 text-red-400',
}[estado.value]))
const estadoTag = computed(() => ({ APPROVED: 'Aprobada', PENDING: 'Pendiente', OBSERVED: 'Observada' }[estado.value]))

const guardar = async () => {
  saving.value = true
  error.value = ''
  saved.value = false
  try {
    // TODO: conectar con endpoint PATCH /venue-admin/venues/:id cuando esté disponible
    await new Promise(r => setTimeout(r, 800))
    saved.value = true
    setTimeout(() => { saved.value = false }, 3000)
  } catch (e) {
    error.value = e?.response?.data?.message || 'No se pudieron guardar los cambios'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.campo {
  @apply w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/10 rounded-lg text-white
         placeholder-gray-600 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/50
         focus:border-emerald-500/40 transition-all;
}
</style>
