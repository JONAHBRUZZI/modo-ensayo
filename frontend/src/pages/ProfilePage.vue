<template>
  <div class="max-w-2xl mx-auto">
    <h1 class="text-3xl font-bold mb-8">Mi Perfil</h1>
    <div v-if="profile" class="bg-white rounded-lg shadow-md p-6">
      <div class="mb-4">
        <label class="block text-gray-700 mb-2">Nombre</label>
        <input v-model="profile.fullName" class="w-full px-3 py-2 border rounded-md bg-gray-50" readonly />
      </div>
      <div class="mb-4">
        <label class="block text-gray-700 mb-2">Email</label>
        <input v-model="profile.email" class="w-full px-3 py-2 border rounded-md bg-gray-50" readonly />
      </div>
      <div class="mb-4">
        <label class="block text-gray-700 mb-2">Telefono</label>
        <input v-model="editForm.phone" class="w-full px-3 py-2 border rounded-md" />
      </div>
      <button @click="updateProfile" class="bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700">
        Actualizar Perfil
      </button>
      <div class="mt-6">
        <h3 class="text-lg font-semibold mb-2">Roles</h3>
        <div class="flex gap-2">
          <span v-for="role in profile.roles" :key="role" class="bg-indigo-100 text-indigo-800 px-3 py-1 rounded-full text-sm">
            {{ role }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userService } from '../services/userService'

const profile = ref(null)
const editForm = ref({ phone: '' })

onMounted(async () => {
  try {
    profile.value = await userService.getProfile()
    editForm.value.phone = profile.value.phone || ''
  } catch (error) {
    console.error('Error loading profile:', error)
  }
})

const updateProfile = async () => {
  try {
    profile.value = await userService.updateProfile({ fullName: profile.value.fullName, phone: editForm.value.phone })
    alert('Perfil actualizado')
  } catch (error) {
    alert('Error al actualizar perfil')
  }
}
</script>
