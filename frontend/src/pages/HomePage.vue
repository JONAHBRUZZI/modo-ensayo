<template>
  <div class="space-y-8">
    <!-- Unauthenticated Hero -->
    <div v-if="!isAuthenticated" class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-indigo-600 via-purple-600 to-pink-500 shadow-xl p-8 md:p-12 text-white">
      <div class="absolute top-0 right-0 -mt-8 -mr-8 w-40 h-40 bg-white opacity-10 rounded-full"></div>
      <div class="absolute bottom-0 left-0 -mb-8 -ml-8 w-32 h-32 bg-white opacity-10 rounded-full"></div>
      <div class="relative z-10 max-w-2xl">
        <h1 class="text-4xl md:text-5xl font-bold mb-4 leading-tight">
          Descubre tu próxima<br/>clase artística
        </h1>
        <p class="text-lg text-indigo-100 mb-8">
          Plataforma de gestión de clases artísticas con pagos condicionados. Explora, reserva y paga solo cuando la clase se realiza.
        </p>
        <div class="flex flex-col sm:flex-row space-y-3 sm:space-y-0 sm:space-x-4">
          <router-link to="/classes" class="inline-flex items-center justify-center px-6 py-3 bg-white text-indigo-600 rounded-lg font-semibold hover:bg-gray-100 transition-colors shadow-md">
            <svg class="w-5 h-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
            Explorar Clases
          </router-link>
          <router-link to="/register" class="inline-flex items-center justify-center px-6 py-3 border-2 border-white text-white rounded-lg font-semibold hover:bg-white hover:text-indigo-600 transition-colors">
            Crear Cuenta Gratis
          </router-link>
        </div>
      </div>
    </div>

    <!-- Authenticated Hero -->
    <div v-else class="bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-500 rounded-2xl shadow-xl p-8 text-white relative overflow-hidden">
      <div class="absolute top-0 right-0 -mt-4 -mr-4 w-32 h-32 bg-white opacity-10 rounded-full"></div>
      <div class="absolute bottom-0 left-0 -mb-4 -ml-4 w-24 h-24 bg-white opacity-10 rounded-full"></div>
      <div class="relative z-10">
        <h1 class="text-4xl font-bold mb-2">¡Hola, {{ user?.fullName?.split(' ')[0] || 'Usuario' }}! 👋</h1>
        <p class="text-indigo-100 text-lg">Gestiona tus clases artísticas y reserva tu próxima sesión</p>
      </div>
    </div>

    <!-- Features for Unauthenticated -->
    <div v-if="!isAuthenticated" class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="bg-white rounded-xl shadow-md p-6 border border-gray-100 text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-indigo-100 rounded-full mb-4">
          <svg class="w-8 h-8 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-2">Clases para Todos</h3>
        <p class="text-gray-500 text-sm">Ballet, danza contemporánea, música, teatro y más en 5 sedes con salas profesionales.</p>
      </div>
      <div class="bg-white rounded-xl shadow-md p-6 border border-gray-100 text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-green-100 rounded-full mb-4">
          <svg class="w-8 h-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-2">Pago Seguro</h3>
        <p class="text-gray-500 text-sm">Paga con Mercado Pago. Tu dinero queda retenido y se libera solo cuando la clase se realiza.</p>
      </div>
      <div class="bg-white rounded-xl shadow-md p-6 border border-gray-100 text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-purple-100 rounded-full mb-4">
          <svg class="w-8 h-8 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-2">Inscribe a tu Familia</h3>
        <p class="text-gray-500 text-sm">Agrega familiares y gestiona sus inscripciones desde una sola cuenta.</p>
      </div>
    </div>

    <!-- Dashboard Cards for Authenticated -->
    <div v-else class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <router-link to="/classes" class="bg-white rounded-xl shadow-md p-6 hover:shadow-lg transition-shadow duration-300 border border-gray-100">
        <div class="flex items-start justify-between mb-4">
          <div class="p-3 bg-indigo-100 rounded-lg">
            <svg class="w-6 h-6 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <span class="bg-green-100 text-green-700 text-xs font-semibold px-3 py-1 rounded-full">Activo</span>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-1">Mis Clases</h3>
        <p class="text-gray-500 text-sm">Revisa tus clases inscritas</p>
      </router-link>
      <router-link to="/cart" class="bg-white rounded-xl shadow-md p-6 hover:shadow-lg transition-shadow duration-300 border border-gray-100">
        <div class="flex items-start justify-between mb-4">
          <div class="p-3 bg-green-100 rounded-lg">
            <svg class="w-6 h-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          </div>
          <span v-if="cartCount > 0" class="bg-red-500 text-white text-xs font-bold px-2.5 py-1 rounded-full">{{ cartCount }}</span>
          <span v-else class="bg-gray-100 text-gray-600 text-xs font-semibold px-3 py-1 rounded-full">Vacío</span>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-1">Mi Carrito</h3>
        <p class="text-gray-500 text-sm">{{ cartCount > 0 ? `${cartCount} clase(s) pendiente(s)` : 'Agrega clases para reservar' }}</p>
      </router-link>
      <router-link to="/profile" class="bg-white rounded-xl shadow-md p-6 hover:shadow-lg transition-shadow duration-300 border border-gray-100">
        <div class="flex items-start justify-between mb-4">
          <div class="p-3 bg-purple-100 rounded-lg">
            <svg class="w-6 h-6 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </div>
        </div>
        <h3 class="text-lg font-semibold text-gray-900 mb-1">Mi Perfil</h3>
        <p class="text-gray-500 text-sm">Actualiza tus datos personales</p>
      </router-link>
    </div>

    <!-- CTA Section for Unauthenticated -->
    <div v-if="!isAuthenticated" class="bg-indigo-50 rounded-2xl p-8 text-center">
      <h2 class="text-2xl font-bold text-gray-900 mb-4">¿Listo para comenzar?</h2>
      <p class="text-gray-600 mb-6">Crea tu cuenta gratuita y empieza a explorar clases artísticas en las mejores sedes.</p>
      <router-link to="/register" class="inline-flex items-center px-8 py-3 bg-indigo-600 text-white rounded-lg font-semibold hover:bg-indigo-700 transition-colors shadow-md">
        Registrarme Ahora
        <svg class="w-5 h-5 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
        </svg>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../hooks/useAuth'
import { cartService } from '../services/cartService'

const { user, isAuthenticated } = useAuth()
const cartCount = ref(0)

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
</script>