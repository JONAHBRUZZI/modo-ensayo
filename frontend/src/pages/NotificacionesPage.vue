<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Notificaciones</h1>
        <p class="text-gray-400 text-sm mt-0.5">Mantente al día con tu actividad</p>
      </div>
      <button v-if="unreadCount > 0" @click="markAllRead"
        class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
        Marcar todo como leído
      </button>
    </div>

    <!-- Tabs -->
    <div class="flex gap-1 bg-[#161824] rounded-xl border border-white/10 p-1 w-fit">
      <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
        :class="activeTab === tab.key ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'"
        class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center gap-1.5">
        {{ tab.label }}
        <span v-if="tab.badge" class="bg-red-500 text-white text-xs rounded-full px-1.5 py-0.5 leading-none">
          {{ tab.badge }}
        </span>
      </button>
    </div>

    <!-- Lista -->
    <div class="space-y-2">
      <template v-if="filteredNotifs.length">
        <div v-for="n in filteredNotifs" :key="n.id"
          @click="markRead(n.id)"
          class="bg-[#161824] rounded-xl border p-4 cursor-pointer transition-all hover:border-white/20"
          :class="n.read ? 'border-white/5' : 'border-indigo-500/30 bg-indigo-500/[0.03]'">
          <div class="flex items-start gap-3">
            <div :class="iconConfig(n.type).bg"
              class="w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0">
              <svg :class="iconConfig(n.type).color" class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="iconConfig(n.type).path" />
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-start justify-between gap-2">
                <p class="text-sm font-semibold text-white">{{ n.title }}</p>
                <span v-if="!n.read" class="w-2 h-2 bg-indigo-400 rounded-full flex-shrink-0 mt-1.5"></span>
              </div>
              <p class="text-xs text-gray-400 mt-0.5 leading-relaxed">{{ n.message }}</p>
              <p class="text-xs text-gray-600 mt-1.5">{{ formatTime(n.createdAt) }}</p>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="text-center py-14 bg-[#161824] rounded-xl border border-white/10">
        <div class="inline-flex items-center justify-center w-14 h-14 bg-white/5 rounded-full border border-white/10 mb-3">
          <svg class="w-6 h-6 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
        </div>
        <p class="text-gray-400 text-sm">No tienes notificaciones aquí</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useNotifications } from '../hooks/useNotifications'

const { notifs, unreadCount, iconConfig, markRead, markAllRead, formatTime } = useNotifications()

const activeTab = ref('todas')

const tabs = computed(() => [
  { key: 'todas',     label: 'Todas' },
  { key: 'no-leidas', label: 'No leídas', badge: unreadCount.value || null },
  { key: 'sistema',   label: 'Sistema' },
])

const filteredNotifs = computed(() => {
  if (activeTab.value === 'no-leidas') return notifs.value.filter(n => !n.read)
  if (activeTab.value === 'sistema')   return notifs.value.filter(n => n.type === 'sistema')
  return notifs.value
})
</script>
