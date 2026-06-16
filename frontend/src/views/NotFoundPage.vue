<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4">
    <div
      v-motion
      :initial="{ opacity: 0, y: 24 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 450 } }"
      class="text-center max-w-md"
    >
      <div class="text-8xl font-bold text-[#1e2130] mb-6 select-none">404</div>
      <div class="w-16 h-16 bg-primary/15 border border-primary/25 rounded-2xl flex items-center justify-center mx-auto mb-6">
        <svg class="w-8 h-8 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
      </div>
      <h1 class="text-2xl font-bold text-white mb-3">Página no encontrada</h1>
      <p class="text-gray-400 mb-8">La página que buscas no existe o fue movida a otra dirección.</p>
      <div class="flex flex-col sm:flex-row gap-3 justify-center">
        <router-link to="/" class="btn-primary">Volver al inicio</router-link>
        <router-link to="/classes" class="btn-secondary">Ver clases</router-link>
      </div>
    </div>
  </div>
</template>
