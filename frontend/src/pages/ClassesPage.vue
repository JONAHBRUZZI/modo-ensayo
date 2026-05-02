<template>
  <div>
    <h1 class="text-3xl font-bold mb-8">Clases Disponibles</h1>
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="cls in classes" :key="cls.id" class="bg-white rounded-lg shadow-md p-6">
        <h3 class="text-xl font-semibold mb-2">{{ cls.title }}</h3>
        <p class="text-gray-600 mb-2">Disciplina: {{ cls.discipline || 'General' }}</p>
        <p class="text-gray-600 mb-2">Sede: {{ cls.venueName }}</p>
        <p class="text-gray-600 mb-4">Precio: ${{ cls.price?.toLocaleString() }}</p>
        <button @click="addToCart(cls)" class="w-full bg-indigo-600 text-white py-2 rounded-md hover:bg-indigo-700">
          Agregar al Carrito
        </button>
      </div>
    </div>
    <p v-if="classes.length === 0" class="text-gray-500 text-center py-8">No hay clases disponibles</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { classService } from '../services/classService'
import { cartService } from '../services/cartService'

const classes = ref([])

onMounted(async () => {
  try {
    classes.value = await classService.listPublished()
  } catch (error) {
    console.error('Error loading classes:', error)
  }
})

const addToCart = async (cls) => {
  try {
    await cartService.addToCart({
      classId: cls.id,
      beneficiaryType: 'USER',
    })
    alert('Clase agregada al carrito')
  } catch (error) {
    alert('Error al agregar al carrito')
  }
}
</script>
