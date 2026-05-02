<template>
  <div class="min-h-screen bg-gray-50">
    <nav class="bg-white shadow-sm border-b">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <router-link to="/" class="text-xl font-bold text-indigo-600">
              Modo Ensayo
            </router-link>
            <div class="ml-8 flex space-x-4">
              <router-link to="/classes" class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md hover:bg-gray-100">
                Clases
              </router-link>
              <router-link v-if="isAuthenticated" to="/cart" class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md hover:bg-gray-100">
                Carrito
              </router-link>
            </div>
          </div>

          <div class="flex items-center space-x-4">
            <template v-if="!isAuthenticated">
              <router-link to="/login" class="text-gray-600 hover:text-gray-900 px-3 py-2">
                Iniciar Sesion
              </router-link>
              <router-link to="/register" class="bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700">
                Registrarse
              </router-link>
            </template>

            <template v-else>
              <router-link to="/profile" class="text-gray-600 hover:text-gray-900 px-3 py-2 rounded-md hover:bg-gray-100">
                {{ user?.fullName || 'Mi Perfil' }}
              </router-link>
              <button @click="handleLogout" class="text-red-600 hover:text-red-800 px-3 py-2 rounded-md hover:bg-red-50">
                Cerrar Sesion
              </button>
            </template>
          </div>
        </div>
      </div>
    </nav>

    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useAuth } from '../hooks/useAuth'

const { isAuthenticated, user, logout } = useAuth()

const handleLogout = () => {
  logout()
}
</script>
