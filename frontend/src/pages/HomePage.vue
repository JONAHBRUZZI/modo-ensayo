<template>
  <div class="space-y-8">
    <div class="bg-gradient-to-r from-indigo-500 to-purple-600 rounded-lg shadow-lg p-8 text-white">
      <h1 class="text-3xl font-bold mb-2">Bienvenido, {{ user?.fullName || 'Usuario' }}</h1>
      <p class="text-indigo-100">Plataforma para gestion de clases artisticas con pagos condicionados</p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="bg-white rounded-lg shadow-md p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-gray-900">Mis Clases</h3>
          <span class="bg-indigo-100 text-indigo-800 text-xs font-medium px-2.5 py-0.5 rounded-full">Activo</span>
        </div>
        <p class="text-gray-600 text-sm mb-4">Revisa tus clases inscritas y proximas sesiones</p>
        <router-link to="/classes" class="text-indigo-600 hover:text-indigo-800 font-medium text-sm">
          Ver clases →
        </router-link>
      </div>

      <div class="bg-white rounded-lg shadow-md p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-gray-900">Mi Carrito</h3>
          <span class="bg-green-100 text-green-800 text-xs font-medium px-2.5 py-0.5 rounded-full">Pendiente</span>
        </div>
        <p class="text-gray-600 text-sm mb-4">Completa tu pago para reservar tu cupo en las clases</p>
        <router-link to="/cart" class="text-indigo-600 hover:text-indigo-800 font-medium text-sm">
          Ir al carrito →
        </router-link>
      </div>

      <div class="bg-white rounded-lg shadow-md p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-gray-900">Mi Perfil</h3>
          <span class="bg-gray-100 text-gray-800 text-xs font-medium px-2.5 py-0.5 rounded-full">Config</span>
        </div>
        <p class="text-gray-600 text-sm mb-4">Actualiza tus datos personales y familiares asociados</p>
        <router-link to="/profile" class="text-indigo-600 hover:text-indigo-800 font-medium text-sm">
          Editar perfil →
        </router-link>
      </div>
    </div>

    <div class="bg-white rounded-lg shadow-md p-6">
      <h2 class="text-xl font-semibold text-gray-900 mb-4">Clases Destacadas</h2>
      <router-link to="/classes" class="text-indigo-600 hover:text-indigo-800 font-medium">
        Ver todas las clases disponibles →
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { useAuth } from '../hooks/useAuth'

const { user } = useAuth()
</script>
