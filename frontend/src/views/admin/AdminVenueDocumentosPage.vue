<template>
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="flex items-center gap-3 mb-8">
      <router-link to="/admin/sedes" class="text-gray-400 hover:text-white transition-colors">
        <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </router-link>
      <div>
        <h1 class="text-2xl font-bold text-white">Documentos de la sede</h1>
        <p v-if="venueName" class="text-gray-400 text-sm mt-0.5">{{ venueName }}</p>
      </div>
    </div>

    <div v-if="loading" class="text-center py-20 text-gray-500">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando documentos...</p>
    </div>

    <div v-else-if="docs.length === 0" class="card text-center py-16">
      <p class="text-gray-400">No hay documentos subidos para esta sede.</p>
    </div>

    <div v-else class="space-y-4">
      <div v-for="doc in docs" :key="doc.id" class="card">
        <div class="flex items-start justify-between gap-4">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-white font-medium text-sm">{{ tipoLabel(doc.tipo) }}</span>
              <span :class="estadoClass(doc.estado)" class="text-xs px-2 py-0.5 rounded-full font-medium">
                {{ estadoLabel(doc.estado) }}
              </span>
            </div>
            <p v-if="doc.nombre" class="text-gray-400 text-xs truncate">{{ doc.nombre }}</p>
            <p class="text-gray-500 text-xs mt-1">{{ formatDate(doc.createdAt) }}</p>
            <p v-if="doc.motivoRechazo" class="text-red-400 text-xs mt-1">
              Motivo: {{ doc.motivoRechazo }}
            </p>
          </div>

          <div class="flex items-center gap-2 flex-shrink-0">
            <a v-if="signedUrls[doc.id]" :href="signedUrls[doc.id]" target="_blank" rel="noopener noreferrer"
               class="text-xs px-3 py-1.5 rounded-lg bg-[var(--bg-elevated)] border border-white/10 text-gray-300 hover:text-white transition-colors">
              Ver archivo
            </a>
            <span v-else class="text-xs text-gray-500">—</span>

            <button v-if="doc.estado !== 'APROBADO'"
                    @click="review(doc.id, 'approve')"
                    :disabled="reviewing === doc.id"
                    class="text-xs px-3 py-1.5 rounded-lg bg-green-500/20 text-green-400 hover:bg-green-500/30 transition-colors disabled:opacity-50">
              Aprobar
            </button>
            <button v-if="doc.estado !== 'RECHAZADO'"
                    @click="openReject(doc)"
                    :disabled="reviewing === doc.id"
                    class="text-xs px-3 py-1.5 rounded-lg bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors disabled:opacity-50">
              Rechazar
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal rechazo -->
    <div v-if="rejectTarget" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
      <div class="bg-[var(--bg-card)] rounded-2xl p-6 w-full max-w-md border border-white/10">
        <h3 class="text-white font-semibold mb-3">Rechazar documento</h3>
        <p class="text-gray-400 text-sm mb-4">{{ tipoLabel(rejectTarget.tipo) }} · {{ rejectTarget.nombre }}</p>
        <textarea v-model="rejectMotivo" rows="3" placeholder="Motivo del rechazo (opcional)"
                  class="w-full bg-[var(--bg-elevated)] border border-white/10 rounded-lg px-3 py-2 text-white text-sm resize-none focus:outline-none focus:border-primary/50 mb-4" />
        <div class="flex gap-3 justify-end">
          <button @click="rejectTarget = null; rejectMotivo = ''"
                  class="text-sm px-4 py-2 rounded-lg bg-[var(--bg-elevated)] text-gray-300 hover:text-white transition-colors">
            Cancelar
          </button>
          <button @click="confirmReject"
                  class="text-sm px-4 py-2 rounded-lg bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors">
            Rechazar
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import adminService from '@/services/adminService'
import { supabase } from '@/services/supabase'

const route = useRoute()
const venueId = route.params.venueId

const docs = ref([])
const signedUrls = ref({})
const venueName = ref('')
const loading = ref(true)
const reviewing = ref(null)
const rejectTarget = ref(null)
const rejectMotivo = ref('')

onMounted(async () => {
  try {
    const { data: venue } = await supabase.from('venues').select('name').eq('id', venueId).single()
    venueName.value = venue?.name || ''
  } catch {}

  try {
    docs.value = await adminService.getVenueDocuments(venueId)
    await loadSignedUrls()
  } catch (e) {
    console.error('Error cargando documentos:', e)
  } finally {
    loading.value = false
  }
})

async function loadSignedUrls() {
  const results = {}
  await Promise.all(docs.value.map(async (doc) => {
    if (!doc.fileUrl) return
    const { data } = await supabase.storage
      .from('venue-documents')
      .createSignedUrl(doc.fileUrl, 3600)
    if (data?.signedUrl) results[doc.id] = data.signedUrl
  }))
  signedUrls.value = results
}

async function review(docId, action, motivo = null) {
  reviewing.value = docId
  try {
    const updated = await adminService.reviewDocument(docId, action, motivo)
    const idx = docs.value.findIndex(d => d.id === docId)
    if (idx !== -1) docs.value[idx] = updated
  } catch (e) {
    console.error('Error al revisar documento:', e)
  } finally {
    reviewing.value = null
  }
}

function openReject(doc) {
  rejectTarget.value = doc
  rejectMotivo.value = ''
}

async function confirmReject() {
  const doc = rejectTarget.value
  rejectTarget.value = null
  await review(doc.id, 'reject', rejectMotivo.value || null)
  rejectMotivo.value = ''
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('es-CL', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function tipoLabel(tipo) {
  const labels = {
    RUT_EMPRESA: 'RUT de la Empresa',
    CEDULA_IDENTIDAD: 'Cédula de Identidad',
    INICIO_ACTIVIDADES_F4415: 'Inicio de Actividades (SII)',
    CERTIFICADO_SITUACION_TRIBUTARIA: 'Certificado Situación Tributaria',
    CONTRATO_ARRIENDO: 'Contrato de Arriendo',
    COMPROBANTE_DOMICILIO: 'Comprobante de Domicilio',
    PERMISO_MUNICIPAL: 'Permiso Municipal',
    CARPETA_TRIBUTARIA_ELECTRONICA: 'Carpeta Tributaria Electrónica',
    ESCRITURA_CONSTITUCION: 'Escritura de Constitución',
    AUTORIZACION_NOTARIAL_PROPIETARIO: 'Autorización Notarial del Propietario',
    CERTIFICADO_IVA: 'Certificado IVA',
    PATENTE_COMERCIAL: 'Patente Comercial',
    RESOLUCION_SANITARIA: 'Resolución Sanitaria',
    OTRO: 'Otro documento',
  }
  return labels[tipo] || tipo
}

function estadoLabel(estado) {
  return { PENDIENTE: 'Pendiente', APROBADO: 'Aprobado', RECHAZADO: 'Rechazado' }[estado] || estado
}

function estadoClass(estado) {
  return {
    PENDIENTE: 'bg-yellow-500/20 text-yellow-400',
    APROBADO: 'bg-green-500/20 text-green-400',
    RECHAZADO: 'bg-red-500/20 text-red-400',
  }[estado] || 'bg-gray-500/20 text-gray-400'
}
</script>
