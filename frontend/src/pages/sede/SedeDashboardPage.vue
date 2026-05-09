<template>
  <div class="space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Panel de Sede</h1>
      <p class="text-gray-400 text-sm mt-0.5">Operación y control de tu infraestructura artística</p>
    </div>

    <!-- CTA urgente: clases por confirmar -->
    <router-link v-if="pendientes > 0" to="/sede/clases-por-confirmar"
      class="flex items-center gap-4 p-4 bg-amber-500/8 border border-amber-500/25 rounded-xl hover:border-amber-500/40 transition-all group">
      <div class="w-10 h-10 rounded-lg bg-amber-500/15 flex items-center justify-center flex-shrink-0">
        <svg class="w-5 h-5 text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <div class="flex-1 min-w-0">
        <p class="text-sm font-semibold text-amber-300 group-hover:text-amber-200 transition-colors">
          {{ pendientes }} clase{{ pendientes !== 1 ? 's' : '' }} pendiente{{ pendientes !== 1 ? 's' : '' }} de confirmación
        </p>
        <p class="text-xs text-amber-400/60 mt-0.5">Confírmalas para liberar los pagos a los profesores</p>
      </div>
      <svg class="w-4 h-4 text-amber-500 group-hover:translate-x-0.5 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
      </svg>
    </router-link>

    <!-- Stats -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="s in stats" :key="s.label" class="bg-[#161824] rounded-xl border border-white/10 p-4">
        <div :class="s.iconBg" class="w-9 h-9 rounded-lg flex items-center justify-center mb-3">
          <svg :class="s.iconColor" class="w-4.5 h-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="s.icon" />
          </svg>
        </div>
        <p class="text-2xl font-bold text-white">{{ loading ? '—' : s.value }}</p>
        <p class="text-xs text-gray-500 mt-0.5">{{ s.label }}</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Estado de salas -->
      <div class="bg-[#161824] rounded-xl border border-white/10">
        <div class="px-5 py-4 border-b border-white/5 flex items-center justify-between">
          <h2 class="font-semibold text-white text-sm">Estado de Salas</h2>
          <router-link to="/sede/salas" class="text-xs text-emerald-400 hover:text-emerald-300 transition-colors">Ver agenda</router-link>
        </div>
        <div class="divide-y divide-white/5">
          <div v-for="sala in salas" :key="sala.id" class="px-5 py-3 flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-white">{{ sala.name }}</p>
              <p class="text-xs text-gray-500">Cap. {{ sala.capacity }}</p>
            </div>
            <EstadoBadge :estado="sala.estado" />
          </div>
        </div>
      </div>

      <!-- Módulos del panel -->
      <div class="space-y-2">
        <router-link v-for="link in modulos" :key="link.to" :to="link.to"
          class="flex items-center gap-3 p-4 bg-[#161824] border border-white/10 hover:border-white/20 rounded-xl transition-all group">
          <div :class="link.iconBg" class="w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0">
            <svg :class="link.iconColor" class="w-4.5 h-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="link.icon" />
            </svg>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-white group-hover:text-emerald-300 transition-colors">{{ link.label }}</p>
            <p class="text-xs text-gray-500">{{ link.desc }}</p>
          </div>
          <span v-if="link.badge" class="text-xs px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-400 flex-shrink-0">
            {{ link.badge }}
          </span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { venueService } from '../../services/venueService'
import EstadoBadge from '../../components/EstadoBadge.vue'

const loading = ref(true)
const pendientes = ref(0)

onMounted(async () => {
  try {
    const clases = await venueService.getClassesPendingConfirmation()
    pendientes.value = clases.length
  } catch { /* ignore */ }
  finally { loading.value = false }
})

const stats = [
  { label: 'Salas totales', value: '4', icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4', iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400' },
  { label: 'En uso ahora', value: '2', icon: 'M13 10V3L4 14h7v7l9-11h-7z', iconBg: 'bg-indigo-500/20', iconColor: 'text-indigo-400' },
  { label: 'Clases hoy', value: '6', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z', iconBg: 'bg-purple-500/20', iconColor: 'text-purple-400' },
  { label: 'Ingresos mes', value: '$485k', icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z', iconBg: 'bg-amber-500/20', iconColor: 'text-amber-400' },
]

const salas = [
  { id: 1, name: 'Sala Principal', capacity: 30, estado: 'EN_CURSO' },
  { id: 2, name: 'Sala Pequeña', capacity: 12, estado: 'ACTIVA' },
  { id: 3, name: 'Sala Multiuso', capacity: 20, estado: 'PROXIMA' },
  { id: 4, name: 'Sala Exterior', capacity: 50, estado: 'ACTIVA' },
]

const modulos = [
  { to: '/sede/clases-por-confirmar', label: 'Clases por confirmar', desc: 'Valida clases y libera pagos', icon: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z', iconBg: 'bg-amber-500/20', iconColor: 'text-amber-400' },
  { to: '/sede/mis-clases', label: 'Gestión de clases', desc: 'Ver todas las clases programadas', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2', iconBg: 'bg-indigo-500/20', iconColor: 'text-indigo-400' },
  { to: '/sede/salas', label: 'Agenda de salas', desc: 'Disponibilidad y bloques horarios', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z', iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400' },
  { to: '/sede/profesores', label: 'Profesores asociados', desc: 'Instructores vinculados a la sede', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z', iconBg: 'bg-purple-500/20', iconColor: 'text-purple-400' },
  { to: '/sede/metricas', label: 'Métricas', desc: 'Ocupación, ingresos y horarios', icon: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z', iconBg: 'bg-cyan-500/20', iconColor: 'text-cyan-400' },
  { to: '/sede/configuracion', label: 'Configuración', desc: 'Editar info, redes y documentación', icon: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z', iconBg: 'bg-gray-500/20', iconColor: 'text-gray-400' },
]
</script>
