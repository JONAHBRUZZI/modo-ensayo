<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Navbar -->
    <nav class="bg-white shadow-sm border-b sticky top-0 z-40">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <!-- Left Side - Logo & Navigation -->
          <div class="flex items-center">
            <router-link to="/" class="flex items-center space-x-2 text-xl font-bold text-indigo-600 hover:text-indigo-700 transition-colors">
              <div class="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>
              </div>
              <span>Modo Ensayo</span>
            </router-link>
            
            <div class="ml-8 hidden md:flex space-x-1">
              <router-link to="/classes" 
                           class="px-3 py-2 rounded-md text-sm font-medium transition-colors"
                           :class="$route.path === '/classes' ? 'text-indigo-600 bg-indigo-50' : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'">
                Clases
              </router-link>
              <router-link v-if="isAuthenticated" to="/cart" 
                           class="relative px-3 py-2 rounded-md text-sm font-medium transition-colors"
                           :class="$route.path === '/cart' ? 'text-indigo-600 bg-indigo-50' : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'">
                <span class="flex items-center">
                  <svg class="w-5 h-5 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                  Carrito
                  <span v-if="cartCount > 0" class="ml-1.5 bg-red-500 text-white text-xs font-bold px-1.5 py-0.5 rounded-full min-w-[1.25rem] text-center">
                    {{ cartCount > 9 ? '9+' : cartCount }}
                  </span>
                </span>
              </router-link>
            </div>
          </div>

          <!-- Right Side - Auth -->
          <div class="flex items-center space-x-3">
            <template v-if="!isAuthenticated">
              <router-link to="/login" class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md text-sm font-medium transition-colors">
                Iniciar Sesión
              </router-link>
              <router-link to="/register" class="bg-indigo-600 text-white px-4 py-2 rounded-md text-sm font-medium hover:bg-indigo-700 transition-colors shadow-sm hover:shadow-md">
                Registrarse
              </router-link>
            </template>

            <template v-else>
              <NotificationBell />
              <!-- User Dropdown -->
              <div class="relative">
                <button @click="showUserMenu = !showUserMenu" 
                        class="flex items-center space-x-2 px-3 py-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors">
                  <div class="w-8 h-8 bg-indigo-100 rounded-full flex items-center justify-center">
                    <span class="text-sm font-semibold text-indigo-600">
                      {{ user?.fullName?.charAt(0)?.toUpperCase() || 'U' }}
                    </span>
                  </div>
                  <span class="hidden sm:block text-sm font-medium">{{ user?.fullName?.split(' ')[0] || 'Usuario' }}</span>
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
                
                <!-- Dropdown Menu -->
                <Transition enter-active-class="transition ease-out duration-100" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-75" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
                  <div v-if="showUserMenu" class="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg py-1 ring-1 ring-black ring-opacity-5 z-50">
                    <router-link to="/profile" @click="showUserMenu = false" class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                      Mi Perfil
                    </router-link>
                    <button @click="handleLogout" class="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50">
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

    <!-- Mobile Navigation -->
    <div class="md:hidden bg-white border-t">
      <div class="flex justify-around py-2">
        <router-link to="/" class="flex flex-col items-center p-2 text-xs" :class="$route.path === '/' ? 'text-indigo-600' : 'text-gray-600'">
          <svg class="w-6 h-6 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          Inicio
        </router-link>
        <router-link to="/classes" class="flex flex-col items-center p-2 text-xs" :class="$route.path === '/classes' ? 'text-indigo-600' : 'text-gray-600'">
          <svg class="w-6 h-6 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
          Clases
        </router-link>
        <router-link v-if="isAuthenticated" to="/cart" class="flex flex-col items-center p-2 text-xs relative" :class="$route.path === '/cart' ? 'text-indigo-600' : 'text-gray-600'">
          <div class="relative">
            <svg class="w-6 h-6 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <span v-if="cartCount > 0" class="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold w-4 h-4 rounded-full flex items-center justify-center">
              {{ cartCount > 9 ? '9+' : cartCount }}
            </span>
          </div>
          Carrito
        </router-link>
        <router-link v-if="isAuthenticated" to="/profile" class="flex flex-col items-center p-2 text-xs" :class="$route.path === '/profile' ? 'text-indigo-600' : 'text-gray-600'">
          <svg class="w-6 h-6 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
          Perfil
        </router-link>
      </div>
    </div>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="bg-white border-t mt-auto">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
          <div>
            <h4 class="text-sm font-semibold text-gray-900 mb-2">Alumnos</h4>
            <div class="space-y-1">
              <router-link to="/classes" class="block text-sm text-gray-500 hover:text-gray-700">Explorar Clases</router-link>
              <router-link v-if="isAuthenticated" to="/cart" class="block text-sm text-gray-500 hover:text-gray-700">Mi Carrito</router-link>
              <router-link v-if="isAuthenticated" to="/profile" class="block text-sm text-gray-500 hover:text-gray-700">Mi Perfil</router-link>
              <router-link v-if="isAuthenticated" to="/reviews" class="block text-sm text-gray-500 hover:text-gray-700">Mis Evaluaciones</router-link>
            </div>
          </div>
          <div>
            <h4 class="text-sm font-semibold text-gray-900 mb-2">Profesores</h4>
            <div class="space-y-1">
              <router-link v-if="isAuthenticated" to="/classes" class="block text-sm text-gray-500 hover:text-gray-700">Gestionar Clases</router-link>
              <router-link v-if="isAuthenticated" to="/reviews" class="block text-sm text-gray-500 hover:text-gray-700">Evaluar Alumnos</router-link>
            </div>
          </div>
          <div>
            <h4 class="text-sm font-semibold text-gray-900 mb-2">Directores de Sede</h4>
            <div class="space-y-1">
              <router-link v-if="isAuthenticated" to="/venue-admin" class="block text-sm text-gray-500 hover:text-gray-700">Mis Sedes</router-link>
              <router-link v-if="isAuthenticated" to="/venues/register" class="block text-sm text-gray-500 hover:text-gray-700">Registrar Sede</router-link>
              <router-link v-if="isAuthenticated" to="/rooms/register" class="block text-sm text-gray-500 hover:text-gray-700">Registrar Sala</router-link>
            </div>
          </div>
          <div>
            <h4 class="text-sm font-semibold text-gray-900 mb-2">Administracion</h4>
            <div class="space-y-1">
              <router-link v-if="isAuthenticated" to="/admin" class="block text-sm text-gray-500 hover:text-gray-700">Panel Admin</router-link>
              <router-link v-if="isAuthenticated" to="/admin/roles" class="block text-sm text-gray-500 hover:text-gray-700">Gestionar Roles</router-link>
              <router-link v-if="isAuthenticated" to="/associates" class="block text-sm text-gray-500 hover:text-gray-700">Familiares</router-link>
            </div>
          </div>
        </div>
        <div class="border-t pt-4 flex flex-col md:flex-row items-center justify-between text-sm text-gray-500">
          <p>&copy; 2025 Modo Ensayo. Todos los derechos reservados.</p>
          <div class="flex space-x-4 mt-2 md:mt-0">
            <router-link to="/classes" class="hover:text-gray-700">Clases</router-link>
            <router-link v-if="isAuthenticated" to="/cart" class="hover:text-gray-700">Carrito</router-link>
          </div>
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
    } catch (error) {
      console.error('Error loading cart count:', error)
    }
  }
})

const handleLogout = () => {
  showUserMenu.value = false
  logout()
}
</script>
