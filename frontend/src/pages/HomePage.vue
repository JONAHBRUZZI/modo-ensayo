<template>
  <div>
    <!-- ═══════════════════════════════ HERO ═══════════════════════════════ -->
    <section class="relative overflow-hidden px-4 sm:px-6 lg:px-8 pt-20 pb-10">
      <!-- Dot grid + glow -->
      <div class="absolute inset-0 pointer-events-none overflow-hidden">
        <div class="absolute inset-0 opacity-40"
          style="background-image: radial-gradient(circle at 1px 1px, rgba(255,255,255,0.06) 1px, transparent 0); background-size: 36px 36px;"></div>
        <div class="absolute top-1/4 left-1/3 w-[500px] h-[500px] bg-purple-700/20 rounded-full blur-3xl"></div>
        <div class="absolute top-1/3 right-1/4 w-80 h-80 bg-indigo-600/15 rounded-full blur-3xl"></div>
      </div>

      <div class="relative max-w-7xl mx-auto">
        <!-- Heading -->
        <div class="text-center max-w-3xl mx-auto mb-12">
          <h1 class="text-5xl md:text-6xl font-bold text-white leading-tight mb-6">
            Gestiona clases, salas y<br class="hidden sm:block"/>alumnos en un solo lugar.
          </h1>
          <p class="text-base md:text-lg text-gray-400 mb-10 leading-relaxed">
            El ecosistema digital que conecta a dueños de sedes, profesores y familias<br class="hidden md:block"/>
            para profesionalizar la educación artística con tecnología de vanguardia.
          </p>
          <div class="flex flex-col sm:flex-row gap-3 justify-center">
            <router-link to="/venue-admin"
              class="px-6 py-3 rounded-lg border border-white/20 text-white text-sm font-medium hover:bg-white/10 transition-colors">
              Gestionar mi sede
            </router-link>
            <router-link to="/teacher/classes"
              class="px-6 py-3 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-white text-sm font-semibold transition-colors shadow-lg shadow-cyan-500/25">
              Crear mis clases
            </router-link>
            <router-link to="/classes"
              class="px-6 py-3 rounded-lg border border-white/20 text-white text-sm font-medium hover:bg-white/10 transition-colors">
              Encontrar clases
            </router-link>
          </div>
        </div>

        <!-- Dashboard mockup -->
        <div class="relative mx-auto max-w-2xl h-72 hidden md:block">
          <!-- Glow -->
          <div class="absolute inset-0 bg-purple-500/5 rounded-3xl blur-2xl"></div>

          <!-- Smart Calendar — top left -->
          <div class="absolute top-0 left-6 bg-[#161824] border border-white/10 rounded-xl p-3 w-44 shadow-xl">
            <div class="flex items-center gap-2 mb-2">
              <div class="w-3 h-3 bg-indigo-400 rounded-sm flex-shrink-0"></div>
              <span class="text-[10px] text-gray-400 tracking-widest font-semibold uppercase">Smart Calendar</span>
            </div>
            <div class="flex items-end gap-1 h-9">
              <div class="flex-1 bg-gray-700 rounded-sm" style="height:45%"></div>
              <div class="flex-1 bg-indigo-500 rounded-sm" style="height:90%"></div>
              <div class="flex-1 bg-gray-700 rounded-sm" style="height:35%"></div>
              <div class="flex-1 bg-gray-700 rounded-sm" style="height:65%"></div>
              <div class="flex-1 bg-indigo-400 rounded-sm" style="height:55%"></div>
            </div>
          </div>

          <!-- Revenue — top right -->
          <div class="absolute top-0 right-6 bg-[#161824] border border-white/10 rounded-xl p-4 w-38 shadow-xl">
            <p class="text-[10px] text-gray-500 uppercase tracking-wider mb-1">Ventas del mes</p>
            <p class="text-2xl font-bold text-white tracking-tight">$45.2k</p>
            <div class="mt-2 h-1 bg-gray-700 rounded-full overflow-hidden">
              <div class="h-1 bg-emerald-400 rounded-full w-3/4"></div>
            </div>
          </div>

          <!-- Hub center -->
          <div class="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-10
                      w-14 h-14 bg-indigo-600 rounded-full flex items-center justify-center
                      shadow-2xl shadow-indigo-500/40 ring-4 ring-indigo-500/20">
            <svg class="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
            </svg>
          </div>

          <!-- Teacher card — bottom left -->
          <div class="absolute bottom-0 left-6 bg-[#161824] border border-white/10 rounded-xl p-3 flex items-center gap-3 w-52 shadow-xl">
            <div class="w-9 h-9 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex-shrink-0 flex items-center justify-center text-sm font-bold text-white">M</div>
            <div>
              <p class="text-sm font-semibold text-white leading-tight">Marta Rossi</p>
              <p class="text-xs text-gray-400">Clase de Contemporáneo</p>
            </div>
          </div>

          <!-- Payment — bottom right -->
          <div class="absolute bottom-0 right-6 bg-[#161824] border border-white/10 rounded-xl p-3 w-52 shadow-xl">
            <div class="flex items-center gap-2 mb-1">
              <div class="w-2 h-2 bg-emerald-400 rounded-full"></div>
              <span class="text-xs text-emerald-400 font-semibold">Pago Recibido</span>
            </div>
            <p class="text-xs text-gray-300 mb-2">Inscripción Ballet II</p>
            <span class="text-[10px] bg-emerald-500/20 text-emerald-400 px-2 py-0.5 rounded-full border border-emerald-500/30 font-medium">
              Confirmado
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════ QUIÉN ERES ═══════════════════════════ -->
    <section class="px-4 sm:px-6 lg:px-8 py-20">
      <div class="max-w-7xl mx-auto">
        <div class="text-center mb-12">
          <h2 class="text-3xl md:text-4xl font-bold text-white mb-4">¿Quién eres en Modo Ensayo?</h2>
          <p class="text-gray-400 text-sm md:text-base">
            Una herramienta adaptativa que cambia según tus necesidades y rol dentro<br class="hidden md:block"/>del ecosistema artístico.
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <!-- Sede / Hub -->
          <div class="bg-[#161824] border border-white/10 rounded-2xl p-6 hover:border-white/20 transition-colors group">
            <div class="flex items-center justify-between mb-5">
              <div class="w-10 h-10 bg-indigo-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
                </svg>
              </div>
              <span class="text-xs text-indigo-400 font-mono tracking-wider">#SEDE</span>
            </div>
            <h3 class="text-xl font-bold text-white mb-2">Sede / Hub</h3>
            <p class="text-sm text-gray-400 mb-6 leading-relaxed">
              Administra salas, visualiza la ocupación en tiempo real y gestiona cobros de forma automatizada.
            </p>
            <!-- Mini widget -->
            <div class="bg-[#0d0f1a] rounded-xl p-4 border border-white/5">
              <div class="flex items-center justify-between mb-3">
                <span class="text-xs text-gray-500">Ocupación Salas</span>
                <span class="text-lg font-bold text-white">84%</span>
              </div>
              <div class="flex items-end gap-1.5 h-8">
                <div class="flex-1 bg-gray-700 rounded-sm" style="height:40%"></div>
                <div class="flex-1 bg-gray-700 rounded-sm" style="height:60%"></div>
                <div class="flex-1 bg-indigo-500 rounded-sm" style="height:85%"></div>
                <div class="flex-1 bg-indigo-500 rounded-sm" style="height:100%"></div>
                <div class="flex-1 bg-gray-700 rounded-sm" style="height:65%"></div>
                <div class="flex-1 bg-gray-700 rounded-sm" style="height:50%"></div>
              </div>
            </div>
          </div>

          <!-- Maestro -->
          <div class="bg-[#161824] border border-white/10 rounded-2xl p-6 hover:border-white/20 transition-colors group">
            <div class="flex items-center justify-between mb-5">
              <div class="w-10 h-10 bg-purple-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-purple-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
                </svg>
              </div>
              <span class="text-xs text-purple-400 font-mono tracking-wider">#MAESTRO</span>
            </div>
            <h3 class="text-xl font-bold text-white mb-2">Maestro</h3>
            <p class="text-sm text-gray-400 mb-6 leading-relaxed">
              Crea tu curriculum, publica tus clases y mantén una comunicación fluida con alumnos y familias.
            </p>
            <!-- Mini widget -->
            <div class="bg-[#0d0f1a] rounded-xl p-4 border border-white/5">
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 bg-purple-500/30 rounded-full flex-shrink-0 flex items-center justify-center">
                  <svg class="w-4 h-4 text-purple-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                  </svg>
                </div>
                <div>
                  <p class="text-xs font-semibold text-white">Nueva Clase de Jazz</p>
                  <p class="text-xs text-gray-500 mt-0.5">Martes 18:00 · Sala Principal</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Alumno / Familia -->
          <div class="bg-[#161824] border border-white/10 rounded-2xl p-6 hover:border-white/20 transition-colors group">
            <div class="flex items-center justify-between mb-5">
              <div class="w-10 h-10 bg-pink-500/20 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-pink-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              </div>
              <span class="text-xs text-pink-400 font-mono tracking-wider">#FAMILIA</span>
            </div>
            <h3 class="text-xl font-bold text-white mb-2">Alumno / Familia</h3>
            <p class="text-sm text-gray-400 mb-6 leading-relaxed">
              Descubre nuevas disciplinas, inscríbete fácilmente y paga tus mensualidades desde un solo panel.
            </p>
            <!-- Mini widget -->
            <div class="bg-[#0d0f1a] rounded-xl p-4 border border-white/5">
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 bg-pink-500/30 rounded-full flex-shrink-0"></div>
                <div class="flex-1">
                  <p class="text-xs font-semibold text-white">Inscripción Activa</p>
                  <p class="text-xs text-gray-500 mt-0.5">Piano Avanzado · Lunes</p>
                </div>
                <svg class="w-4 h-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════ CICLO DEL ARTE ═══════════════════════ -->
    <section class="px-4 sm:px-6 lg:px-8 py-20 border-t border-white/5">
      <div class="max-w-7xl mx-auto">
        <div class="text-center mb-14">
          <h2 class="text-3xl md:text-4xl font-bold text-white mb-4">El ciclo completo del arte</h2>
          <p class="text-gray-400 text-sm md:text-base">Desde la idea hasta la validación del aprendizaje, todo integrado.</p>
        </div>

        <div class="grid grid-cols-3 md:grid-cols-6 gap-6">
          <div class="flex flex-col items-center text-center gap-3">
            <div class="w-12 h-12 rounded-full border border-white/15 flex items-center justify-center bg-white/3">
              <svg class="w-5 h-5 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <div>
              <p class="text-sm font-semibold text-white">Buscar Sala</p>
              <p class="text-xs text-gray-500 mt-1">Filtra por zona y tipo</p>
            </div>
          </div>

          <div class="flex flex-col items-center text-center gap-3">
            <div class="w-12 h-12 rounded-full border border-white/15 flex items-center justify-center bg-white/3">
              <svg class="w-5 h-5 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
            </div>
            <div>
              <p class="text-sm font-semibold text-white">Crear Clase</p>
              <p class="text-xs text-gray-500 mt-1">Configura horarios</p>
            </div>
          </div>

          <div class="flex flex-col items-center text-center gap-3">
            <div class="w-12 h-12 rounded-full border border-white/15 flex items-center justify-center bg-white/3">
              <svg class="w-5 h-5 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
              </svg>
            </div>
            <div>
              <p class="text-sm font-semibold text-white">Inscripción</p>
              <p class="text-xs text-gray-500 mt-1">Familias reservan</p>
            </div>
          </div>

          <div class="flex flex-col items-center text-center gap-3">
            <div class="w-12 h-12 rounded-full border border-white/15 flex items-center justify-center bg-white/3">
              <svg class="w-5 h-5 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
              </svg>
            </div>
            <div>
              <p class="text-sm font-semibold text-white">Pagos</p>
              <p class="text-xs text-gray-500 mt-1">Cobros recurrentes</p>
            </div>
          </div>

          <div class="flex flex-col items-center text-center gap-3">
            <div class="w-12 h-12 rounded-full border border-white/15 flex items-center justify-center bg-white/3">
              <svg class="w-5 h-5 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
              </svg>
            </div>
            <div>
              <p class="text-sm font-semibold text-white">Asistencia</p>
              <p class="text-xs text-gray-500 mt-1">Control QR y manual</p>
            </div>
          </div>

          <div class="flex flex-col items-center text-center gap-3">
            <div class="w-12 h-12 rounded-full border border-white/15 flex items-center justify-center bg-white/3">
              <svg class="w-5 h-5 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <div>
              <p class="text-sm font-semibold text-white">Validación</p>
              <p class="text-xs text-gray-500 mt-1">Reportes de progreso</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════ ROLES DINÁMICOS ══════════════════════════ -->
    <section class="px-4 sm:px-6 lg:px-8 py-20 border-t border-white/5">
      <div class="max-w-7xl mx-auto">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <!-- Left: text -->
          <div>
            <div class="inline-flex items-center gap-2 bg-cyan-500/10 border border-cyan-500/20 rounded-full px-3 py-1 mb-6">
              <div class="w-1.5 h-1.5 bg-cyan-400 rounded-full"></div>
              <span class="text-xs text-cyan-400 font-semibold tracking-wider uppercase">Tecnología Adaptativa</span>
            </div>
            <h2 class="text-3xl md:text-4xl font-bold text-white mb-6 leading-tight">
              Roles Dinámicos e<br/>Inteligencia Operativa
            </h2>
            <p class="text-gray-400 text-sm leading-relaxed mb-8">
              Modo Ensayo entiende que el mundo artístico es fluido. Un alumno hoy puede ser profesor mañana.
              Nuestra arquitectura permite transiciones sin fricción entre perfiles manteniendo todo tu historial unificado.
            </p>
            <ul class="space-y-4">
              <li class="flex items-start gap-3">
                <div class="w-5 h-5 bg-cyan-500/20 rounded flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg class="w-3 h-3 text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <span class="text-sm text-gray-300">Agendamiento inteligente basado en disponibilidad de salas.</span>
              </li>
              <li class="flex items-start gap-3">
                <div class="w-5 h-5 bg-cyan-500/20 rounded flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg class="w-3 h-3 text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <span class="text-sm text-gray-300">Detección de conflictos de horario automática.</span>
              </li>
              <li class="flex items-start gap-3">
                <div class="w-5 h-5 bg-cyan-500/20 rounded flex items-center justify-center flex-shrink-0 mt-0.5">
                  <svg class="w-3 h-3 text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <span class="text-sm text-gray-300">Analítica predictiva de demanda para optimizar tus sedes.</span>
              </li>
            </ul>
          </div>

          <!-- Right: panel mockup -->
          <div class="bg-[#161824] rounded-2xl border border-white/10 p-6">
            <div class="flex items-center gap-2 mb-5">
              <div class="w-2.5 h-2.5 bg-red-400/80 rounded-full"></div>
              <div class="w-2.5 h-2.5 bg-yellow-400/80 rounded-full"></div>
              <div class="w-2.5 h-2.5 bg-emerald-400/80 rounded-full"></div>
              <div class="flex items-center gap-2 ml-3">
                <svg class="w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
                </svg>
                <span class="text-sm text-gray-300 font-medium">Panel de Disponibilidad</span>
              </div>
            </div>

            <div class="space-y-3">
              <!-- Sala A — EN CURSO -->
              <div class="bg-[#0d0f1a] rounded-xl p-4 border border-white/5">
                <div class="flex items-center justify-between">
                  <div>
                    <p class="text-sm font-semibold text-white">Sala A – Jazz Moderno</p>
                    <p class="text-xs text-gray-500 mt-0.5">16:00 – 18:00 · Prof. Julieta M.</p>
                  </div>
                  <span class="text-xs bg-amber-500/15 text-amber-400 px-2.5 py-1 rounded-full border border-amber-500/25 font-semibold whitespace-nowrap">
                    EN CURSO
                  </span>
                </div>
              </div>

              <!-- Sala B — Libre -->
              <div class="bg-[#0d0f1a] rounded-xl p-4 border border-white/5">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <p class="text-sm font-semibold text-white">Sala B – Libre</p>
                    <p class="text-xs text-gray-500 mt-0.5">Disponible para reserva inmediata</p>
                  </div>
                  <button class="text-xs bg-cyan-500/15 text-cyan-400 px-3 py-1.5 rounded-lg border border-cyan-500/25 font-semibold hover:bg-cyan-500/25 transition-colors whitespace-nowrap">
                    Reservar
                  </button>
                </div>
              </div>

              <!-- Sala C — PRÓXIMO -->
              <div class="bg-[#0d0f1a] rounded-xl p-4 border border-white/5">
                <div class="flex items-center justify-between">
                  <div>
                    <p class="text-sm font-semibold text-white">Sala C – Fotografía</p>
                    <p class="text-xs text-gray-500 mt-0.5">18:30 – 20:30 · Workshop Semanal</p>
                  </div>
                  <span class="text-xs bg-indigo-500/15 text-indigo-400 px-2.5 py-1 rounded-full border border-indigo-500/25 font-semibold whitespace-nowrap">
                    PRÓXIMO
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════════════════ CTA ══════════════════════════════ -->
    <section class="px-4 sm:px-6 lg:px-8 py-20">
      <div class="max-w-7xl mx-auto">
        <div class="relative overflow-hidden bg-[#161824] border border-white/10 rounded-3xl px-8 py-20 text-center">
          <!-- Glow -->
          <div class="absolute inset-0 overflow-hidden pointer-events-none">
            <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-40 bg-purple-700/25 rounded-full blur-3xl"></div>
          </div>

          <div class="relative">
            <h2 class="text-4xl md:text-5xl font-bold text-white mb-5 leading-tight">
              ¿Listo para subir al<br/>escenario digital?
            </h2>
            <p class="text-gray-400 text-sm md:text-base mb-10">
              Únete a cientos de academias y profesionales que ya están transformando la<br class="hidden md:block"/>gestión del arte con Modo Ensayo.
            </p>
            <router-link
              to="/register"
              class="inline-flex items-center px-8 py-4 bg-purple-600 hover:bg-purple-500 text-white rounded-xl font-semibold text-sm transition-colors shadow-xl shadow-purple-600/30"
            >
              Empezar ahora
            </router-link>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../hooks/useAuth'
import { cartService } from '../services/cartService'

const { isAuthenticated } = useAuth()
const cartCount = ref(0)

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
</script>
