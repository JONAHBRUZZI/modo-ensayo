<template>
  <div class="max-w-2xl mx-auto space-y-6 pb-16">

    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-white">Registrar Sede</h1>
      <p class="text-gray-400 text-sm mt-0.5">Solicita la incorporación de tu espacio a Modo Ensayo</p>
    </div>

    <!-- Progreso -->
    <div v-if="tipoSede" class="bg-[#161824] rounded-xl border border-white/10 px-5 py-4">
      <div class="flex items-center justify-between mb-3">
        <span class="text-xs text-gray-500">Paso {{ currentStep }} de {{ totalSteps }}</span>
        <span class="text-xs font-medium" :class="tipoSede === 'ACADEMIA' ? 'text-purple-400' : 'text-indigo-400'">
          {{ tipoSede === 'ACADEMIA' ? 'Academia / Empresa formal' : 'HomeStudio / Espacio independiente' }}
        </span>
      </div>
      <div class="w-full bg-white/5 rounded-full h-1.5">
        <div class="h-1.5 rounded-full transition-all duration-500"
          :class="tipoSede === 'ACADEMIA' ? 'bg-purple-500' : 'bg-indigo-500'"
          :style="{ width: `${(currentStep / totalSteps) * 100}%` }" />
      </div>
      <div class="flex items-center justify-between mt-2">
        <span class="text-xs text-gray-500">{{ stepLabels[currentStep - 1] }}</span>
      </div>
    </div>

    <!-- ───────────────────────────── PASO 0: Tipo de sede ───────────────────────────── -->
    <div v-if="currentStep === 0" class="space-y-4">
      <div class="bg-[#161824] rounded-xl border border-white/10 p-5">
        <h2 class="font-semibold text-white mb-1">¿Qué tipo de espacio tienes?</h2>
        <p class="text-gray-500 text-sm mb-5">Esta selección define los documentos y requisitos que necesitarás.</p>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <button @click="seleccionarTipo('ACADEMIA')" type="button"
            class="text-left p-5 rounded-xl border-2 transition-all duration-200"
            :class="tipoSede === 'ACADEMIA'
              ? 'border-purple-500 bg-purple-500/10'
              : 'border-white/10 bg-white/3 hover:border-white/20'">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center mb-3"
              :class="tipoSede === 'ACADEMIA' ? 'bg-purple-500/20' : 'bg-white/5'">
              <svg class="w-5 h-5" :class="tipoSede === 'ACADEMIA' ? 'text-purple-400' : 'text-gray-500'"
                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1" />
              </svg>
            </div>
            <p class="font-semibold text-white text-sm">Academia / Empresa formal</p>
            <p class="text-xs text-gray-500 mt-1">Escuela de arte, estudio profesional, centro cultural, empresa con RUT activo.</p>
            <div v-if="tipoSede === 'ACADEMIA'" class="mt-3 flex items-center gap-1.5">
              <div class="w-4 h-4 rounded-full bg-purple-500 flex items-center justify-center flex-shrink-0">
                <svg class="w-2.5 h-2.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <span class="text-xs text-purple-400 font-medium">Seleccionado</span>
            </div>
          </button>

          <button @click="seleccionarTipo('HOMESTUDIO')" type="button"
            class="text-left p-5 rounded-xl border-2 transition-all duration-200"
            :class="tipoSede === 'HOMESTUDIO'
              ? 'border-indigo-500 bg-indigo-500/10'
              : 'border-white/10 bg-white/3 hover:border-white/20'">
            <div class="w-10 h-10 rounded-lg flex items-center justify-center mb-3"
              :class="tipoSede === 'HOMESTUDIO' ? 'bg-indigo-500/20' : 'bg-white/5'">
              <svg class="w-5 h-5" :class="tipoSede === 'HOMESTUDIO' ? 'text-indigo-400' : 'text-gray-500'"
                fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.75"
                  d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              </svg>
            </div>
            <p class="font-semibold text-white text-sm">HomeStudio / Espacio independiente</p>
            <p class="text-xs text-gray-500 mt-1">Espacio propio, sala adaptada, estudio casero con actividad artística real.</p>
            <div v-if="tipoSede === 'HOMESTUDIO'" class="mt-3 flex items-center gap-1.5">
              <div class="w-4 h-4 rounded-full bg-indigo-500 flex items-center justify-center flex-shrink-0">
                <svg class="w-2.5 h-2.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <span class="text-xs text-indigo-400 font-medium">Seleccionado</span>
            </div>
          </button>
        </div>
      </div>

      <button v-if="tipoSede" @click="currentStep = 1"
        class="w-full py-3 rounded-xl font-medium text-sm text-white transition-colors"
        :class="tipoSede === 'ACADEMIA' ? 'bg-purple-600 hover:bg-purple-500' : 'bg-indigo-600 hover:bg-indigo-500'">
        Continuar →
      </button>
    </div>

    <!-- ───────────────────────────── PASO 1: Información General ───────────────────────────── -->
    <div v-if="currentStep === 1" class="space-y-4">
      <SectionCard titulo="Información General" :accent="accent">
        <Campo label="Nombre de la sede" required>
          <input v-model="form.nombre" placeholder="Ej: Academia de Arte Valparaíso"
            class="campo-input" />
        </Campo>

        <Campo :label="tipoSede === 'HOMESTUDIO' ? 'Descripción del espacio' : 'Descripción de la sede'" required>
          <p v-if="tipoSede === 'HOMESTUDIO'" class="text-xs text-gray-500 mb-2">
            Explica qué actividades realizas, cómo funciona el espacio y qué tipo de clases ofreces.
          </p>
          <textarea v-model="form.descripcion" rows="4" placeholder="Describe tu espacio, actividades y propuesta artística..."
            class="campo-input resize-none" />
        </Campo>
      </SectionCard>
    </div>

    <!-- ───────────────────────────── PASO 2: Ubicación ───────────────────────────── -->
    <div v-if="currentStep === 2" class="space-y-4">
      <SectionCard titulo="Ubicación" :accent="accent">
        <div class="grid grid-cols-2 gap-4">
          <Campo label="Región" required>
            <select v-model="form.region" class="campo-input">
              <option value="" disabled>Selecciona región</option>
              <option v-for="r in regiones" :key="r" :value="r">{{ r }}</option>
            </select>
          </Campo>
          <Campo label="Comuna" required>
            <input v-model="form.comuna" placeholder="Ej: Providencia" class="campo-input" />
          </Campo>
        </div>

        <Campo label="Dirección completa" required>
          <input v-model="form.direccion" placeholder="Ej: Av. Libertador 1234, Piso 3" class="campo-input" />
        </Campo>

        <Campo label="Referencia adicional" :required="false">
          <input v-model="form.referencia" placeholder="Ej: Frente al mall, portón rojo" class="campo-input" />
        </Campo>

        <Campo label="Ubicación en mapa" required>
          <input v-model="form.mapLink" placeholder="Link de Google Maps o coordenadas" class="campo-input" />
          <p class="text-xs text-gray-600 mt-1">Puedes pegar el link de Google Maps de tu ubicación.</p>
        </Campo>
      </SectionCard>
    </div>

    <!-- ───────────────────────────── PASO 3: Contacto ───────────────────────────── -->
    <div v-if="currentStep === 3" class="space-y-4">
      <SectionCard titulo="Contacto" :accent="accent">
        <div class="grid grid-cols-2 gap-4">
          <Campo label="Teléfono" required>
            <input v-model="form.telefono" placeholder="+56 9 1234 5678" class="campo-input" />
          </Campo>
          <Campo label="Email de contacto" required>
            <input v-model="form.emailContacto" type="email" placeholder="contacto@tusede.cl" class="campo-input" />
          </Campo>
        </div>
      </SectionCard>
    </div>

    <!-- ───────────────────────────── PASO 4: Presencia Pública ───────────────────────────── -->
    <div v-if="currentStep === 4" class="space-y-4">
      <SectionCard titulo="Presencia Pública" :accent="accent">
        <div class="flex items-start gap-3 p-3 bg-amber-500/8 border border-amber-500/20 rounded-lg mb-2">
          <svg class="w-4 h-4 text-amber-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          <p class="text-xs text-amber-300">
            <span class="font-semibold">Muy recomendado para acelerar la validación.</span>
            Ayuda a demostrar actividad real y generar confianza con los alumnos.
          </p>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <Campo label="Instagram" :required="false">
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 text-sm">@</span>
              <input v-model="form.instagram" placeholder="tusede" class="campo-input pl-7" />
            </div>
          </Campo>
          <Campo label="Facebook" :required="false">
            <input v-model="form.facebook" placeholder="facebook.com/tusede" class="campo-input" />
          </Campo>
          <Campo label="TikTok" :required="false">
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 text-sm">@</span>
              <input v-model="form.tiktok" placeholder="tusede" class="campo-input pl-7" />
            </div>
          </Campo>
          <Campo v-if="tipoSede === 'HOMESTUDIO'" label="Canal YouTube" :required="false">
            <input v-model="form.youtube" placeholder="youtube.com/@tusede" class="campo-input" />
          </Campo>
          <Campo label="Sitio web" :required="false">
            <input v-model="form.sitioWeb" placeholder="https://tusede.cl" class="campo-input" />
          </Campo>
        </div>

        <p v-if="tipoSede === 'ACADEMIA'" class="text-xs text-gray-600 mt-1">
          También puedes indicar otras redes donde promocionas actividades.
        </p>
      </SectionCard>
    </div>

    <!-- ───────────────────────────── PASO 5: Documentación ───────────────────────────── -->
    <div v-if="currentStep === 5" class="space-y-4">

      <!-- ACADEMIA: al menos 1 de 6 -->
      <SectionCard v-if="tipoSede === 'ACADEMIA'" titulo="Documentación Legal" :accent="accent">
        <div class="flex items-start gap-3 p-3 bg-[#0d0f1a] border border-white/8 rounded-lg mb-4">
          <svg class="w-4 h-4 text-gray-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p class="text-xs text-gray-400">
            Para validar tu sede necesitamos documentación que acredite la operación del espacio.
            <span class="text-white font-medium"> Sube al menos uno de los documentos a continuación.</span>
          </p>
        </div>

        <div class="space-y-3">
          <DocUpload
            v-for="doc in docsAcademia" :key="doc.key"
            :label="doc.label" :file="form.docsLegales[doc.key]"
            @change="(f) => form.docsLegales[doc.key] = f"
            @remove="form.docsLegales[doc.key] = null"
            :accent="accent" />
        </div>

        <p v-if="errorDocAcademia" class="text-xs text-red-400 mt-2 flex items-center gap-1">
          <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01" />
          </svg>
          Debes subir al menos un documento.
        </p>
      </SectionCard>

      <!-- HOMESTUDIO: 2 obligatorios -->
      <SectionCard v-if="tipoSede === 'HOMESTUDIO'" titulo="Documentos Obligatorios" :accent="accent">
        <div class="flex items-start gap-3 p-3 bg-[#0d0f1a] border border-white/8 rounded-lg mb-4">
          <svg class="w-4 h-4 text-gray-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p class="text-xs text-gray-400">
            Para validar tu espacio independiente necesitamos confirmar actividad artística y relación con el lugar.
          </p>
        </div>

        <div class="space-y-4">
          <DocUpload
            label="Inicio de actividades SII" required
            :file="form.docSII"
            @change="(f) => form.docSII = f"
            @remove="form.docSII = null"
            :accent="accent" />

          <DocUpload
            label="Evidencia de dirección" required
            hint="Boleta de servicios, internet, agua, luz u otro documento que acredite el domicilio."
            :file="form.docDireccion"
            @change="(f) => form.docDireccion = f"
            @remove="form.docDireccion = null"
            :accent="accent" />
        </div>
      </SectionCard>
    </div>

    <!-- ───────────────────────────── PASO 6: Evidencia Visual ───────────────────────────── -->
    <div v-if="currentStep === 6" class="space-y-4">
      <SectionCard titulo="Evidencia Visual" :accent="accent">
        <p v-if="tipoSede === 'HOMESTUDIO'" class="text-xs text-gray-500 mb-4">
          Las fotos deben mostrar el espacio real: equipamiento, acústica, instrumentos, iluminación y acondicionamiento.
        </p>

        <!-- Foto de portada -->
        <Campo label="Foto principal (portada)" required>
          <div v-if="!form.fotoPortada"
            class="border-2 border-dashed border-white/10 rounded-xl p-6 text-center hover:border-white/20 transition-colors cursor-pointer relative"
            @click="$refs.inputPortada.click()">
            <svg class="w-8 h-8 text-gray-600 mx-auto mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <p class="text-sm text-gray-500">Haz clic para subir la foto de portada</p>
            <p class="text-xs text-gray-600 mt-1">JPG, PNG, WEBP — máx. 5MB</p>
            <input ref="inputPortada" type="file" accept="image/*" class="hidden"
              @change="handleFotoPortada" />
          </div>
          <div v-else class="relative rounded-xl overflow-hidden">
            <img :src="previews.portada" class="w-full h-48 object-cover rounded-xl" />
            <button @click="form.fotoPortada = null; previews.portada = null" type="button"
              class="absolute top-2 right-2 w-7 h-7 bg-black/60 hover:bg-black/80 rounded-full flex items-center justify-center transition-colors">
              <svg class="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </Campo>

        <!-- Fotos del espacio -->
        <Campo label="Fotografías del espacio (mínimo 2)" required>
          <div class="grid grid-cols-3 gap-3">
            <div v-for="(preview, i) in previews.espacio" :key="i" class="relative aspect-square">
              <img :src="preview" class="w-full h-full object-cover rounded-lg" />
              <button @click="removeFotoEspacio(i)" type="button"
                class="absolute top-1 right-1 w-6 h-6 bg-black/60 hover:bg-black/80 rounded-full flex items-center justify-center transition-colors">
                <svg class="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div v-if="form.fotosEspacio.length < 8"
              class="aspect-square border-2 border-dashed border-white/10 rounded-lg flex flex-col items-center justify-center cursor-pointer hover:border-white/20 transition-colors"
              @click="$refs.inputFotos.click()">
              <svg class="w-6 h-6 text-gray-600 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
              <span class="text-xs text-gray-600">Agregar</span>
              <input ref="inputFotos" type="file" accept="image/*" multiple class="hidden"
                @change="handleFotosEspacio" />
            </div>
          </div>
          <p class="text-xs text-gray-600 mt-2">Puedes subir hasta 8 fotos. Mínimo 2 requeridas.</p>
          <p v-if="errorFotos" class="text-xs text-red-400 mt-1 flex items-center gap-1">
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01" />
            </svg>
            Se requieren al menos 2 fotos del espacio.
          </p>
        </Campo>
      </SectionCard>
    </div>

    <!-- ───────────────────────────── PASO 7: Declaración ───────────────────────────── -->
    <div v-if="currentStep === 7" class="space-y-4">
      <SectionCard titulo="Declaración Responsable" :accent="accent">
        <div class="p-4 bg-[#0d0f1a] rounded-xl border border-white/8 space-y-4">
          <p class="text-sm text-gray-300">Antes de enviar tu solicitud, confirma lo siguiente:</p>

          <label class="flex items-start gap-3 cursor-pointer group">
            <div class="relative flex-shrink-0 mt-0.5">
              <input v-model="form.declaracion" type="checkbox" class="sr-only" />
              <div class="w-5 h-5 rounded border-2 transition-all duration-200 flex items-center justify-center"
                :class="form.declaracion
                  ? (tipoSede === 'ACADEMIA' ? 'bg-purple-600 border-purple-600' : 'bg-indigo-600 border-indigo-600')
                  : 'border-white/20 bg-white/3 group-hover:border-white/30'">
                <svg v-if="form.declaracion" class="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                </svg>
              </div>
            </div>
            <p class="text-sm text-gray-300 leading-relaxed">
              Declaro que la información entregada corresponde a un espacio real autorizado para realizar actividades artísticas,
              y que los documentos adjuntos son auténticos y vigentes.
            </p>
          </label>
        </div>

        <div class="p-3 bg-white/3 rounded-lg border border-white/5">
          <p class="text-xs text-gray-500">
            Tu solicitud será revisada por el equipo de Modo Ensayo.
            Recibirás una respuesta en un plazo de 2 a 5 días hábiles.
          </p>
        </div>

        <p v-if="errorDeclaracion" class="text-xs text-red-400 flex items-center gap-1">
          <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01" />
          </svg>
          Debes aceptar la declaración para continuar.
        </p>
      </SectionCard>
    </div>

    <!-- ───────────────────────────── Éxito ───────────────────────────── -->
    <div v-if="currentStep === 8" class="text-center py-10 space-y-4">
      <div class="inline-flex items-center justify-center w-16 h-16 rounded-full mb-2"
        :class="tipoSede === 'ACADEMIA' ? 'bg-purple-500/15' : 'bg-indigo-500/15'">
        <svg class="w-8 h-8" :class="tipoSede === 'ACADEMIA' ? 'text-purple-400' : 'text-indigo-400'"
          fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <h2 class="text-xl font-bold text-white">¡Solicitud enviada!</h2>
      <p class="text-gray-400 text-sm max-w-sm mx-auto">
        Revisaremos tu solicitud y te contactaremos en un plazo de 2 a 5 días hábiles.
        Te notificaremos cuando tu sede sea aprobada.
      </p>
      <router-link to="/alumno/dashboard"
        class="inline-block mt-4 px-6 py-2.5 rounded-xl text-sm font-medium text-white transition-colors"
        :class="tipoSede === 'ACADEMIA' ? 'bg-purple-600 hover:bg-purple-500' : 'bg-indigo-600 hover:bg-indigo-500'">
        Ir a mi panel
      </router-link>
    </div>

    <!-- Error de envío -->
    <div v-if="submitError" class="flex items-center gap-2 px-4 py-3 bg-red-500/10 border border-red-500/20 rounded-xl text-sm text-red-400">
      <svg class="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      {{ submitError }}
    </div>

    <!-- ───────────────────────────── Navegación ───────────────────────────── -->
    <div v-if="currentStep > 0 && currentStep < 8" class="flex gap-3">
      <button @click="retroceder" type="button"
        class="flex-1 py-3 border border-white/10 hover:border-white/20 text-gray-300 hover:text-white rounded-xl text-sm font-medium transition-colors">
        ← Atrás
      </button>
      <button v-if="currentStep < totalSteps" @click="avanzar" type="button"
        class="flex-1 py-3 rounded-xl text-sm font-medium text-white transition-colors"
        :class="tipoSede === 'ACADEMIA' ? 'bg-purple-600 hover:bg-purple-500' : 'bg-indigo-600 hover:bg-indigo-500'">
        Continuar →
      </button>
      <button v-else @click="enviar" type="button" :disabled="enviando"
        class="flex-1 py-3 rounded-xl text-sm font-medium text-white transition-colors disabled:opacity-50"
        :class="tipoSede === 'ACADEMIA' ? 'bg-purple-600 hover:bg-purple-500' : 'bg-indigo-600 hover:bg-indigo-500'">
        {{ enviando ? 'Enviando...' : 'Enviar solicitud' }}
      </button>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, defineComponent, h } from 'vue'
import { venueService } from '../services/venueService'
import { uploadService } from '../services/uploadService'

// ─── Componentes locales ────────────────────────────────────────────────────

const SectionCard = defineComponent({
  props: { titulo: String, accent: String },
  setup(props, { slots }) {
    return () => h('div', { class: 'bg-[#161824] rounded-xl border border-white/10 p-5 space-y-4' }, [
      h('h2', { class: 'font-semibold text-white text-sm' }, props.titulo),
      slots.default?.(),
    ])
  },
})

const Campo = defineComponent({
  props: { label: String, required: { type: Boolean, default: true } },
  setup(props, { slots }) {
    return () => h('div', {}, [
      h('label', { class: 'block text-xs font-medium text-gray-500 uppercase tracking-wider mb-1.5' }, [
        props.label,
        props.required
          ? h('span', { class: 'text-red-400 ml-0.5' }, ' *')
          : h('span', { class: 'ml-1 text-gray-600 normal-case font-normal' }, '(opcional)'),
      ]),
      slots.default?.(),
    ])
  },
})

const DocUpload = defineComponent({
  props: { label: String, required: { type: Boolean, default: false }, hint: String, file: Object, accent: String },
  emits: ['change', 'remove'],
  setup(props, { emit }) {
    const inputRef = ref(null)
    return () => h('div', { class: 'p-3 bg-white/3 border border-white/8 rounded-lg' }, [
      h('div', { class: 'flex items-center justify-between' }, [
        h('div', { class: 'flex items-center gap-2 min-w-0' }, [
          h('div', {
            class: `w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 ${
              props.file
                ? (props.accent === 'purple' ? 'bg-purple-500/20' : 'bg-indigo-500/20')
                : 'bg-white/5'
            }`,
          }, [
            h('svg', { class: `w-3.5 h-3.5 ${props.file ? (props.accent === 'purple' ? 'text-purple-400' : 'text-indigo-400') : 'text-gray-600'}`, fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor' }, [
              h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'stroke-width': '2', d: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' }),
            ]),
          ]),
          h('div', { class: 'min-w-0' }, [
            h('p', { class: 'text-sm text-gray-300 truncate' }, [
              props.label,
              props.required ? h('span', { class: 'text-red-400 ml-0.5 text-xs' }, ' *') : null,
            ]),
            props.file
              ? h('p', { class: 'text-xs text-gray-500 truncate' }, props.file.name)
              : props.hint ? h('p', { class: 'text-xs text-gray-600' }, props.hint) : null,
          ]),
        ]),
        props.file
          ? h('button', {
              type: 'button',
              class: 'text-xs text-red-400 hover:text-red-300 transition-colors flex-shrink-0 ml-2',
              onClick: () => emit('remove'),
            }, 'Quitar')
          : h('button', {
              type: 'button',
              class: `text-xs font-medium transition-colors flex-shrink-0 ml-2 ${props.accent === 'purple' ? 'text-purple-400 hover:text-purple-300' : 'text-indigo-400 hover:text-indigo-300'}`,
              onClick: () => inputRef.value?.click(),
            }, 'Subir'),
      ]),
      h('input', {
        ref: inputRef,
        type: 'file',
        accept: '.pdf,.jpg,.jpeg,.png',
        class: 'hidden',
        onChange: (e) => { if (e.target.files[0]) emit('change', e.target.files[0]) },
      }),
    ])
  },
})

// ─── Estado ─────────────────────────────────────────────────────────────────

const currentStep = ref(0)
const tipoSede = ref('')
const enviando = ref(false)

const errorDocAcademia = ref(false)
const errorFotos = ref(false)
const errorDeclaracion = ref(false)
const submitError = ref('')

const form = ref({
  nombre: '',
  descripcion: '',
  region: '',
  comuna: '',
  direccion: '',
  referencia: '',
  mapLink: '',
  telefono: '',
  emailContacto: '',
  instagram: '',
  facebook: '',
  tiktok: '',
  youtube: '',
  sitioWeb: '',
  docsLegales: { patente: null, sii_academia: null, arriendo: null, tributario: null, certificado: null, municipal: null },
  docSII: null,
  docDireccion: null,
  fotosEspacio: [],
  fotoPortada: null,
  declaracion: false,
})

const previews = ref({ portada: null, espacio: [] })

// ─── Constantes ──────────────────────────────────────────────────────────────

const regiones = [
  'Arica y Parinacota', 'Tarapacá', 'Antofagasta', 'Atacama', 'Coquimbo',
  'Valparaíso', 'Metropolitana de Santiago', "Libertador Gral. Bernardo O'Higgins",
  'Maule', 'Ñuble', 'Biobío', 'La Araucanía', 'Los Ríos', 'Los Lagos',
  "Aysén del Gral. Carlos Ibáñez del Campo", 'Magallanes y la Antártica Chilena',
]

const docsAcademia = [
  { key: 'patente', label: 'Patente comercial' },
  { key: 'sii_academia', label: 'Inicio de actividades SII' },
  { key: 'arriendo', label: 'Contrato de arriendo comercial' },
  { key: 'tributario', label: 'Documento tributario' },
  { key: 'certificado', label: 'Certificado de empresa' },
  { key: 'municipal', label: 'Autorización municipal' },
]

const stepLabels = [
  'Información general',
  'Ubicación',
  'Contacto',
  'Presencia pública',
  'Documentación',
  'Evidencia visual',
  'Declaración responsable',
]

const totalSteps = 7
const accent = computed(() => tipoSede.value === 'ACADEMIA' ? 'purple' : 'indigo')

// ─── Métodos ─────────────────────────────────────────────────────────────────

const seleccionarTipo = (tipo) => { tipoSede.value = tipo }

const validarPasoActual = () => {
  errorDocAcademia.value = false
  errorFotos.value = false
  errorDeclaracion.value = false

  if (currentStep.value === 1) {
    return form.value.nombre.trim() && form.value.descripcion.trim()
  }
  if (currentStep.value === 2) {
    return form.value.region && form.value.comuna.trim() && form.value.direccion.trim() && form.value.mapLink.trim()
  }
  if (currentStep.value === 3) {
    return form.value.telefono.trim() && form.value.emailContacto.trim()
  }
  if (currentStep.value === 4) return true
  if (currentStep.value === 5) {
    if (tipoSede.value === 'ACADEMIA') {
      const tieneAlguno = Object.values(form.value.docsLegales).some(v => v !== null)
      if (!tieneAlguno) { errorDocAcademia.value = true; return false }
    } else {
      return form.value.docSII && form.value.docDireccion
    }
  }
  if (currentStep.value === 6) {
    if (!form.value.fotoPortada || form.value.fotosEspacio.length < 2) {
      errorFotos.value = true; return false
    }
  }
  if (currentStep.value === 7) {
    if (!form.value.declaracion) { errorDeclaracion.value = true; return false }
  }
  return true
}

const avanzar = () => {
  if (!validarPasoActual()) return
  currentStep.value++
}

const retroceder = () => {
  if (currentStep.value === 1) { currentStep.value = 0 } else { currentStep.value-- }
}

const handleFotoPortada = (e) => {
  const f = e.target.files[0]
  if (!f) return
  form.value.fotoPortada = f
  previews.value.portada = URL.createObjectURL(f)
}

const handleFotosEspacio = (e) => {
  const files = Array.from(e.target.files)
  const disponibles = 8 - form.value.fotosEspacio.length
  files.slice(0, disponibles).forEach(f => {
    form.value.fotosEspacio.push(f)
    previews.value.espacio.push(URL.createObjectURL(f))
  })
  e.target.value = ''
}

const removeFotoEspacio = (i) => {
  form.value.fotosEspacio.splice(i, 1)
  URL.revokeObjectURL(previews.value.espacio[i])
  previews.value.espacio.splice(i, 1)
}

const enviar = async () => {
  if (!validarPasoActual()) return
  enviando.value = true
  submitError.value = ''
  try {
    let imageUrl = ''
    if (form.value.fotoPortada) {
      try {
        const { url } = await uploadService.upload(form.value.fotoPortada, 'venues')
        imageUrl = url
      } catch { /* continuar sin imagen si falla el upload */ }
    }
    const address = [form.value.direccion, form.value.comuna, form.value.region]
      .filter(Boolean).join(', ')
    await venueService.create({
      name: form.value.nombre,
      description: form.value.descripcion,
      address,
      phone: form.value.telefono,
      email: form.value.emailContacto,
      imageUrl,
    })
    currentStep.value = 8
  } catch (err) {
    submitError.value = err?.response?.data?.message || 'No se pudo enviar la solicitud. Intenta de nuevo.'
  } finally {
    enviando.value = false
  }
}
</script>

<style scoped>
.campo-input {
  @apply w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/10 rounded-lg text-white text-sm
         focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-white/20
         placeholder-gray-600 transition-all;
}

select.campo-input option {
  background-color: #161824;
}
</style>
