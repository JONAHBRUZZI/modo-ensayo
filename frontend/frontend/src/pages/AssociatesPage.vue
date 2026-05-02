<template>
  <div class="max-w-2xl mx-auto space-y-8">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold text-gray-900 mb-2">Mis Familiares</h1>
        <p class="text-gray-600">Gestiona tus asociados para inscribirlos en clases</p>
      </div>
      <button @click="showForm = true" class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">+ Agregar</button>
    </div>

    <div v-if="associates.length > 0" class="space-y-3">
      <div v-for="a in associates" :key="a.id" class="bg-white rounded-xl shadow-md p-4 flex items-center justify-between">
        <div>
          <p class="font-semibold">{{ a.name }}</p>
          <p class="text-sm text-gray-500">{{ a.relation }} · {{ a.rut || 'Sin RUT' }}</p>
        </div>
        <button @click="removeAssociate(a.id)" class="text-red-600 hover:text-red-800 text-sm">Eliminar</button>
      </div>
    </div>
    <p v-else class="text-gray-500 text-center py-8">No tienes familiares registrados</p>

    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50" @click.self="showForm = false">
      <div class="bg-white rounded-xl p-6 w-full max-w-md space-y-4">
        <h3 class="text-lg font-semibold">Agregar Familiar</h3>
        <input v-model="form.name" placeholder="Nombre completo" class="w-full px-3 py-2 border rounded-lg" />
        <input v-model="form.relation" placeholder="Parentesco (hijo/a, pareja)" class="w-full px-3 py-2 border rounded-lg" />
        <input v-model="form.rut" placeholder="RUT" class="w-full px-3 py-2 border rounded-lg" />
        <button @click="addAssociate" :disabled="!form.name" class="w-full py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50">Guardar</button>
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
  } catch (e) { alert('Error al agregar') }
}

const removeAssociate = async (id) => {
  try {
    await associateService.delete(id)
    associates.value = associates.value.filter(a => a.id !== id)
  } catch (e) { alert('Error al eliminar') }
}
</script>