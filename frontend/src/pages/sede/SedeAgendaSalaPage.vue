<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Agenda de Sala</h1>
        <p class="text-gray-400 text-sm mt-0.5">Disponibilidad y ocupación semanal</p>
      </div>
      <select v-model="salaSeleccionada" class="px-3 py-2 bg-[#161824] border border-white/15 rounded-lg text-sm text-white [color-scheme:dark] focus:outline-none focus:ring-2 focus:ring-indigo-500">
        <option v-for="s in salas" :key="s.id" :value="s.id">{{ s.name }}</option>
      </select>
    </div>

    <!-- Calendario semanal -->
    <div class="bg-[#161824] rounded-xl border border-white/10 overflow-hidden">
      <div class="grid grid-cols-7 border-b border-white/5">
        <div v-for="dia in diasSemana" :key="dia.key"
          class="px-3 py-3 text-center border-r border-white/5 last:border-0">
          <p class="text-xs text-gray-500 uppercase">{{ dia.label }}</p>
          <p class="text-lg font-bold" :class="dia.esHoy ? 'text-indigo-400' : 'text-white'">{{ dia.num }}</p>
        </div>
      </div>
      <div class="grid grid-cols-7 min-h-[300px]">
        <div v-for="dia in diasSemana" :key="dia.key"
          class="border-r border-white/5 last:border-0 p-2 space-y-1.5">
          <div v-for="slot in dia.slots" :key="slot.id"
            :class="slot.ocupado ? 'bg-indigo-500/20 border-indigo-500/30 text-indigo-300' : 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'"
            class="rounded-lg border px-2 py-1.5">
            <p class="text-xs font-medium truncate">{{ slot.hora }}</p>
            <p v-if="slot.ocupado" class="text-xs opacity-70 truncate">{{ slot.clase }}</p>
            <p v-else class="text-xs opacity-70">Libre</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const salas = [
  { id: 1, name: 'Sala Principal' },
  { id: 2, name: 'Sala Pequeña' },
  { id: 3, name: 'Sala Multiuso' },
]
const salaSeleccionada = ref(1)

const diasSemana = computed(() => {
  const hoy = new Date()
  const lunes = new Date(hoy)
  lunes.setDate(hoy.getDate() - hoy.getDay() + 1)
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(lunes)
    d.setDate(lunes.getDate() + i)
    return {
      key: i,
      label: d.toLocaleDateString('es-CL', { weekday: 'short' }),
      num: d.getDate(),
      esHoy: d.toDateString() === hoy.toDateString(),
      slots: mockSlots(i),
    }
  })
})

const mockSlots = (dayIdx) => {
  const slots = [
    { id: 1, hora: '09:00', ocupado: dayIdx % 3 === 0, clase: 'Tango' },
    { id: 2, hora: '11:00', ocupado: dayIdx % 2 === 0, clase: 'Jazz' },
    { id: 3, hora: '15:00', ocupado: dayIdx === 1 || dayIdx === 4, clase: 'Yoga' },
    { id: 4, hora: '18:00', ocupado: dayIdx < 5, clase: 'Danza Clásica' },
  ]
  return slots
}
</script>
