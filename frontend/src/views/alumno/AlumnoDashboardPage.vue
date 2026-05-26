<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-2">Mi Espacio</h1>
    <p class="text-gray-400 mb-8">Bienvenido, {{ displayName }}</p>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
      <div class="card">
        <h3 class="text-gray-400 text-sm mb-1">Clases Tomadas</h3>
        <p class="text-3xl font-bold text-white">{{ stats.totalClases || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-400 text-sm mb-1">Proximas Clases</h3>
        <p class="text-3xl font-bold text-primary">{{ stats.proximas || 0 }}</p>
      </div>
      <div class="card">
        <h3 class="text-gray-400 text-sm mb-1">Identidad</h3>
        <EstadoBadge :status="identidadEstado" />
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <router-link to="/alumno/mis-clases" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Mis Clases</h3>
        <p class="text-gray-400 text-sm mt-2">Revisa las clases en las que estas inscrito y su estado.</p>
      </router-link>
      <router-link to="/alumno/pagos" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Historial de Pagos</h3>
        <p class="text-gray-400 text-sm mt-2">Revisa tu historial de pagos y comprobantes.</p>
      </router-link>
      <router-link to="/alumno/crear-clase" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Crear Clase</h3>
        <p class="text-gray-400 text-sm mt-2">Organiza una nueva clase con un profesor.</p>
      </router-link>
      <router-link to="/alumno/asociados" class="card hover:border-primary/50 transition-colors group">
        <h3 class="text-lg font-semibold text-white group-hover:text-primary transition-colors">Asociados</h3>
        <p class="text-gray-400 text-sm mt-2">Gestiona tus asociados para clases grupales.</p>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuth } from '@/stores/auth'
import EstadoBadge from '@/components/EstadoBadge.vue'

const { displayName, identidadValidada, identidadEnRevision, identidadRechazada } = useAuth()

const identidadEstado = computed(() => {
  if (identidadValidada.value) return 'APPROVED'
  if (identidadEnRevision.value) return 'PENDING'
  if (identidadRechazada.value) return 'REJECTED'
  return 'NO_SOLICITADA'
})

const stats = ref({
  totalClases: 0,
  proximas: 0
})
</script>
