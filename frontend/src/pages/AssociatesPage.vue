<template>
  <div class="max-w-2xl mx-auto space-y-8">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold text-white mb-2">Mis Familiares</h1>
        <p class="text-gray-400">Gestiona tus asociados para inscribirlos en clases</p>
      </div>
      <button @click="showForm = true"
              class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">
        + Agregar
      </button>
    </div>

    <div v-if="associates.length > 0" class="space-y-3">
      <div v-for="a in associates" :key="a.id"
           class="bg-[#161824] rounded-xl border border-white/10 p-4 flex items-center justify-between hover:border-white/20 transition-colors">
        <div>
          <p class="font-semibold text-white text-sm">{{ a.name }}</p>
          <p class="text-xs text-gray-500 mt-0.5">{{ a.relation }} · {{ a.rut || 'Sin RUT' }}</p>
        </div>
        <button @click="removeAssociate(a.id)"
                class="text-sm text-red-400 hover:text-red-300 hover:bg-red-500/10 px-3 py-1.5 rounded-lg transition-colors">
          Eliminar
        </button>
      </div>
    </div>

    <div v-else class="text-center py-16">
      <div class="inline-flex items-center justify-center w-16 h-16 bg-white/5 rounded-full border border-white/10 mb-4">
        <svg class="w-8 h-8 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </div>
      <p class="text-gray-400 text-sm">No tienes familiares registrados</p>
    </div>

    <!-- Modal -->
    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70" @click.self="showForm = false">
      <div class="bg-[#161824] rounded-2xl border border-white/10 p-6 w-full max-w-md mx-4 space-y-4 shadow-2xl">
        <h3 class="text-lg font-bold text-white">Agregar Familiar</h3>
        <input v-model="form.name" placeholder="Nombre completo"
               class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm" />
        <input v-model="form.relation" placeholder="Parentesco (hijo/a, pareja)"
               class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm" />
        <input v-model="form.rut" placeholder="RUT"
               class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm" />
        <div class="flex gap-2 pt-1">
          <button @click="showForm = false"
                  class="flex-1 py-2.5 border border-white/15 text-gray-300 rounded-lg text-sm hover:bg-white/5 transition-colors">
            Cancelar
          </button>
          <button @click="addAssociate" :disabled="!form.name"
                  class="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium disabled:opacity-50 transition-colors">
            Guardar
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { associateService } from '../services/associateService'

const associates = ref([])
const showForm = ref(false)
const form = ref({ name: '', relation: '', rut: '' })

onMounted(async () => {
  try { associates.value = await associateService.list() } catch (e) { console.error(e) }
})

const addAssociate = async () => {
  try {
    const a = await associateService.create(form.value)
    associates.value.push(a)
    showForm.value = false
    form.value = { name: '', relation: '', rut: '' }
  } catch { alert('Error al agregar') }
}

const removeAssociate = async (id) => {
  try {
    await associateService.delete(id)
    associates.value = associates.value.filter(a => a.id !== id)
  } catch { alert('Error al eliminar') }
}
</script>
