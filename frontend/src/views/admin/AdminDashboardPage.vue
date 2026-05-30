<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Panel de Administracion</h1>

    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-10">
      <router-link to="/admin/usuarios" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-400 text-sm mb-1">Usuarios</h3>
        <p class="text-3xl font-bold text-white group-hover:text-primary transition-colors">{{ stats.usuarios || 0 }}</p>
      </router-link>
      <router-link to="/admin/roles" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-400 text-sm mb-1">Sedes</h3>
        <p class="text-3xl font-bold text-primary">{{ stats.sedes || 0 }}</p>
      </router-link>
      <router-link to="/admin/roles" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-400 text-sm mb-1">Verif. Pendientes</h3>
        <p class="text-3xl font-bold text-yellow-400 group-hover:text-yellow-300 transition-colors">{{ stats.pendientes || 0 }}</p>
      </router-link>
      <router-link to="/admin/roles" class="card hover:border-primary/50 transition-colors group cursor-pointer">
        <h3 class="text-gray-400 text-sm mb-1">Sedes Pend.</h3>
        <p class="text-3xl font-bold text-red-400 group-hover:text-red-300 transition-colors">{{ stats.sedesPendientes || 0 }}</p>
      </router-link>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-10">
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Total Clases</h3><p class="text-3xl font-bold text-white">{{ stats.totalClases || 0 }}</p></div>
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Realizadas</h3><p class="text-3xl font-bold text-green-400">{{ stats.clasesRealizadas || 0 }}</p></div>
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Tasa Ocupacion</h3><p class="text-3xl font-bold text-primary">{{ stats.tasaOcupacion || 0 }}%</p></div>
      <div class="card"><h3 class="text-gray-400 text-sm mb-1">Ingresos</h3><p class="text-3xl font-bold text-yellow-400">${{ stats.ingresos?.toLocaleString() || 0 }}</p></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import adminService from '@/services/adminService'

const stats = ref({ usuarios: 0, sedes: 0, pendientes: 0, sedesPendientes: 0 })

onMounted(async () => {
  try { stats.value = await adminService.getStats() } catch { stats.value = {} }
})
</script>
