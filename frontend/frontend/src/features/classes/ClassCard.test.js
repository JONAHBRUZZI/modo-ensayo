import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import ClassCard from '../features/classes/ClassCard.vue'

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [{ path: '/classes', name: 'Classes' }],
  })
}

describe('ClassCard', () => {
  it('renders class info correctly', () => {
    const router = createTestRouter()
    const wrapper = mount(ClassCard, {
      props: {
        classItem: {
          id: '1',
          title: 'Clase de Ballet',
          discipline: 'Ballet',
          venue: 'Sede Centro',
          room: 'Sala 1',
          startTime: '10:00 AM',
          capacity: 15,
          price: 5000,
        },
      },
      global: { plugins: [router] },
    })

    expect(wrapper.text()).toContain('Clase de Ballet')
    expect(wrapper.text()).toContain('Ballet')
    expect(wrapper.text()).toContain('$5000')
    expect(wrapper.text()).toContain('15 cupos')
  })
})
