<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8 text-center">
      <div class="bg-white p-8 rounded-lg shadow-md">
        <div class="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-red-100 mb-4">
          <svg class="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
        <h2 class="text-3xl font-extrabold text-gray-900 mb-2">Pago Cancelado</h2>
        <p class="text-gray-600 mb-6">El pago no se completó. No te preocupes, no se ha realizado ningún cargo.</p>
        
        <div class="space-y-3">
          <router-link to="/cart" 
                       class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700">
            Intentar nuevamente
          </router-link>
          <router-link to="/classes" 
                       class="w-full flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">
            Ver otras clases
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>
