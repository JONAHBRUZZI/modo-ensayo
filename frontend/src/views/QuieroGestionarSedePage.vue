<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-20 text-center">
    <div
      v-motion
      :initial="{ opacity: 0, scale: 0.85 }"
      :enter="{ opacity: 1, scale: 1, transition: { duration: 450, type: 'spring', stiffness: 180 } }"
      class="w-20 h-20 bg-purple-500/15 border border-purple-500/25 rounded-2xl flex items-center justify-center mx-auto mb-6"
    >
      <svg class="w-10 h-10 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
      </svg>
    </div>

    <div
      v-motion
      :initial="{ opacity: 0, y: 20 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 420, delay: 100 } }"
    >
      <h1 class="text-4xl font-bold text-white mb-3">Gestiónar una Sede</h1>
      <p class="text-gray-400 text-lg max-w-2xl mx-auto mb-10">
        Administra tu propio espacio de ensayo. Registra salas, gestióna horarios, confirma clases y monitorea tus métricas.
      </p>
    </div>

    <div
      v-motion
      :initial="{ opacity: 0, y: 16 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 400, delay: 200 } }"
      class="card max-w-lg mx-auto text-left"
    >
      <h3 class="text-sm font-medium text-gray-500 uppercase tracking-wider mb-4">Requisitos</h3>
      <ul class="space-y-3">
        <li class="flex items-start gap-3">
          <div class="w-5 h-5 bg-green-500/20 rounded-full flex items-center justify-center shrink-0 mt-0.5">
            <svg class="w-3 h-3 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
          </div>
          <span class="text-gray-300 text-sm">Identidad verificada en la plataforma</span>
        </li>
        <li class="flex items-start gap-3">
          <div class="w-5 h-5 bg-green-500/20 rounded-full flex items-center justify-center shrink-0 mt-0.5">
            <svg class="w-3 h-3 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
          </div>
          <span class="text-gray-300 text-sm">Espacio físico con salas de ensayo</span>
        </li>
        <li class="flex items-start gap-3">
          <div class="w-5 h-5 bg-green-500/20 rounded-full flex items-center justify-center shrink-0 mt-0.5">
            <svg class="w-3 h-3 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
          </div>
          <span class="text-gray-300 text-sm">Capacidad de gestión operativa</span>
        </li>
      </ul>
      <router-link to="/sede/registro" class="btn-primary w-full text-center mt-6 block">
        Registrar mi Sede
      </router-link>
    </div>
  </div>
</template>
