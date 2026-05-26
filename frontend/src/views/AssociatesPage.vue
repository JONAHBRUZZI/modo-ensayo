<template>
  <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
    <div class="flex items-center justify-between mb-8">
      <h1 class="text-3xl font-bold text-white">Asociados</h1>
      <button @click="showForm = !showForm" class="btn-primary">
        {{ showForm ? 'Cancelar' : 'Nuevo Asociado' }}
      </button>
    </div>

    <div v-if="showForm" class="card mb-8">
      <form @submit.prevent="createAssociate" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1">Email del Asociado</label>
          <input type="email" v-model="newEmail" required class="input-field" placeholder="asociado@email.com" />
        </div>
        <button type="submit" :disabled="creating" class="btn-primary">
          {{ creating ? 'Agregando...' : 'Agregar Asociado' }}
        </button>
      </form>
    </div>

    <div v-if="loading" class="text-center text-gray-400 py-20">Cargando...</div>
    <div v-else-if="associates.length === 0" class="card text-center py-12">
      <p class="text-gray-400">No tienes asociados registrados.</p>
    </div>
    <div v-else class="space-y-3">
      <div v-for="a in associates" :key="a.id" class="card flex items-center justify-between">
        <div>
          <p class="text-white font-medium">{{ a.email || a.name }}</p>
          <p class="text-gray-400 text-sm">{{ a.status || 'Activo' }}</p>
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
import { ref, onMounted } from 'vue'
import associateService from '@/services/associateService'
import ConfirmModal from '@/components/ConfirmModal.vue'

const associates = ref([])
const loading = ref(true)
const showForm = ref(false)
const newEmail = ref('')
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
  creating.value = true
  try {
    await associateService.createAssociate({ email: newEmail.value })
    newEmail.value = ''
    showForm.value = false
    await loadAssociates()
  } catch {} finally {
    creating.value = false
  }
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
</script>
