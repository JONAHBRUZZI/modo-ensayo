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
            <h3 class="text-white font-semibold text-lg">{{ venue.name }}</h3>
            <p class="text-gray-400 text-sm">{{ venue.address }}, {{ venue.city }}</p>
          </div>
          <EstadoBadge :status="venue.status" />
        </div>
        <div v-if="venue.tipo" class="mt-2">
          <span class="badge badge-blue">{{ venue.tipo === 'HOME_STUDIO' ? 'HomeStudio' : 'Sede' }}</span>
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
            <input v-model="formDatos.address" required class="input-field" />
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Descripcion</label>
          <textarea v-model="formDatos.description" rows="3" class="input-field" placeholder="Describe tu sede..."></textarea>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Telefono</label>
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

      <!-- Sede aprobada: datos estructurales solo lectura -->
      <div v-else class="card">
        <div class="flex items-start gap-3 mb-4">
          <svg class="w-5 h-5 text-green-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <div>
            <p class="text-green-400 font-medium">Sede verificada y aprobada</p>
            <p class="text-gray-500 text-sm">Los datos estructurales (nombre, direccion) no pueden modificarse. Contacta al Administrador General si necesitas cambios.</p>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3 text-sm">
          <div><span class="text-gray-500">Descripcion:</span> <span class="text-gray-300">{{ venue.description || '—' }}</span></div>
          <div><span class="text-gray-500">Telefono:</span> <span class="text-gray-300">{{ venue.phone || '—' }}</span></div>
          <div><span class="text-gray-500">Email:</span> <span class="text-gray-300">{{ venue.email || '—' }}</span></div>
        </div>
      </div>

      <!-- Documentos de la sede -->
      <div class="card space-y-4">
        <h3 class="text-white font-medium">Documentos de la sede</h3>
        <p class="text-gray-500 text-xs">Sube permisos, certificados o documentos requeridos para la aprobación. El Admin General los revisará.</p>

        <!-- Lista de documentos existentes -->
        <div v-if="documentos.length > 0" class="space-y-2">
          <div v-for="doc in documentos" :key="doc.id"
               class="flex items-center justify-between p-3 bg-dark-bg rounded-lg border border-dark-border">
            <div class="flex items-center gap-3">
              <svg class="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              <div>
                <p class="text-sm text-white">{{ doc.nombre || 'Documento' }}</p>
                <span :class="{
                  'text-yellow-400': doc.estado === 'PENDIENTE',
                  'text-green-400': doc.estado === 'APROBADO',
                  'text-red-400': doc.estado === 'RECHAZADO'
                }" class="text-xs">{{ doc.estado }}</span>
                <span v-if="doc.motivoRechazo" class="text-xs text-red-400 ml-2">— {{ doc.motivoRechazo }}</span>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <a :href="doc.fileUrl" target="_blank" class="text-primary text-xs hover:underline">Ver</a>
              <button @click="eliminarDocumento(doc.id)" class="text-red-400 text-xs hover:underline">Eliminar</button>
            </div>
          </div>
        </div>
        <p v-else class="text-gray-500 text-sm">No hay documentos subidos aún.</p>

        <!-- Subir nuevo documento -->
        <div class="border border-dark-border rounded-lg p-4 space-y-3">
          <p class="text-sm text-gray-300 font-medium">Subir documento</p>
          <div>
            <label class="block text-xs text-gray-400 mb-1">Tipo de documento</label>
            <select v-model="nuevoDoc.tipo" class="input-field">
              <option value="">Seleccionar tipo</option>
              <option value="RUT_EMPRESA">RUT Empresa</option>
              <option value="CEDULA_IDENTIDAD">Cedula Identidad</option>
              <option value="INICIO_ACTIVIDADES_F4415">Inicio Actividades F4415</option>
              <option value="CERTIFICADO_SITUACION_TRIBUTARIA">Cert. Situacion Tributaria</option>
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

      <!-- Redes sociales y contacto web (siempre editable) -->
      <form @submit.prevent="saveSocial" class="card space-y-4">
        <h3 class="text-white font-medium">Redes sociales y contacto web</h3>
        <p class="text-gray-500 text-xs">Estos datos son siempre editables y se muestran en tu perfil publico.</p>

        <div v-if="venue.status === 'APROBADA'" class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Descripcion</label>
            <textarea v-model="formSocial.description" rows="2" class="input-field" placeholder="Describe tu sede..."></textarea>
          </div>
          <div class="space-y-3">
            <div>
              <label class="block text-sm font-medium text-gray-300 mb-1">Telefono</label>
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
import { ref, reactive, onMounted } from 'vue'
import venueService from '@/services/venueService'
import api from '@/services/api'
import EstadoBadge from '@/components/EstadoBadge.vue'

const venue = ref(null)
const loading = ref(true)

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

onMounted(async () => {
  try {
    const venues = await venueService.getMyVenues()
    const vArr = Array.isArray(venues) ? venues : venues?.content || []
    if (vArr.length > 0) {
      venue.value = vArr[0]
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
      } catch { documentos.value = [] }
    }
  } catch {}
  loading.value = false
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
    const formData = new FormData()
    formData.append('file', file)
    formData.append('type', 'documents')
    const res = await api.post('/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    nuevoDoc.fileUrl = res.data.fileUrl || res.data.url
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
  } catch {}
}
</script>
