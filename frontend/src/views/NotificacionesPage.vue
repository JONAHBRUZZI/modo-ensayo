<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <h1 class="text-3xl font-bold text-white mb-8">Notificaciones</h1>
    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="notifications.length === 0" class="card text-center py-12"><p class="text-gray-400">No tienes notificaciones.</p></div>
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

const notifications = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const data = await rescheduleService.getNotifications()
    notifications.value = Array.isArray(data) ? data : data?.content || data?.data || []
  } catch { notifications.value = [] }
  loading.value = false
})

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
}
</script>
