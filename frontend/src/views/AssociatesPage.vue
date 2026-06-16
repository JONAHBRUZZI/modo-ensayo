<template>
  <div
    v-motion
    :initial="{ opacity: 0, y: 16 }"
    :enter="{ opacity: 1, y: 0, transition: { duration: 400 } }"
    class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10"
  >
    <div class="flex items-center justify-between mb-8">
      <h1 class="text-3xl font-bold text-white">Asociados</h1>
      <button @click="showForm = !showForm" class="btn-primary">
        {{ showForm ? 'Cancelar' : 'Nuevo Asociado' }}
      </button>
    </div>

    <div v-if="showForm" class="card mb-8">
      <form @submit.prevent="createAssociate" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Nombre completo *</label>
          <input type="text" v-model="form.name" required class="input-field" placeholder="Nombre del asociado" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Relacion *</label>
          <select v-model="form.relationship" required class="input-field">
            <option value="">Seleccionar</option>
            <option value="HIJO">Hijo/a</option>
            <option value="FAMILIAR">Familiar</option>
            <option value="AMIGO">Amigo/a</option>
            <option value="OTRO">Otro</option>
          </select>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Fecha de nacimiento *</label>
            <input type="date" v-model="form.birthDate" required class="input-field" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">RUT *</label>
            <input type="text" v-model="form.rut" required class="input-field" placeholder="12.345.678-9" @input="formatearRut" @blur="validarRut" />
            <p v-if="rutError" class="text-xs text-red-400 mt-1">{{ rutError }}</p>
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Email <span class="text-gray-500">(opcional)</span></label>
          <input type="email" v-model="form.email" class="input-field" placeholder="asociado@email.com" />
        </div>
        <button type="submit" :disabled="creating" class="btn-primary">
          {{ creating ? 'Agregando...' : 'Agregar Asociado' }}
        </button>
      </form>
    </div>

    <div v-if="loading" class="text-center text-gray-500 py-20">
      <div class="inline-block w-6 h-6 border-2 border-primary/40 border-t-primary rounded-full animate-spin mb-3"></div>
      <p class="text-sm">Cargando...</p>
    </div>
    <div v-else-if="associates.length === 0" class="card text-center py-12">
      <p class="text-gray-400">No tienes asociados registrados.</p>
    </div>
    <div v-else class="space-y-3">
      <div v-for="a in associates" :key="a.id" class="card flex items-center justify-between">
        <div>
          <p class="text-white font-medium">{{ a.name || a.email || 'Sin nombre' }}</p>
          <p class="text-gray-400 text-sm">{{ a.relationship || '' }}<span v-if="a.birthDate"> · {{ formatDate(a.birthDate) }}</span></p>
          <p v-if="a.rut" class="text-gray-500 text-xs">RUT: {{ a.rut }}</p>
        </div>
        <button @click="confirmDelete(a)" class="text-red-400 hover:text-red-300">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
        </button>
      </div>
    </div>

    <ConfirmModal
      :show="showConfirm"
      title="Eliminar Asociado"
      :message="`Eliminar a ${associateToDelete?.email}?`"
      @close="showConfirm = false"
      @confirm="deleteAssociate"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import associateService from '@/services/associateService'
import ConfirmModal from '@/components/ConfirmModal.vue'

const associates = ref([])
const loading = ref(true)
const showForm = ref(false)
const form = reactive({ name: '', relationship: '', birthDate: '', rut: '', email: '' })
const rutError = ref('')
const creating = ref(false)
const showConfirm = ref(false)
const associateToDelete = ref(null)

onMounted(async () => {
  await loadAssociates()
})

async function loadAssociates() {
  loading.value = true
  try {
    const data = await associateService.getAssociates()
    associates.value = Array.isArray(data) ? data : data.content || []
  } catch {
    associates.value = []
  } finally {
    loading.value = false
  }
}

async function createAssociate() {
  if (rutError.value) return
  creating.value = true
  try {
    await associateService.createAssociate({
      name: form.name,
      relationship: form.relationship,
      birthDate: form.birthDate || null,
      rut: form.rut || null,
      email: form.email || null
    })
    Object.assign(form, { name: '', relationship: '', birthDate: '', rut: '', email: '' })
    rutError.value = ''
    showForm.value = false
    await loadAssociates()
  } catch {} finally {
    creating.value = false
  }
}

function formatearRut() {
  if (!form.rut) return
  let valor = form.rut.replace(/[^0-9kK]/g, '')
  if (valor.length < 2) { form.rut = valor; return }
  const dv = valor.slice(-1)
  let cuerpo = valor.slice(0, -1)
  cuerpo = cuerpo.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  form.rut = cuerpo + '-' + dv
}

function validarRut() {
  rutError.value = ''
  if (!form.rut) return
  const rut = form.rut.replace(/[^0-9kK]/g, '')
  if (rut.length < 2) { rutError.value = 'RUT invalido'; return }
  const dv = rut.slice(-1).toUpperCase()
  const cuerpo = rut.slice(0, -1)
  let suma = 0
  let multiplo = 2
  for (let i = cuerpo.length - 1; i >= 0; i--) {
    suma += parseInt(cuerpo[i]) * multiplo
    multiplo = multiplo < 7 ? multiplo + 1 : 2
  }
  const dvEsperado = 11 - (suma % 11)
  const dvCalculado = dvEsperado === 11 ? '0' : dvEsperado === 10 ? 'K' : dvEsperado.toString()
  if (dv !== dvCalculado) rutError.value = 'RUT invalido: digito verificador no coincide'
}

function confirmDelete(associate) {
  associateToDelete.value = associate
  showConfirm.value = true
}

async function deleteAssociate() {
  try {
    await associateService.deleteAssociate(associateToDelete.value.id)
    showConfirm.value = false
    await loadAssociates()
  } catch {}
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-CL', { day: 'numeric', month: 'short', year: 'numeric' })
}
</script>
