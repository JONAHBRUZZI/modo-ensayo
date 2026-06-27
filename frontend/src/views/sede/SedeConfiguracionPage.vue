<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
    <h1 class="text-3xl font-bold text-white">Mi Sede</h1>

    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>

    <template v-else-if="!venue">
      <div class="card text-center py-12">
        <p class="text-gray-400">No tienes una sede registrada.</p>
        <router-link to="/sede/registro" class="btn-primary mt-4 inline-block">Registrar Sede</router-link>
      </div>
    </template>

    <template v-else>
      <!-- Estado de la sede -->
      <div class="card">
        <div class="flex items-center justify-between">
          <div>
            <div class="flex items-center gap-2 flex-wrap">
              <h3 class="text-white font-semibold text-lg">{{ venue.name }}</h3>
              <span v-if="venueRating" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-yellow-500/15 text-yellow-400 text-xs font-medium">
                ★ {{ venueRating?.toFixed(1) }}
              </span>
            </div>
            <p class="text-gray-400 text-sm">{{ venue.address }}, {{ venue.city }}</p>
          </div>
          <EstadoBadge :status="venue.status" />
        </div>
        <div v-if="venue.tipo" class="mt-2">
          <span class="badge badge-blue">{{ venue.tipo === 'HOME_STUDIO' ? 'HomeStudio' : 'Sede' }}</span>
        </div>
      </div>

      <!-- Foto de perfil de la sede -->
      <div class="card space-y-4" :class="venue.imageUrl ? '' : 'border border-yellow-500/40'">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h3 class="text-white font-medium">Foto de la sede</h3>
            <p class="text-gray-500 text-xs mt-1">
              Foto del frontis de la sede. Se usa como imagen de perfil en el sistema. Puedes cambiarla cuando quieras.
            </p>
          </div>
          <span v-if="!venue.imageUrl" class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-yellow-500/15 text-yellow-400 border border-yellow-500/30 whitespace-nowrap">
            Falta foto
          </span>
        </div>

        <div class="flex items-center gap-4 flex-wrap">
          <img
            v-if="fotoPerfilPreview || venue.imageUrl"
            :src="fotoPerfilPreview || venue.imageUrl"
            alt="Foto de la sede"
            class="w-28 h-28 object-cover rounded-xl border border-white/10"
          />
          <div v-else class="w-28 h-28 rounded-xl border border-dashed border-white/20 flex items-center justify-center text-gray-600">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
          </div>

          <div class="flex-1 min-w-[200px] space-y-2">
            <input type="file"
              accept="image/*"
              @change="onFotoPerfil"
              class="block w-full text-sm text-gray-400
                file:mr-4 file:py-2 file:px-4 file:rounded file:border-0
                file:text-sm file:bg-primary file:text-white
                hover:file:bg-primary/80 file:cursor-pointer file:transition-colors" />
            <p v-if="!venue.imageUrl" class="text-yellow-400 text-xs">
              Esta sede aún no tiene foto. Sube una para completar tu perfil.
            </p>
            <p v-if="msgFoto" :class="msgFotoType === 'error' ? 'text-red-400' : 'text-green-400'" class="text-xs">{{ msgFoto }}</p>
            <button type="button" @click="guardarFoto" :disabled="savingFoto || !fotoPerfil" class="btn-primary text-sm disabled:opacity-50 disabled:cursor-not-allowed">
              {{ savingFoto ? 'Guardando...' : (venue.imageUrl ? 'Cambiar foto' : 'Subir foto') }}
            </button>
          </div>
        </div>
      </div>

      <!-- Sede rechazada: motivo -->
      <div v-if="venue.status === 'RECHAZADA'" class="card border-red-500/30 bg-red-500/5">
        <h3 class="text-red-400 font-medium mb-1">Sede Rechazada</h3>
        <p class="text-gray-400 text-sm">Motivo: {{ venue.rejectionReason || 'No especificado' }}</p>
        <p class="text-gray-500 text-xs mt-1">Corrige los datos y reenvia para aprobacion.</p>
      </div>

      <!-- Datos estructurales (editable solo si no APROBADA) -->
      <form v-if="venue.status !== 'APROBADA'" @submit.prevent="saveDatos" class="card space-y-4">
        <h3 class="text-white font-medium">Datos de la sede</h3>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Nombre</label>
          <input v-model="formDatos.name" required class="input-field" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Ciudad</label>
            <input v-model="formDatos.city" required class="input-field" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Direccion</label>
            <input ref="addressInput" v-model="formDatos.address" required class="input-field" placeholder="ej: Av. Italia 1234, Providencia" />
          </div>
        </div>
        <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Descripción</label>
            <textarea v-model="formDatos.description" rows="3" class="input-field" placeholder="Describe tu sede..."></textarea>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Teléfono</label>
            <input v-model="formDatos.phone" class="input-field" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Email de contacto</label>
            <input v-model="formDatos.email" type="email" class="input-field" />
          </div>
        </div>
        <p v-if="msgDatos" :class="msgDatosType === 'error' ? 'text-red-400' : 'text-green-400'" class="text-sm">{{ msgDatos }}</p>
        <button type="submit" :disabled="savingDatos" class="btn-primary w-full">
          {{ savingDatos ? 'Guardando...' : 'Guardar y reenviar para aprobacion' }}
        </button>
      </form>

      <!-- Sede aprobada: datos verificados -->
      <div v-else class="card space-y-5">
        <!-- Banner verificado -->
        <div class="flex items-center gap-3 p-3 rounded-xl bg-green-500/10 border border-green-500/25">
          <div class="w-9 h-9 rounded-full bg-green-500/20 flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
            </svg>
          </div>
          <div>
            <p class="text-green-400 font-semibold text-sm">Sede verificada y aprobada por el Administrador General</p>
            <p class="text-gray-500 text-xs mt-0.5">Los datos registrados fueron revisados y validados. Si necesitas modificar datos estructurales, contacta al Administrador General.</p>
          </div>
        </div>

        <!-- Datos verificados -->
        <div>
          <h4 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Datos registrados y verificados</h4>
          <div class="space-y-2">
            <div v-for="campo in camposVerificados" :key="campo.label"
                 class="flex items-start justify-between py-2 border-b border-white/5 last:border-0">
              <span class="text-gray-500 text-sm w-32 flex-shrink-0">{{ campo.label }}</span>
              <div class="flex items-center gap-2 flex-1 justify-end">
                <span class="text-gray-200 text-sm text-right">{{ campo.valor }}</span>
                <svg class="w-4 h-4 text-green-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/>
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Horario laboral de la sede (solo sedes aprobadas) -->
      <div v-if="venue.status === 'APROBADA'" class="card space-y-4" :class="hayHorario ? '' : 'border border-yellow-500/40'">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h3 class="text-white font-medium">Horario laboral de la sede</h3>
            <p class="text-gray-500 text-xs mt-1">
              Define los días y horas de atención. El sistema genera automáticamente los bloques
              reservables de tus salas a partir de este horario.
            </p>
          </div>
          <span v-if="!hayHorario" class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-yellow-500/15 text-yellow-400 border border-yellow-500/30 whitespace-nowrap">
            Sin configurar
          </span>
        </div>

        <p v-if="!hayHorario" class="text-yellow-300/90 text-sm bg-yellow-500/10 border border-yellow-500/20 rounded-lg p-3">
          ⚠️ Tu sede aún no tiene horario configurado. Mientras no lo definas, tus salas no mostrarán
          disponibilidad y los profesores no podrán reservarlas.
        </p>

        <div class="space-y-3">
          <div v-for="day in days" :key="day" class="flex items-center gap-4 flex-wrap">
            <label class="flex items-center gap-2 w-28">
              <input type="checkbox" v-model="scheduleDays[day].enabled"
                     class="w-4 h-4 rounded border-dark-border bg-dark-bg text-primary focus:ring-primary/50" />
              <span class="text-sm text-gray-300">{{ dayLabels[day] }}</span>
            </label>
            <template v-if="scheduleDays[day].enabled">
              <input type="time" v-model="scheduleDays[day].openTime" class="input-field w-32" />
              <span class="text-gray-400 text-sm">a</span>
              <input type="time" v-model="scheduleDays[day].closeTime" class="input-field w-32" />
            </template>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2 border-t border-dark-border">
          <div>
            <label class="block text-xs text-gray-400 mb-1">Duración del bloque (min)</label>
            <input type="number" v-model.number="blockCfg.duration" class="input-field" min="15" step="5" />
          </div>
          <div>
            <label class="block text-xs text-gray-400 mb-1">Brecha entre bloques (min)</label>
            <input type="number" v-model.number="blockCfg.gap" class="input-field" min="0" step="5" />
          </div>
        </div>

        <p v-if="configMsg" :class="configMsgType === 'error' ? 'text-red-400' : 'text-green-400'" class="text-sm">{{ configMsg }}</p>

        <button @click="confirmSaveConfig = true" :disabled="savingConfig" class="btn-primary text-sm">
          {{ savingConfig ? 'Guardando...' : 'Guardar configuración' }}
        </button>
      </div>

      <!-- Confirmar regeneración de bloques -->
      <div v-if="confirmSaveConfig" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
        <div class="card max-w-md w-full mx-4 space-y-4">
          <h4 class="text-lg font-semibold text-white">Confirmar cambios</h4>
          <p class="text-sm text-gray-400">
            ⚠️ Este cambio es TOTAL. Se regenerarán todos los bloques. Las clases en horarios que ya no existan serán afectadas. ¿Confirmas?
          </p>
          <div class="flex justify-end gap-3">
            <button @click="confirmSaveConfig = false" class="text-sm text-gray-400 hover:text-white">Cancelar</button>
            <button @click="saveAllConfig" class="btn-primary text-sm">Confirmar</button>
          </div>
        </div>
      </div>

      <!-- Documentos de la sede -->
      <div class="card space-y-4">
        <div class="flex items-start justify-between">
          <div>
            <h3 class="text-white font-medium">Documentos de la sede</h3>
            <p v-if="venue.status === 'APROBADA'" class="text-green-400 text-xs mt-1">✓ Todos los documentos fueron revisados y aprobados</p>
            <p v-else class="text-gray-500 text-xs mt-1">Sube permisos, certificados o documentos requeridos para la aprobación. El Admin General los revisará.</p>
          </div>
        </div>

        <!-- Lista de documentos existentes -->
        <div v-if="documentos.length > 0" class="space-y-2">
          <div v-for="doc in documentos" :key="doc.id"
               class="flex items-center justify-between p-3 bg-dark-bg rounded-lg border"
               :class="venue.status === 'APROBADA' ? 'border-green-500/30' : 'border-dark-border'">
            <div class="flex items-center gap-3">
              <svg :class="{
                'text-green-400': venue.status === 'APROBADA' || doc.estado === 'APROBADO',
                'text-yellow-500': venue.status !== 'APROBADA' && doc.estado === 'PENDIENTE',
                'text-red-400': doc.estado === 'RECHAZADO'
              }" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              <div>
                <p class="text-sm text-white">{{ doc.nombre || 'Documento' }}</p>
                <span :class="{
                  'text-yellow-400': venue.status !== 'APROBADA' && doc.estado === 'PENDIENTE',
                  'text-green-400': venue.status === 'APROBADA' || doc.estado === 'APROBADO',
                  'text-red-400': doc.estado === 'RECHAZADO'
                }" class="text-xs font-medium">
                  {{ venue.status === 'APROBADA' ? 'APROBADO' : doc.estado }}
                </span>
                <span v-if="doc.motivoRechazo" class="text-xs text-red-400 ml-2">— {{ doc.motivoRechazo }}</span>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <a :href="doc.fileUrl" target="_blank" class="text-primary text-xs hover:underline">Ver</a>
              <button v-if="venue.status !== 'APROBADA'" @click="eliminarDocumento(doc.id)" class="text-red-400 text-xs hover:underline">Eliminar</button>
            </div>
          </div>
        </div>
        <p v-else class="text-gray-500 text-sm">No hay documentos subidos aún.</p>

        <!-- Subir nuevo documento (solo si no aprobada) -->
        <div v-if="venue.status !== 'APROBADA'" class="border border-dark-border rounded-lg p-4 space-y-3">
          <p class="text-sm text-gray-300 font-medium">Subir documento</p>
          <div>
            <label class="block text-xs text-gray-400 mb-1">Tipo de documento</label>
            <select v-model="nuevoDoc.tipo" class="input-field">
              <option value="">Seleccionar tipo</option>
              <option value="RUT_EMPRESA">RUT Empresa</option>
              <option value="CEDULA_IDENTIDAD">Cedula Identidad</option>
              <option value="INICIO_ACTIVIDADES_F4415">Inicio Actividades F4415</option>
              <option value="CERTIFICADO_SITUACION_TRIBUTARIA">Cert. Situación Tributaria</option>
              <option value="PERMISO_MUNICIPAL">Permiso Municipal</option>
              <option value="CONTRATO_ARRIENDO">Contrato Arriendo</option>
              <option value="OTRO">Otro</option>
            </select>
          </div>
          <div>
            <label class="block text-xs text-gray-400 mb-1">Nombre / Descripción</label>
            <input v-model="nuevoDoc.nombre" class="input-field" placeholder="Ej: Permiso Municipal" />
          </div>
          <div>
            <label class="block text-xs text-gray-400 mb-1">Archivo (PDF, imagen, máx 5MB)</label>
            <input type="file" @change="onDocFileChange" accept=".pdf,image/*"
                   class="block w-full text-sm text-gray-400 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:bg-primary file:text-white hover:file:bg-primary/80" />
            <p v-if="uploadingDoc" class="text-xs text-gray-400 mt-1">Subiendo...</p>
          </div>
          <button @click="subirDocumento" :disabled="uploadingDoc || !nuevoDoc.fileUrl || !nuevoDoc.nombre"
                  class="btn-primary text-sm px-4 py-2">
            Agregar Documento
          </button>
          <p v-if="msgDoc" :class="msgDocType === 'error' ? 'text-red-400' : 'text-green-400'" class="text-xs">{{ msgDoc }}</p>
        </div>
      </div>

      <!-- Cobros con MercadoPago (solo sedes aprobadas) -->
      <div v-if="venue.status === 'APROBADA'" class="card space-y-4">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h3 class="text-white font-medium">Cobros con MercadoPago</h3>
            <p class="text-gray-500 text-xs mt-1">
              Vincula tu cuenta de MercadoPago para recibir los pagos por arriendo de tus salas directamente en ella.
            </p>
          </div>
          <span v-if="mpConectado" class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-green-500/15 text-green-400 border border-green-500/30 whitespace-nowrap">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
            Conectada
          </span>
          <span v-else class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-gray-500/15 text-gray-400 border border-white/10 whitespace-nowrap">
            No conectada
          </span>
        </div>
        <p v-if="mpConectado" class="text-sm text-gray-400">
          Tu cuenta está vinculada. Los profesores que arrienden tus salas pagarán por MercadoPago y el dinero llegará a tu cuenta.
        </p>
        <button v-else @click="conectarMercadoPago" :disabled="conectandoMp" class="btn-primary">
          {{ conectandoMp ? 'Redirigiendo a MercadoPago...' : 'Conectar MercadoPago' }}
        </button>
      </div>

      <!-- Redes sociales y contacto web (siempre editable) -->
      <form @submit.prevent="saveSocial" class="card space-y-4">
        <h3 class="text-white font-medium">Redes sociales y contacto web</h3>
        <p class="text-gray-500 text-xs">Estos datos son siempre editables y se muestran en tu perfil publico.</p>

        <div v-if="venue.status === 'APROBADA'" class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Descripción</label>
            <textarea v-model="formSocial.description" rows="2" class="input-field" placeholder="Describe tu sede..."></textarea>
          </div>
          <div class="space-y-3">
            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">Teléfono</label>
              <input v-model="formSocial.phone" class="input-field" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">Email de contacto</label>
              <input v-model="formSocial.email" type="email" class="input-field" />
            </div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              <span class="inline-flex items-center gap-1">
                <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073z"/></svg>
                Instagram
              </span>
            </label>
            <input v-model="formSocial.instagram" class="input-field" placeholder="@mi_sede" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Facebook</label>
            <input v-model="formSocial.facebook" class="input-field" placeholder="facebook.com/mi-sede" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">YouTube</label>
            <input v-model="formSocial.youtube" class="input-field" placeholder="youtube.com/@mi-sede" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Sitio Web</label>
            <input v-model="formSocial.sitioWeb" class="input-field" placeholder="https://mi-sede.cl" />
          </div>
        </div>

        <p v-if="msgSocial" :class="msgSocialType === 'error' ? 'text-red-400' : 'text-green-400'" class="text-sm">{{ msgSocial }}</p>
        <button type="submit" :disabled="savingSocial" class="btn-primary">
          {{ savingSocial ? 'Guardando...' : 'Guardar redes sociales' }}
        </button>
      </form>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import venueService from '@/services/venueService'
import scheduleService from '@/services/scheduleService'
import paymentService from '@/services/paymentService'
import uploadService from '@/services/uploadService'
import { reviewService } from '@/services/reviewService'
import { useToast } from '@/composables/useToast'
import EstadoBadge from '@/components/EstadoBadge.vue'
import { usePlacesAutocomplete } from '@/composables/usePlacesAutocomplete'

const { attachAutocomplete } = usePlacesAutocomplete()
const addressInput = ref(null)
const route = useRoute()
const router = useRouter()
const toast = useToast()

const venue = ref(null)
const loading = ref(true)
const venueRating = ref(null)

// Estado de vinculación con MercadoPago
const mpStatus = ref(null)
const mpConectado = computed(() => mpStatus.value?.status === 'CONNECTED' && mpStatus.value?.hasToken)
const conectandoMp = ref(false)

async function cargarMpStatus() {
  try { mpStatus.value = await paymentService.getMpAccountStatus() } catch { mpStatus.value = null }
}

async function conectarMercadoPago() {
  conectandoMp.value = true
  try {
    const { authUrl } = await paymentService.startMpConnect()
    if (authUrl) window.location.href = authUrl
    else throw new Error('sin authUrl')
  } catch (e) {
    conectandoMp.value = false
    toast.error(e?.response?.data?.error || 'No se pudo iniciar la conexión con MercadoPago')
  }
}

const camposVerificados = computed(() => {
  if (!venue.value) return []
  return [
    { label: 'Nombre', valor: venue.value.name },
    { label: 'Tipo', valor: venue.value.tipo === 'HOME_STUDIO' ? 'Home Studio' : 'Sede' },
    { label: 'Ciudad', valor: venue.value.city },
    { label: 'Dirección', valor: venue.value.address },
    { label: 'Teléfono', valor: venue.value.phone || '—' },
    { label: 'Email', valor: venue.value.email || '—' },
  ].filter(c => c.valor)
})

// Foto de perfil de la sede
const fotoPerfil = ref(null)
const fotoPerfilPreview = ref('')
const savingFoto = ref(false)
const msgFoto = ref('')
const msgFotoType = ref('')

function onFotoPerfil(event) {
  const file = event.target.files?.[0]
  if (!file) return
  fotoPerfil.value = file
  fotoPerfilPreview.value = URL.createObjectURL(file)
  msgFoto.value = ''
}

async function guardarFoto() {
  if (!fotoPerfil.value || !venue.value) return
  savingFoto.value = true
  msgFoto.value = ''
  try {
    const { url } = await uploadService.uploadFile(fotoPerfil.value, 'venue', venue.value.id)
    await venueService.updateVenueExtras(venue.value.id, { imageUrl: url })
    venue.value = { ...venue.value, imageUrl: url }
    fotoPerfil.value = null
    fotoPerfilPreview.value = ''
    msgFoto.value = 'Foto actualizada correctamente.'
    msgFotoType.value = 'success'
  } catch (e) {
    msgFoto.value = e?.response?.data?.message || e?.message || 'Error al subir la foto'
    msgFotoType.value = 'error'
  }
  savingFoto.value = false
}

// Formulario datos estructurales
const formDatos = reactive({ name: '', city: '', address: '', description: '', phone: '', email: '' })
const savingDatos = ref(false)
const msgDatos = ref('')
const msgDatosType = ref('')

// Formulario redes sociales
const formSocial = reactive({ instagram: '', facebook: '', youtube: '', sitioWeb: '', description: '', phone: '', email: '' })
const savingSocial = ref(false)
const msgSocial = ref('')
const msgSocialType = ref('')

// Documentos de sede
const documentos = ref([])
const nuevoDoc = reactive({ nombre: '', tipo: '', fileUrl: '' })
const uploadingDoc = ref(false)
const msgDoc = ref('')
const msgDocType = ref('')

// Horario laboral de la sede
const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const dayLabels = {
  MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves', FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo'
}
const scheduleDays = reactive({
  MONDAY: { enabled: true, openTime: '08:00', closeTime: '18:00' },
  TUESDAY: { enabled: true, openTime: '08:00', closeTime: '18:00' },
  WEDNESDAY: { enabled: true, openTime: '08:00', closeTime: '18:00' },
  THURSDAY: { enabled: true, openTime: '08:00', closeTime: '18:00' },
  FRIDAY: { enabled: true, openTime: '08:00', closeTime: '18:00' },
  SATURDAY: { enabled: false, openTime: '08:00', closeTime: '18:00' },
  SUNDAY: { enabled: false, openTime: '08:00', closeTime: '18:00' }
})
const blockCfg = reactive({ duration: 60, gap: 15 })
const hayHorario = ref(false)
const savingConfig = ref(false)
const confirmSaveConfig = ref(false)
const configMsg = ref('')
const configMsgType = ref('')

async function cargarHorario(venueId) {
  try {
    const [scheduleRes, blockConfigRes] = await Promise.allSettled([
      scheduleService.getSchedule(venueId),
      scheduleService.getBlockConfig(venueId)
    ])
    if (scheduleRes.status === 'fulfilled' && Array.isArray(scheduleRes.value)) {
      hayHorario.value = scheduleRes.value.length > 0
      // Reinicia y aplica solo los días que vienen del servidor.
      for (const d of days) scheduleDays[d].enabled = false
      for (const s of scheduleRes.value) {
        if (scheduleDays[s.dayOfWeek]) {
          scheduleDays[s.dayOfWeek].enabled = true
          scheduleDays[s.dayOfWeek].openTime = s.openTime?.slice(0, 5) || '08:00'
          scheduleDays[s.dayOfWeek].closeTime = s.closeTime?.slice(0, 5) || '18:00'
        }
      }
    }
    if (blockConfigRes.status === 'fulfilled' && blockConfigRes.value) {
      blockCfg.duration = blockConfigRes.value.blockDurationMin || 60
      blockCfg.gap = blockConfigRes.value.gapBetweenBlocksMin || 15
    }
  } catch (err) {
    console.error('Error al cargar el horario de la sede', err)
  }
}

async function saveAllConfig() {
  confirmSaveConfig.value = false
  savingConfig.value = true
  configMsg.value = ''
  try {
    const schedules = days
      .filter(d => scheduleDays[d].enabled)
      .map(d => ({ dayOfWeek: d, openTime: scheduleDays[d].openTime, closeTime: scheduleDays[d].closeTime }))

    if (schedules.length === 0) {
      configMsg.value = 'Marca al menos un día de atención.'
      configMsgType.value = 'error'
      savingConfig.value = false
      return
    }

    await scheduleService.saveSchedule(venue.value.id, schedules)
    await scheduleService.saveBlockConfig(venue.value.id, {
      blockDurationMin: blockCfg.duration,
      gapBetweenBlocksMin: blockCfg.gap
    })
    await scheduleService.generateBlocks(venue.value.id)

    hayHorario.value = true
    configMsg.value = 'Horario guardado y bloques regenerados correctamente.'
    configMsgType.value = 'success'
  } catch (e) {
    configMsg.value = e?.response?.data?.message || 'Error al guardar el horario'
    configMsgType.value = 'error'
  }
  savingConfig.value = false
}

onMounted(async () => {
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues?.content || []
    const prioridad = ['APROBADA', 'SUSPENDIDA', 'PENDIENTE_APROBACION', 'RECHAZADA']
    const sorted = [...vArr].sort((a, b) =>
      prioridad.indexOf(a.status) - prioridad.indexOf(b.status)
    )
    if (sorted.length > 0) {
      venue.value = sorted[0]
      const v = venue.value
      // Poblar formulario datos
      formDatos.name = v.name || ''
      formDatos.city = v.city || ''
      formDatos.address = v.address || ''
      formDatos.description = v.description || ''
      formDatos.phone = v.phone || ''
      formDatos.email = v.email || ''
      // Poblar formulario redes sociales
      formSocial.instagram = v.instagram || ''
      formSocial.facebook = v.facebook || ''
      formSocial.youtube = v.youtube || ''
      formSocial.sitioWeb = v.sitioWeb || ''
      formSocial.description = v.description || ''
      formSocial.phone = v.phone || ''
      formSocial.email = v.email || ''
      // Cargar documentos
      try {
        documentos.value = await venueService.getVenueDocuments(v.id)
      } catch (err) {
        console.error('Error al cargar documentos de la sede', err)
        documentos.value = []
      }
      // Cargar rating promedio de la sede
      try {
        const revRes = await reviewService.getByTarget('VENUE', v.id)
        const reviews = revRes.data
        if (Array.isArray(reviews) && reviews.length > 0) {
          const avg = reviews.reduce((s, r) => s + (r.score || 0), 0) / reviews.length
          venueRating.value = avg
        }
      } catch (err) {
        console.error('Error al cargar rating de la sede', err)
      }
      // Cargar el horario laboral (relevante solo en sedes aprobadas)
      if (v.status === 'APROBADA') await cargarHorario(v.id)
    }
  } catch (err) { console.error('Error al cargar sedes del usuario', err) }
  loading.value = false
  attachAutocomplete(addressInput.value, (place) => {
    formDatos.address = place.formatted_address
    if (place.city && !formDatos.city) formDatos.city = place.city
  })

  // Estado de la cuenta MercadoPago y feedback del retorno OAuth.
  cargarMpStatus()
  if (route.query.mp === 'ok') {
    toast.success('Cuenta de MercadoPago vinculada correctamente')
    router.replace({ query: {} })
  } else if (route.query.mp === 'error') {
    toast.error('No se pudo vincular la cuenta de MercadoPago. Intenta nuevamente.')
    router.replace({ query: {} })
  }
})

async function saveDatos() {
  savingDatos.value = true
  msgDatos.value = ''
  try {
    const updated = await venueService.updateVenue(venue.value.id, formDatos)
    venue.value = { ...venue.value, ...updated }
    msgDatos.value = 'Datos actualizados. Espera la aprobacion del Administrador General.'
    msgDatosType.value = 'success'
  } catch (e) {
    msgDatos.value = e?.response?.data?.message || 'Error al guardar'
    msgDatosType.value = 'error'
  }
  savingDatos.value = false
}

async function saveSocial() {
  savingSocial.value = true
  msgSocial.value = ''
  try {
    const updated = await venueService.updateVenueSocial(venue.value.id, formSocial)
    venue.value = { ...venue.value, ...updated }
    msgSocial.value = 'Redes sociales actualizadas.'
    msgSocialType.value = 'success'
  } catch (e) {
    msgSocial.value = e?.response?.data?.message || 'Error al guardar'
    msgSocialType.value = 'error'
  }
  savingSocial.value = false
}

async function onDocFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  uploadingDoc.value = true
  nuevoDoc.fileUrl = ''
  msgDoc.value = ''
  try {
    const res = await uploadService.uploadFile(file, 'venue-documents', venue.value.id)
    nuevoDoc.fileUrl = res.url
    nuevoDoc.tipoArchivo = file.type
  } catch (e) {
    msgDoc.value = e?.response?.data?.error || 'Error al subir el archivo'
    msgDocType.value = 'error'
  }
  uploadingDoc.value = false
}

async function subirDocumento() {
  if (!nuevoDoc.fileUrl || !nuevoDoc.nombre) return
  msgDoc.value = ''
  try {
    const doc = await venueService.addVenueDocument(venue.value.id, {
      fileUrl: nuevoDoc.fileUrl,
      nombre: nuevoDoc.nombre,
      tipo: nuevoDoc.tipo || null,
      tipoArchivo: nuevoDoc.tipoArchivo || ''
    })
    documentos.value.unshift(doc)
    nuevoDoc.nombre = ''
    nuevoDoc.tipo = ''
    nuevoDoc.fileUrl = ''
    msgDoc.value = 'Documento subido correctamente.'
    msgDocType.value = 'success'
  } catch (e) {
    msgDoc.value = e?.response?.data?.message || 'Error al guardar el documento'
    msgDocType.value = 'error'
  }
}

async function eliminarDocumento(docId) {
  try {
    await venueService.deleteVenueDocument(docId)
    documentos.value = documentos.value.filter(d => d.id !== docId)
  } catch (err) {
    console.error('Error al eliminar documento de la sede', err)
  }
}
</script>
