<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-white">Crear Clase en mi Sede</h1>
      <p class="text-gray-400 text-sm mt-0.5">Programa una nueva clase en tus instalaciones</p>
    </div>

    <div class="bg-[#161824] rounded-xl border border-white/10 p-6 space-y-4">
      <div>
        <label class="block text-xs font-medium text-gray-400 mb-1.5">Nombre de la clase *</label>
        <input v-model="form.title" placeholder="Ej: Ballet intermedio"
          class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Disciplina *</label>
          <select v-model="form.discipline"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
            <option value="">Selecciona</option>
            <option>Danza</option><option>Teatro</option><option>Música</option>
            <option>Yoga</option><option>Artes visuales</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Sala *</label>
          <select v-model="form.roomId"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all">
            <option value="">Selecciona</option>
            <option value="1">Sala Principal</option>
            <option value="2">Sala Pequeña</option>
          </select>
        </div>
      </div>

      <div>
        <label class="block text-xs font-medium text-gray-400 mb-1.5">Profesor asignado</label>
        <input v-model="form.teacher" placeholder="Nombre del instructor (opcional)"
          class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Fecha y hora *</label>
          <input v-model="form.startTime" type="datetime-local"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white text-sm [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Duración (min)</label>
          <input v-model.number="form.duration" type="number" min="30" step="15" placeholder="60"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Cupos *</label>
          <input v-model.number="form.capacity" type="number" min="1" placeholder="20"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-400 mb-1.5">Precio (CLP) *</label>
          <input v-model.number="form.price" type="number" min="0" placeholder="20000"
            class="w-full px-3 py-2.5 bg-[#0d0f1a] border border-white/15 rounded-lg text-white placeholder-gray-500 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all" />
        </div>
      </div>

      <button @click="crear" :disabled="!canSubmit || saving"
        class="w-full py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-xl text-sm font-semibold transition-colors">
        {{ saving ? 'Creando...' : 'Publicar Clase' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { classService } from '../../services/classService'

const router = useRouter()
const saving = ref(false)
const form = ref({ title: '', discipline: '', roomId: '', teacher: '', startTime: '', duration: 60, capacity: '', price: '' })

const canSubmit = computed(() => form.value.title && form.value.discipline && form.value.roomId && form.value.startTime && form.value.capacity && form.value.price)

const crear = async () => {
  if (!canSubmit.value) return
  saving.value = true
  try {
    await classService.createClass(form.value)
    router.push('/sede/mis-clases')
  } catch (e) {
    alert(e?.response?.data?.message || 'Error al crear la clase')
  } finally {
    saving.value = false
  }
}
</script>
