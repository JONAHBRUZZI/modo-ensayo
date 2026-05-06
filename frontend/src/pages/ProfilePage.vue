<template>
  <div class="max-w-2xl mx-auto">
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-white mb-2">Mi Perfil</h1>
      <p class="text-gray-400">Gestiona tu información personal</p>
    </div>

    <!-- Profile Card -->
    <div class="bg-[#161824] rounded-2xl border border-white/10 overflow-hidden">
      <div class="h-32 bg-gradient-to-r from-indigo-600 to-purple-600"></div>

      <div class="px-6 pb-6">
        <div class="relative flex justify-between items-end -mt-12 mb-6">
          <div class="w-24 h-24 bg-[#161824] rounded-2xl border-4 border-[#161824] shadow-xl flex items-center justify-center">
            <span class="text-4xl font-bold text-indigo-400">
              {{ user?.fullName?.charAt(0)?.toUpperCase() || 'U' }}
            </span>
          </div>
          <button @click="isEditing = !isEditing"
                  class="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-colors">
            {{ isEditing ? 'Cancelar' : 'Editar Perfil' }}
          </button>
        </div>

        <!-- View Mode -->
        <div v-if="!isEditing" class="space-y-5">
          <div>
            <label class="text-xs font-medium text-gray-500 uppercase tracking-wider">Nombre Completo</label>
            <p class="text-base font-semibold text-white mt-1">{{ profile?.fullName }}</p>
          </div>
          <div>
            <label class="text-xs font-medium text-gray-500 uppercase tracking-wider">Email</label>
            <p class="text-base text-gray-300 mt-1">{{ profile?.email }}</p>
          </div>
          <div>
            <label class="text-xs font-medium text-gray-500 uppercase tracking-wider">Teléfono</label>
            <p class="text-base text-gray-300 mt-1">{{ profile?.phone || 'No registrado' }}</p>
          </div>
          <div>
            <label class="text-xs font-medium text-gray-500 uppercase tracking-wider">Roles</label>
            <div class="flex flex-wrap gap-2 mt-2">
              <span v-for="role in profile?.roles" :key="role"
                    class="px-3 py-1 bg-indigo-500/20 text-indigo-400 text-xs font-medium rounded-full border border-indigo-500/30">
                {{ role }}
              </span>
            </div>
          </div>
          <div>
            <label class="text-xs font-medium text-gray-500 uppercase tracking-wider">Score como alumno</label>
            <p class="text-base text-gray-300 mt-1">
              {{ profile?.averageStudentScore ? Number(profile.averageStudentScore).toFixed(2) : 'Sin evaluaciones' }}
              <span class="text-sm text-gray-500 ml-1">({{ profile?.totalStudentReviews || 0 }} evaluaciones)</span>
            </p>
          </div>
        </div>

        <!-- Edit Mode -->
        <form v-else @submit.prevent="updateProfile" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Nombre Completo</label>
            <input v-model="editForm.fullName" type="text" required
                   class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Email</label>
            <input :value="profile?.email" type="email" disabled
                   class="w-full px-3 py-2.5 bg-white/5 border border-white/10 rounded-lg text-gray-500 cursor-not-allowed">
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">Teléfono</label>
            <input v-model="editForm.phone" type="tel"
                   class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                   placeholder="+56912345678">
          </div>
          <div class="flex gap-3 pt-2">
            <button type="submit" :disabled="isSaving"
                    class="flex-1 py-2.5 px-4 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-medium disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
              {{ isSaving ? 'Guardando...' : 'Guardar Cambios' }}
            </button>
            <button type="button" @click="isEditing = false"
                    class="px-4 py-2.5 border border-white/15 text-gray-300 rounded-lg font-medium hover:bg-white/5 transition-colors">
              Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Quick Links -->
    <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-4">
      <router-link v-for="link in quickLinks" :key="link.to" :to="link.to"
                   class="flex items-center p-4 bg-[#161824] rounded-xl border border-white/10 hover:border-white/20 transition-all group">
        <div class="p-2 rounded-lg mr-4 flex-shrink-0" :class="link.iconBg">
          <component :is="'svg'" class="w-5 h-5" :class="link.iconColor" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="link.iconPath" />
          </component>
        </div>
        <div>
          <h4 class="font-semibold text-white text-sm group-hover:text-indigo-400 transition-colors">{{ link.title }}</h4>
          <p class="text-xs text-gray-500 mt-0.5">{{ link.subtitle }}</p>
        </div>
      </router-link>
    </div>

    <!-- Toast -->
    <Transition enter-active-class="transform ease-out duration-300 transition"
                enter-from-class="translate-y-2 opacity-0" enter-to-class="translate-y-0 opacity-100"
                leave-active-class="transition ease-in duration-200" leave-from-class="opacity-100" leave-to-class="opacity-0">
      <div v-if="showToast"
           class="fixed bottom-4 right-4 z-50 bg-emerald-500 text-white px-6 py-4 rounded-xl shadow-lg flex items-center gap-3">
        <svg class="w-5 h-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
        <p class="font-semibold text-sm">Perfil actualizado correctamente</p>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../hooks/useAuth'
import { userService } from '../services/userService'

const { user } = useAuth()
const profile = ref(null)
const isEditing = ref(false)
const isSaving = ref(false)
const showToast = ref(false)
const editForm = ref({ fullName: '', phone: '' })

const quickLinks = [
  { to: '/classes', title: 'Mis Clases', subtitle: 'Ver inscripciones activas', iconBg: 'bg-indigo-500/20', iconColor: 'text-indigo-400', iconPath: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253' },
  { to: '/profile/refund-method', title: 'Método de Devolución', subtitle: 'Configurar reembolsos', iconBg: 'bg-amber-500/20', iconColor: 'text-amber-400', iconPath: 'M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a1 1 0 11-2 0 1 1 0 012 0z' },
  { to: '/profile/identity', title: 'Verificar Identidad', subtitle: 'Subir documento', iconBg: 'bg-blue-500/20', iconColor: 'text-blue-400', iconPath: 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z' },
  { to: '/associates', title: 'Familiares', subtitle: 'Gestionar asociados', iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400', iconPath: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z' },
  { to: '/reviews', title: 'Evaluaciones', subtitle: 'Completar pendientes', iconBg: 'bg-amber-500/20', iconColor: 'text-amber-400', iconPath: 'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.538 1.118l-2.8-2.034a1 1 0 00-1.176 0l-2.8 2.034c-.783.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.363-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81H7.03a1 1 0 00.95-.69l1.07-3.292z' },
]

onMounted(async () => { await loadProfile() })

const loadProfile = async () => {
  try {
    profile.value = await userService.getProfile()
    editForm.value = { fullName: profile.value.fullName, phone: profile.value.phone || '' }
  } catch (error) {
    console.error('Error loading profile:', error)
  }
}

const updateProfile = async () => {
  isSaving.value = true
  try {
    profile.value = await userService.updateProfile({ fullName: editForm.value.fullName, phone: editForm.value.phone })
    isEditing.value = false
    showToast.value = true
    setTimeout(() => { showToast.value = false }, 3000)
  } catch (error) {
    console.error('Error updating profile:', error)
    alert('Error al actualizar el perfil')
  } finally {
    isSaving.value = false
  }
}
</script>
