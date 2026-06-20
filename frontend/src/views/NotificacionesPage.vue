<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <h1 class="text-3xl font-bold text-white mb-8">Notificaciones</h1>
    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="notifications.length === 0" class="card text-center py-16">
      <div class="w-14 h-14 bg-[var(--bg-elevated)] rounded-2xl flex items-center justify-center mx-auto mb-4">
        <svg class="w-7 h-7 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
        </svg>
      </div>
      <p class="text-gray-400 font-medium">Sin notificaciónes por ahora</p>
      <p class='text-gray-600 text-sm mt-1'>Te avisaremos cuando haya algo nuevo.</p>
    </div>
    <div v-else class="space-y-3">
      <div v-for="n in notifications" :key="n.id" :class="['card', !n.read && 'border-primary/30']">
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <h4 class="text-white font-medium">{{ n.title }}</h4>
            <p class="text-gray-400 text-sm mt-1">{{ n.message }}</p>
            <p class="text-gray-500 text-xs mt-2">{{ formatDate(n.createdAt) }}</p>
          </div>
          <span :class="['badge', n.type === 'pago' ? 'badge-green' : n.type === 'clase' ? 'badge-blue' : 'badge-yellow']">{{ n.type }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import rescheduleService from '@/services/rescheduleService'
import { formatDate } from '@/utils/dateFormatter'

const notifications = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await rescheduleService.getNotifications()
    notifications.value = Array.isArray(data) ? data : data?.content || data?.data || []
  } catch { notifications.value = [] }
  loading.value = false
})


</script>
