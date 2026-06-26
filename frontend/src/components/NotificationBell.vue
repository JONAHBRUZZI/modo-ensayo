<template>
  <div class="relative" ref="bellRef">
    <!-- Botón campana -->
    <button @click="toggleMenu" aria-label="Notificaciónes"
      class="relative p-2 text-gray-400 hover:text-white rounded-lg hover:bg-white/5 transition-colors">
      <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
      </svg>

      <!-- Badge contador -->
      <span v-if="unreadCount > 0"
        class="absolute -top-0.5 -right-0.5 flex items-center justify-center min-w-[18px] h-[18px] px-1 bg-red-500 text-white text-[10px] font-bold rounded-full leading-none shadow-sm">
        {{ unreadCount > 9 ? '9+' : unreadCount }}
      </span>
    </button>

    <!-- Dropdown -->
    <Transition
      enter-active-class="transition ease-out duration-150"
      enter-from-class="opacity-0 scale-95 translate-y-1"
      enter-to-class="opacity-100 scale-100 translate-y-0"
      leave-active-class="transition ease-in duration-100"
      leave-from-class="opacity-100 scale-100 translate-y-0"
      leave-to-class="opacity-0 scale-95 translate-y-1">
      <div v-if="showMenu"
        class="absolute right-0 z-50 mt-2 w-80 bg-[#161824] border border-white/10 rounded-2xl shadow-2xl shadow-black/40 overflow-hidden">

        <!-- Cabecera -->
        <div class="flex items-center justify-between px-4 py-3 border-b border-white/5">
          <div class="flex items-center gap-2">
            <h3 class="text-sm font-semibold text-white">Notificaciónes</h3>
            <span v-if="unreadCount > 0"
              class="text-[10px] font-bold bg-red-500 text-white px-1.5 py-0.5 rounded-full leading-none">
              {{ unreadCount }}
            </span>
          </div>
          <div class="flex items-center gap-3">
            <button v-if="unreadCount > 0" @click="markAllRead"
              class="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
              Todo leído
            </button>
            <router-link to="/notificaciones" @click="showMenu = false"
              class="text-xs text-gray-500 hover:text-gray-300 transition-colors">
              Ver todo →
            </router-link>
          </div>
        </div>

        <!-- Lista -->
        <div class="max-h-72 overflow-y-auto">
          <div v-if="notifs.length === 0" class="py-10 text-center">
            <div class="inline-flex items-center justify-center w-10 h-10 bg-white/5 rounded-full mb-3">
              <svg class="w-5 h-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
            </div>
            <p class="text-xs text-gray-500">No tienes notificaciónes nuevas.</p>
          </div>

          <ul v-else class="divide-y divide-white/5">
            <li v-for="n in notifs" :key="n.id"
              @click="markRead(n.id)"
              class="flex items-start gap-3 px-4 py-3 cursor-pointer transition-colors hover:bg-white/3"
              :class="!n.read ? 'bg-indigo-500/5' : ''">

              <!-- Icono tipo -->
              <div :class="iconConfig(n.type).bg"
                class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                <svg :class="iconConfig(n.type).color" class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="iconConfig(n.type).path" />
                </svg>
              </div>

              <!-- Contenido -->
              <div class="flex-1 min-w-0">
                <div class="flex items-start justify-between gap-1">
                  <p class="text-xs font-semibold text-white leading-tight">{{ n.title }}</p>
                  <span v-if="!n.read" class="w-1.5 h-1.5 bg-indigo-400 rounded-full flex-shrink-0 mt-1"></span>
                </div>
                <p class="text-xs text-gray-400 mt-0.5 leading-relaxed line-clamp-2">{{ n.message }}</p>
                <p class="text-[10px] text-gray-600 mt-1">{{ formatTime(n.createdAt) }}</p>
              </div>
            </li>
          </ul>
        </div>

        <!-- Pie -->
        <div class="border-t border-white/5 px-4 py-2.5">
          <router-link to="/notificaciones" @click="showMenu = false"
            class="flex items-center justify-center gap-1.5 text-xs text-gray-500 hover:text-indigo-400 transition-colors">
            Ver todas las notificaciónes
          </router-link>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useNotifications } from '../hooks/useNotifications'

const { notifs, unreadCount, iconConfig, markRead, markAllRead, formatTime } = useNotifications()

const showMenu = ref(false)
const bellRef = ref(null)

const toggleMenu = () => { showMenu.value = !showMenu.value }

const handleClickOutside = (e) => {
  if (bellRef.value && !bellRef.value.contains(e.target)) {
    showMenu.value = false
  }
}

onMounted(() => document.addEventListener('mousedown', handleClickOutside))
onUnmounted(() => document.removeEventListener('mousedown', handleClickOutside))
</script>
