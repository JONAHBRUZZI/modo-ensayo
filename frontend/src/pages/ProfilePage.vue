<template>
  <div class="max-w-2xl mx-auto">
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-gray-900 mb-2">Mi Perfil</h1>
      <p class="text-gray-600">Gestiona tu información personal</p>
    </div>

    <!-- Profile Card -->
    <div class="bg-white rounded-xl shadow-md overflow-hidden border border-gray-100">
      <!-- Banner -->
      <div class="h-32 bg-gradient-to-r from-indigo-500 to-purple-600"></div>
      
      <!-- Avatar & Info -->
      <div class="px-6 pb-6">
        <div class="relative flex justify-between items-end -mt-12 mb-4">
          <div class="w-24 h-24 bg-white rounded-xl shadow-lg flex items-center justify-center border-4 border-white">
            <span class="text-4xl font-bold text-indigo-600">
              {{ user?.fullName?.charAt(0)?.toUpperCase() || 'U' }}
            </span>
          </div>
          <button @click="isEditing = !isEditing" 
                  class="px-4 py-2 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-700 transition-colors">
            {{ isEditing ? 'Cancelar' : 'Editar Perfil' }}
          </button>
        </div>

        <!-- View Mode -->
        <div v-if="!isEditing" class="space-y-4">
          <div>
            <label class="text-sm font-medium text-gray-500">Nombre Completo</label>
            <p class="text-lg font-semibold text-gray-900">{{ profile?.fullName }}</p>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-500">Email</label>
            <p class="text-gray-900">{{ profile?.email }}</p>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-500">Teléfono</label>
            <p class="text-gray-900">{{ profile?.phone || 'No registrado' }}</p>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-500">Roles</label>
            <div class="flex flex-wrap gap-2 mt-1">
              <span v-for="role in profile?.roles" :key="role" 
                    class="px-3 py-1 bg-indigo-100 text-indigo-700 text-sm font-medium rounded-full">
                {{ role }}
              </span>
            </div>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-500">Score promedio como alumno</label>
            <p class="text-gray-900">
              {{ profile?.averageStudentScore ? Number(profile.averageStudentScore).toFixed(2) : 'Sin evaluaciones' }}
              <span class="text-sm text-gray-500">({{ profile?.totalStudentReviews || 0 }} evaluaciones)</span>
            </p>
          </div>
        </div>

        <!-- Edit Mode -->
        <form v-else @submit.prevent="updateProfile" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Nombre Completo</label>
            <input v-model="editForm.fullName" type="text" required
                   class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input :value="profile?.email" type="email" disabled
                   class="w-full px-3 py-2 border border-gray-200 rounded-lg bg-gray-50 text-gray-500">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
            <input v-model="editForm.phone" type="tel"
                   class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                   placeholder="+56912345678">
          </div>
          <div class="flex space-x-3 pt-4">
            <button type="submit" 
                    :disabled="isSaving"
                    class="flex-1 py-2 px-4 bg-indigo-600 text-white rounded-lg font-medium hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
              {{ isSaving ? 'Guardando...' : 'Guardar Cambios' }}
            </button>
            <button type="button" @click="isEditing = false"
                    class="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition-colors">
              Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Quick Links -->
    <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
      <router-link to="/classes" class="flex items-center p-4 bg-white rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
        <div class="p-2 bg-indigo-100 rounded-lg mr-4">
          <svg class="w-5 h-5 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
        </div>
        <div><h4 class="font-semibold text-gray-900">Mis Clases</h4><p class="text-sm text-gray-500">Ver inscripciones activas</p></div>
      </router-link>
      <router-link to="/profile/refund-method" class="flex items-center p-4 bg-white rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
        <div class="p-2 bg-yellow-100 rounded-lg mr-4">
          <svg class="w-5 h-5 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a1 1 0 11-2 0 1 1 0 012 0z" />
          </svg>
        </div>
        <div><h4 class="font-semibold text-gray-900">Metodo de Devolucion</h4><p class="text-sm text-gray-500">Configurar reembolsos</p></div>
      </router-link>
      <router-link to="/profile/identity" class="flex items-center p-4 bg-white rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
        <div class="p-2 bg-blue-100 rounded-lg mr-4">
          <svg class="w-5 h-5 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
        </div>
        <div><h4 class="font-semibold text-gray-900">Verificar Identidad</h4><p class="text-sm text-gray-500">Subir documento</p></div>
      </router-link>
      <router-link to="/associates" class="flex items-center p-4 bg-white rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
        <div class="p-2 bg-green-100 rounded-lg mr-4">
          <svg class="w-5 h-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        </div>
        <div><h4 class="font-semibold text-gray-900">Familiares</h4><p class="text-sm text-gray-500">Gestionar asociados</p></div>
      </router-link>
      <router-link to="/reviews" class="flex items-center p-4 bg-white rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
        <div class="p-2 bg-amber-100 rounded-lg mr-4">
          <svg class="w-5 h-5 text-amber-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.538 1.118l-2.8-2.034a1 1 0 00-1.176 0l-2.8 2.034c-.783.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.363-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81H7.03a1 1 0 00.95-.69l1.07-3.292z" />
          </svg>
        </div>
        <div><h4 class="font-semibold text-gray-900">Evaluaciones</h4><p class="text-sm text-gray-500">Completar pendientes</p></div>
      </router-link>
    </div>

    <!-- Toast Notification -->
    <Transition enter-active-class="transform ease-out duration-300 transition" enter-from-class="translate-y-2 opacity-0" enter-to-class="translate-y-0 opacity-100" leave-active-class="transition ease-in duration-200" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="showToast" 
           class="fixed bottom-4 right-4 z-50 bg-green-500 text-white px-6 py-4 rounded-lg shadow-lg flex items-center space-x-3">
        <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        <p class="font-semibold">Perfil actualizado correctamente</p>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userService } from '../services/userService'

const profile = ref(null)
const isEditing = ref(false)
const isSaving = ref(false)
const showToast = ref(false)
const editForm = ref({
  fullName: '',
  phone: ''
})

onMounted(async () => {
  await loadProfile()
})

const loadProfile = async () => {
  try {
    profile.value = await userService.getProfile()
    editForm.value = {
      fullName: profile.value.fullName,
      phone: profile.value.phone || ''
    }
  } catch (error) {
    console.error('Error loading profile:', error)
  }
}

const updateProfile = async () => {
  isSaving.value = true
  try {
    profile.value = await userService.updateProfile({
      fullName: editForm.value.fullName,
      phone: editForm.value.phone
    })
    isEditing.value = false
    showToast.value = true
    setTimeout(() => {
      showToast.value = false
    }, 3000)
  } catch (error) {
    console.error('Error updating profile:', error)
    alert('Error al actualizar el perfil')
  } finally {
    isSaving.value = false
  }
}
</script>
