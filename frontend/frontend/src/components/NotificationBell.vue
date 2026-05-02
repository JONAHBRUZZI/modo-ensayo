<template>
  <div class="relative">
    <button
      class="relative rounded-md p-2 text-gray-600 transition-colors hover:bg-gray-100 hover:text-gray-900"
      @click="toggleMenu"
      aria-label="Notificaciones"
    >
      <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M15 17h5l-1.4-1.4A2 2 0 0118 14.2V11a6 6 0 10-12 0v3.2c0 .5-.2 1-.6 1.4L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
        />
      </svg>
      <span
        v-if="unreadCount > 0"
        class="absolute -right-0.5 -top-0.5 flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-red-500 px-1 text-xs font-bold text-white"
      >
        {{ unreadCount > 9 ? '9+' : unreadCount }}
      </span>
    </button>

    <Transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
      <div
        v-if="showMenu"
        class="absolute right-0 z-50 mt-2 w-96 rounded-xl border bg-white p-3 shadow-xl"
      >
        <div class="mb-2 flex items-center justify-between border-b pb-2">
          <h3 class="text-sm font-semibold text-gray-800">Notificaciones</h3>
          <button
            v-if="notifications.length > 0"
            class="text-xs font-medium text-indigo-600 hover:text-indigo-700"
            @click="markAllRead"
          >
            Marcar todas
          </button>
        </div>

        <div v-if="loading" class="py-6 text-center text-sm text-gray-500">Cargando...</div>
        <div v-else-if="notifications.length === 0" class="py-6 text-center text-sm text-gray-500">
          No tienes notificaciones nuevas.
        </div>
        <ul v-else class="max-h-80 space-y-2 overflow-auto">
          <li v-for="item in notifications" :key="item.id" class="rounded-lg border bg-gray-50 p-3">
            <p class="text-sm text-gray-700">{{ item.message }}</p>
            <div class="mt-2 flex items-center justify-between">
              <span class="text-xs text-gray-500">{{ formatDate(item.createdAt) }}</span>
              <button class="text-xs font-medium text-indigo-600 hover:text-indigo-700" @click="markRead(item.id)">
                Marcar leida
              </button>
            </div>
          </li>
        </ul>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import rescheduleService from '../services/rescheduleService'

const showMenu = ref(false)
const loading = ref(false)
const unreadCount = ref(0)
const notifications = ref([])

onMounted(async () => {
  await refreshCount()
})

const refreshCount = async () => {
  try {
    const { data } = await rescheduleService.getUnreadCount()
    unreadCount.value = data
  } catch (error) {
    unreadCount.value = 0
  }
}

const loadNotifications = async () => {
  loading.value = true
  try {
    const { data } = await rescheduleService.getNotifications()
    notifications.value = data
  } catch (error) {
    notifications.value = []
  } finally {
    loading.value = false
  }
}

const toggleMenu = async () => {
  showMenu.value = !showMenu.value
  if (showMenu.value) {
    await loadNotifications()
  }
}

const markRead = async (id) => {
  await rescheduleService.markRead(id)
  notifications.value = notifications.value.filter((item) => item.id !== id)
  unreadCount.value = Math.max(0, unreadCount.value - 1)
}

const markAllRead = async () => {
  await rescheduleService.markAllRead()
  notifications.value = []
  unreadCount.value = 0
}

const formatDate = (isoDate) => {
  if (!isoDate) return ''
  return new Date(isoDate).toLocaleString('es-CL')
}
</script>
