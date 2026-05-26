<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
    <div class="text-center">
      <h1 class="text-5xl md:text-6xl font-bold text-white mb-6">
        Encuentra tu <span class="text-primary">espacio</span> perfecto
      </h1>
      <p class="text-xl text-gray-400 max-w-3xl mx-auto mb-10">
        La plataforma que conecta alumnos, profesores y salas de ensayo. 
        Reserva, gestiona y crece con Modo Ensayo.
      </p>
      <div class="flex flex-col sm:flex-row items-center justify-center space-y-4 sm:space-y-0 sm:space-x-4">
        <router-link to="/classes" class="btn-primary text-lg px-8 py-4">
          Explorar Clases
        </router-link>
        <router-link to="/register" class="btn-secondary text-lg px-8 py-4">
          Crear Cuenta
        </router-link>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-8 mt-24">
      <div class="card text-center">
        <div class="w-14 h-14 bg-primary/20 rounded-2xl flex items-center justify-center mx-auto mb-4">
          <svg class="w-7 h-7 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
        </div>
        <h3 class="text-lg font-semibold text-white mb-2">Reserva Salas</h3>
        <p class="text-gray-400 text-sm">Encuentra y reserva salas de ensayo disponibles en tu ciudad con los mejores equipos.</p>
      </div>

      <div class="card text-center">
        <div class="w-14 h-14 bg-green-500/20 rounded-2xl flex items-center justify-center mx-auto mb-4">
          <svg class="w-7 h-7 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/></svg>
        </div>
        <h3 class="text-lg font-semibold text-white mb-2">Toma Clases</h3>
        <p class="text-gray-400 text-sm">Accede a clases de musica con profesores calificados en diversas disciplinas.</p>
      </div>

      <div class="card text-center">
        <div class="w-14 h-14 bg-purple-500/20 rounded-2xl flex items-center justify-center mx-auto mb-4">
          <svg class="w-7 h-7 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
        </div>
        <h3 class="text-lg font-semibold text-white mb-2">Gestiona tu Sede</h3>
        <p class="text-gray-400 text-sm">Administra salas, profesores y clases desde un solo lugar con metricas en tiempo real.</p>
      </div>
    </div>

    <div class="mt-24 text-center">
      <h2 class="text-3xl font-bold text-white mb-4">Como funciona</h2>
      <div class="grid grid-cols-1 md:grid-cols-4 gap-8 mt-12">
        <div>
          <div class="w-10 h-10 bg-primary rounded-full flex items-center justify-center mx-auto mb-3 text-white font-bold">1</div>
          <h4 class="font-medium text-white">Registrate</h4>
          <p class="text-gray-400 text-sm mt-1">Crea tu cuenta gratis</p>
        </div>
        <div>
          <div class="w-10 h-10 bg-primary rounded-full flex items-center justify-center mx-auto mb-3 text-white font-bold">2</div>
          <h4 class="font-medium text-white">Explora</h4>
          <p class="text-gray-400 text-sm mt-1">Busca clases y salas</p>
        </div>
        <div>
          <div class="w-10 h-10 bg-primary rounded-full flex items-center justify-center mx-auto mb-3 text-white font-bold">3</div>
          <h4 class="font-medium text-white">Reserva</h4>
          <p class="text-gray-400 text-sm mt-1">Paga y reserva tu espacio</p>
        </div>
        <div>
          <div class="w-10 h-10 bg-primary rounded-full flex items-center justify-center mx-auto mb-3 text-white font-bold">4</div>
          <h4 class="font-medium text-white">Disfruta</h4>
          <p class="text-gray-400 text-sm mt-1">Ensayar nunca fue tan facil</p>
        </div>
      </div>
    </div>
  </div>
</template>
