<template>
  <div class="space-y-8">
    <h1 class="text-3xl font-bold text-gray-900">Gestion de Roles</h1>
    <div class="bg-white rounded-xl shadow-md p-6">
      <h2 class="text-lg font-semibold mb-4">Usuarios y Roles</h2>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="text-left border-b">
              <th class="pb-2">Usuario</th><th class="pb-2">Email</th><th class="pb-2">Roles</th><th class="pb-2">Accion</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id" class="border-b">
              <td class="py-3">{{ user.fullName }}</td>
              <td>{{ user.email }}</td>
              <td>
                <span v-for="r in getUserRoles(user.id)" :key="r" class="px-2 py-1 bg-indigo-100 text-indigo-700 rounded-full text-xs mr-1">{{ r }}</span>
              </td>
              <td>
                <select @change="assignRole(user.id, $event)" class="text-sm border rounded px-2 py-1">
                  <option value="">Asignar rol</option>
                  <option v-for="r in roles" :key="r.id" :value="r.name">{{ r.name }}</option>
                </select>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { rolesService } from '../services/rolesService'

const users = ref([])
const roles = ref([])
const userRoles = ref({})

onMounted(async () => {
  try {
    users.value = await rolesService.listUsers()
    roles.value = await rolesService.list()
    // Cargar roles existentes de cada usuario
    for (const u of users.value) {
      userRoles.value[u.id] = users.value.find(user => user.id === u.id)?.roles || []
    }
  } catch (e) { console.error(e) }
})

const getUserRoles = (userId) => userRoles.value[userId] || []

const assignRole = async (userId, event) => {
  const roleName = event.target.value
  if (!roleName) return
  try {
    await rolesService.assign(userId, roleName)
    userRoles.value[userId] = [...(userRoles.value[userId] || []), roleName]
    event.target.value = ''
  } catch (e) { alert('Error al asignar') }
}
</script>