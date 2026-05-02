<template>
  <div class="max-w-2xl mx-auto space-y-8">
    <div>
      <h1 class="text-3xl font-bold text-gray-900 mb-2">Registrar Sede</h1>
      <p class="text-gray-600">Completa los datos de tu sede para solicitar aprobacion</p>
    </div>

    <form @submit.prevent="submitVenue" class="bg-white rounded-xl shadow-md p-6 space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Nombre de la Sede *</label>
        <input v-model="form.name" required class="w-full px-3 py-2 border border-gray-300 rounded-lg" placeholder="Ej: Sede Centro" />
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Telefono</label>
          <input v-model="form.phone" class="w-full px-3 py-2 border border-gray-300 rounded-lg" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input v-model="form.email" class="w-full px-3 py-2 border border-gray-300 rounded-lg" />
        </div>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Direccion</label>
        <input v-model="form.address" class="w-full px-3 py-2 border border-gray-300 rounded-lg" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Descripcion</label>
        <textarea v-model="form.description" rows="3" class="w-full px-3 py-2 border border-gray-300 rounded-lg"></textarea>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Foto de la Sede</label>
        <div class="flex items-center space-x-4">
          <label class="px-4 py-2 bg-gray-100 rounded-lg hover:bg-gray-200 cursor-pointer">
            <input type="file" @change="handleImage" accept="image/*" class="hidden" />
            {{ imageFile ? imageFile.name : 'Seleccionar imagen' }}
          </label>
        </div>
        <img v-if="imagePreview" :src="imagePreview" class="mt-2 h-32 rounded-lg" />
      </div>
      <button type="submit" :disabled="isSubmitting" class="w-full py-3 bg-indigo-600 text-white rounded-lg font-semibold hover:bg-indigo-700 disabled:opacity-50">
        {{ isSubmitting ? 'Enviando solicitud...' : 'Solicitar Aprobacion' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { venueService } from '../services/venueService'
import { uploadService } from '../services/uploadService'

const form = ref({ name: '', address: '', description: '', phone: '', email: '', imageUrl: '' })
const imageFile = ref(null)
const imagePreview = ref(null)
const isSubmitting = ref(false)

const handleImage = (event) => {
  imageFile.value = event.target.files[0]
  if (imageFile.value) {
    imagePreview.value = URL.createObjectURL(imageFile.value)
  }
}

const submitVenue = async () => {
  isSubmitting.value = true
  try {
    if (imageFile.value) {
      const { url } = await uploadService.upload(imageFile.value, 'venues')
      form.value.imageUrl = url
    }
    await venueService.create(form.value)
    alert('Sede registrada. Espera la aprobacion.')
  } catch (e) {
    alert('Error al registrar')
  } finally {
    isSubmitting.value = false
  }
}
</script>