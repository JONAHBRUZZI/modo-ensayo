<template>
  <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-20 text-center">
    <div
      v-motion
      :initial="{ opacity: 0, scale: 0.85 }"
      :enter="{ opacity: 1, scale: 1, transition: { duration: 450, type: 'spring', stiffness: 180 } }"
      class="w-20 h-20 bg-primary/15 border border-primary/25 rounded-2xl flex items-center justify-center mx-auto mb-6"
    >
      <svg class="w-10 h-10 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
      </svg>
    </div>

    <div
      v-motion
      :initial="{ opacity: 0, y: 20 }"
      :enter="{ opacity: 1, y: 0, transition: { duration: 420, delay: 100 } }"
    >
      <h1 class="text-4xl font-bold text-white mb-3">Quiero ser Maestro</h1>
      <p class="text-gray-400 text-lg max-w-2xl mx-auto mb-10">
        Comparte tu talento. En Modo Ensayo puedes crear y gestiónar tus propias clases, conectar con alumnos y generar ingresos.
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
          <span class="text-gray-300 text-sm">Experiencia comprobable en tu disciplina</span>
        </li>
        <li class="flex items-start gap-3">
          <div class="w-5 h-5 bg-green-500/20 rounded-full flex items-center justify-center shrink-0 mt-0.5">
            <svg class="w-3 h-3 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>
          </div>
          <span class="text-gray-300 text-sm">Compromiso con la calidad educativa</span>
        </li>
      </ul>
      <router-link to="/profesor/registro" class="btn-primary w-full text-center mt-6 block">
        Registrarme como Maestro
      </router-link>
    </div>
  </div>
</template>
