<template>
  <div class="max-w-2xl mx-auto space-y-8">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold text-white mb-1">Mis Familiares</h1>
        <p class="text-gray-400 text-sm">Gestiona tus asociados para inscribirlos en clases</p>
      </div>
      <button @click="showForm = true"
        class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">
        + Agregar
      </button>
    </div>

    <!-- Lista de familiares -->
    <div v-if="associates.length > 0" class="space-y-3">
      <div v-for="a in associates" :key="a.id"
        class="bg-[#161824] rounded-xl border border-white/10 p-4 flex items-center justify-between hover:border-white/20 transition-colors">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 bg-gradient-to-br from-purple-600 to-indigo-600 rounded-full flex items-center justify-center flex-shrink-0">
            <span class="text-sm font-bold text-white">{{ a.name?.charAt(0)?.toUpperCase() }}</span>
          </div>
          <div>
            <p class="font-semibold text-white text-sm">{{ a.name }}</p>
            <div class="flex items-center gap-2 mt-0.5">
              <span class="text-xs text-gray-500">{{ a.relation }}</span>
              <span v-if="a.documento?.numero" class="text-gray-700 text-xs">·</span>
              <span v-if="a.documento?.numero" class="text-xs text-gray-500 font-mono">
                {{ a.documento.tipo === 'PASAPORTE' ? '🌐' : '🇨🇱' }} {{ a.documento.numero }}
              </span>
              <span v-if="a.birthdate" class="text-gray-700 text-xs">·</span>
              <span v-if="a.birthdate" class="text-xs text-gray-500">{{ calcularEdad(a.birthdate) }} años</span>
            </div>
          </div>
        </div>
        <button @click="confirmarEliminar(a)"
          class="text-sm text-red-400 hover:text-red-300 hover:bg-red-500/10 px-3 py-1.5 rounded-lg transition-colors">
          Eliminar
        </button>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else class="text-center py-16">
      <div class="inline-flex items-center justify-center w-16 h-16 bg-white/5 rounded-full border border-white/10 mb-4">
        <svg class="w-8 h-8 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </div>
      <p class="text-gray-400 text-sm">No tienes familiares registrados</p>
      <button @click="showForm = true" class="mt-3 text-sm text-indigo-400 hover:text-indigo-300 transition-colors">
        Agregar primer familiar
      </button>
    </div>

    <!-- Modal agregar -->
    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70" @click.self="cerrarModal">
      <div class="bg-[#161824] rounded-2xl border border-white/10 p-6 w-full max-w-md mx-4 space-y-4 shadow-2xl">
        <h3 class="text-lg font-bold text-white">Agregar Familiar</h3>

        <!-- Nombre -->
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Nombre completo *</label>
          <input v-model="form.name" placeholder="Ana González"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm transition-all" />
        </div>

        <!-- Parentesco -->
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Parentesco *</label>
          <select v-model="form.relation"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
            <option value="">Selecciona parentesco</option>
            <option>Hijo/a</option>
            <option>Pareja</option>
            <option>Cónyuge</option>
            <option>Hermano/a</option>
            <option>Padre/Madre</option>
            <option>Otro</option>
          </select>
        </div>

        <!-- Fecha de nacimiento -->
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Fecha de nacimiento</label>
          <input v-model="form.birthdate" type="date"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white text-sm [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
          <p v-if="form.birthdate" class="text-xs text-gray-500 mt-1">
            Edad: {{ calcularEdad(form.birthdate) }} años
          </p>
        </div>

        <!-- Documento de identidad -->
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Documento de identidad</label>
          <DocumentoIdentidad v-model="form.documento" />
        </div>

        <div class="flex gap-2 pt-1">
          <button @click="cerrarModal"
            class="flex-1 py-2.5 border border-white/15 text-gray-300 rounded-lg text-sm hover:bg-white/5 transition-colors">
            Cancelar
          </button>
          <button @click="addAssociate" :disabled="!canSubmit || saving"
            class="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium disabled:opacity-50 transition-colors">
            {{ saving ? 'Guardando...' : 'Guardar' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Confirm eliminar -->
    <ConfirmModal
      v-model="showConfirm"
      title="¿Eliminar familiar?"
      :message="`Se eliminará a ${asociadoAEliminar?.name} de tu lista de familiares.`"
      confirm-text="Eliminar"
      type="danger"
      @confirm="removeAssociate"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { associateService } from '../services/associateService'
import DocumentoIdentidad from '../components/DocumentoIdentidad.vue'
import ConfirmModal from '../components/ConfirmModal.vue'

const associates = ref([])
const showForm = ref(false)
const showConfirm = ref(false)
const saving = ref(false)
const asociadoAEliminar = ref(null)

const form = ref({
  name: '',
  relation: '',
  birthdate: '',
  documento: { tipo: 'RUT', numero: '', valido: false },
})

const canSubmit = computed(() =>
  form.value.name?.trim() && form.value.relation
)

onMounted(async () => {
  try { associates.value = await associateService.list() } catch { /* ignore */ }
})

const addAssociate = async () => {
  if (!canSubmit.value) return
  saving.value = true
  try {
    const payload = {
      name: form.value.name,
      relation: form.value.relation,
      birthdate: form.value.birthdate || null,
      rut: form.value.documento.tipo === 'RUT' ? form.value.documento.numero : null,
      pasaporte: form.value.documento.tipo === 'PASAPORTE' ? form.value.documento.numero : null,
      tipoDocumento: form.value.documento.tipo,
      numeroDocumento: form.value.documento.numero,
    }
    const a = await associateService.create(payload)
    associates.value.push(a)
    cerrarModal()
  } catch {
    alert('Error al agregar familiar')
  } finally {
    saving.value = false
  }
}

const confirmarEliminar = (a) => {
  asociadoAEliminar.value = a
  showConfirm.value = true
}

const removeAssociate = async () => {
  if (!asociadoAEliminar.value) return
  try {
    await associateService.delete(asociadoAEliminar.value.id)
    associates.value = associates.value.filter(a => a.id !== asociadoAEliminar.value.id)
  } catch {
    alert('Error al eliminar')
  } finally {
    asociadoAEliminar.value = null
  }
}

const cerrarModal = () => {
  showForm.value = false
  form.value = { name: '', relation: '', birthdate: '', documento: { tipo: 'RUT', numero: '', valido: false } }
}

const calcularEdad = (fechaNac) => {
  if (!fechaNac) return null
  const hoy = new Date()
  const nac = new Date(fechaNac)
  let edad = hoy.getFullYear() - nac.getFullYear()
  const m = hoy.getMonth() - nac.getMonth()
  if (m < 0 || (m === 0 && hoy.getDate() < nac.getDate())) edad--
  return edad
}
</script>
