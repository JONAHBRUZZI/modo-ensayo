<template>
  <div class="min-h-screen bg-[#0a0b14] text-white">
    <!-- Navbar -->
    <nav class="sticky top-0 z-40 bg-[#0a0b14]/90 backdrop-blur-md border-b border-white/5">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16 items-center">
          <!-- Logo + Nav Links -->
          <div class="flex items-center gap-8">
            <router-link to="/" class="flex items-center gap-2 font-bold text-white hover:text-gray-200 transition-colors">
              <svg class="w-5 h-5 text-yellow-400" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
              </svg>
              <span>Modo Ensayo</span>
            </router-link>

            <div class="hidden md:flex items-center gap-1">
              <router-link
                to="/"
                class="px-3 py-2 text-sm font-medium transition-colors"
                :class="$route.path === '/' ? 'text-white border-b-2 border-white pb-[6px]' : 'text-gray-400 hover:text-white'"
              >Dashboard</router-link>
              <router-link
                to="/classes"
                class="px-3 py-2 text-sm font-medium transition-colors"
                :class="$route.path === '/classes' ? 'text-white' : 'text-gray-400 hover:text-white'"
              >Cronograma</router-link>
              <router-link
                v-if="isAuthenticated"
                to="/cart"
                class="px-3 py-2 text-sm font-medium transition-colors"
                :class="$route.path === '/cart' ? 'text-white' : 'text-gray-400 hover:text-white'"
              >
                <span class="flex items-center gap-1.5">
                  Pagos
                  <span v-if="cartCount > 0" class="bg-red-500 text-white text-xs font-bold px-1.5 py-0.5 rounded-full min-w-[1.1rem] text-center leading-none">
                    {{ cartCount > 9 ? '9+' : cartCount }}
                  </span>
                </span>
              </router-link>
              <router-link
                v-if="isAuthenticated"
                to="/admin"
                class="px-3 py-2 text-sm font-medium transition-colors"
                :class="$route.path.startsWith('/admin') ? 'text-white' : 'text-gray-400 hover:text-white'"
              >Métricas</router-link>
            </div>
          </div>

          <!-- Auth -->
          <div class="flex items-center gap-3">
            <template v-if="!isAuthenticated">
              <router-link
                to="/login"
                class="bg-purple-600 hover:bg-purple-500 text-white px-4 py-2 rounded-full text-sm font-medium transition-colors shadow-sm shadow-purple-500/30"
              >Iniciar Sesión</router-link>
            </template>

            <template v-else>
              <NotificationBell />
              <div class="relative">
                <button
                  @click="showUserMenu = !showUserMenu"
                  class="flex items-center gap-2 px-3 py-2 rounded-lg text-gray-300 hover:text-white hover:bg-white/10 transition-colors"
                >
                  <div class="w-8 h-8 bg-purple-600 rounded-full flex items-center justify-center">
                    <span class="text-sm font-semibold text-white">
                      {{ user?.fullName?.charAt(0)?.toUpperCase() || 'U' }}
                    </span>
                  </div>
                  <span class="hidden sm:block text-sm font-medium">{{ user?.fullName?.split(' ')[0] || 'Usuario' }}</span>
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                <Transition
                  enter-active-class="transition ease-out duration-100"
                  enter-from-class="transform opacity-0 scale-95"
                  enter-to-class="transform opacity-100 scale-100"
                  leave-active-class="transition ease-in duration-75"
                  leave-from-class="transform opacity-100 scale-100"
                  leave-to-class="transform opacity-0 scale-95"
                >
                  <div
                    v-if="showUserMenu"
                    class="absolute right-0 mt-2 w-48 bg-[#161824] rounded-lg shadow-xl py-1 ring-1 ring-white/10 z-50"
                  >
                    <router-link to="/profile" @click="showUserMenu = false" class="block px-4 py-2 text-sm text-gray-300 hover:bg-white/5 hover:text-white transition-colors">
                      Mi Perfil
                    </router-link>
                    <button @click="handleLogout" class="block w-full text-left px-4 py-2 text-sm text-red-400 hover:bg-red-500/10 transition-colors">
                      Cerrar Sesión
                    </button>
                  </div>
                </Transition>
              </div>
            </template>
          </div>
        </div>
      </div>
    </nav>

    <!-- Main Content — no container on home, padded elsewhere -->
    <main :class="$route.path !== '/' ? 'max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8' : ''">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="border-t border-white/5 mt-auto">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-8 mb-8">
          <div class="col-span-2 md:col-span-1">
            <div class="flex items-center gap-2 font-bold text-white mb-3">
              <svg class="w-5 h-5 text-yellow-400" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
              </svg>
              Modo Ensayo
            </div>
            <p class="text-sm text-gray-500">El pulso del arte.</p>
          </div>

          <div>
            <h4 class="text-sm font-semibold text-white mb-3">Ecosistema</h4>
            <div class="space-y-2">
              <router-link to="/classes" class="block text-sm text-gray-500 hover:text-gray-300 transition-colors">Academias</router-link>
              <router-link to="/teacher/register" class="block text-sm text-gray-500 hover:text-gray-300 transition-colors">Profesores</router-link>
              <router-link to="/register" class="block text-sm text-gray-500 hover:text-gray-300 transition-colors">Familias</router-link>
            </div>
          </div>

          <div>
            <h4 class="text-sm font-semibold text-white mb-3">Legal</h4>
            <div class="space-y-2">
              <a href="#" class="block text-sm text-gray-500 hover:text-gray-300 transition-colors">Privacidad</a>
              <a href="#" class="block text-sm text-gray-500 hover:text-gray-300 transition-colors">Términos</a>
            </div>
          </div>

          <div>
            <h4 class="text-sm font-semibold text-white mb-3">Síguenos</h4>
            <div class="flex gap-4">
              <a href="#" class="text-gray-500 hover:text-gray-300 transition-colors">
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
                </svg>
              </a>
              <a href="#" class="text-gray-500 hover:text-gray-300 transition-colors">
                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z"/>
                </svg>
              </a>
            </div>
          </div>
        </div>

        <div class="border-t border-white/5 pt-6 text-sm text-gray-600">
          <p>© 2024 Modo Ensayo. El pulso del arte.</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../hooks/useAuth'
import { cartService } from '../services/cartService'
import NotificationBell from '../components/NotificationBell.vue'

const { isAuthenticated, user, logout } = useAuth()
const cartCount = ref(0)
const showUserMenu = ref(false)

onMounted(async () => {
  if (isAuthenticated.value) {
    try {
      const cart = await cartService.getCart()
      cartCount.value = cart.length
    } catch {
      // ignore
    }
  }
})

const handleLogout = () => {
  showUserMenu.value = false
  logout()
}
</script>
