import { ref } from 'vue'

const toasts = ref([])
let nextId = 1

export function useToast() {
  function addToast(message, type = 'info', duration = 4000) {
    const id = nextId++
    toasts.value.push({ id, message, type, duration })
    if (duration > 0) {
      setTimeout(() => removeToast(id), duration)
    }
    return id
  }

  function removeToast(id) {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  function success(msg) { return addToast(msg, 'success') }
  function error(msg) { return addToast(msg, 'error') }
  function info(msg) { return addToast(msg, 'info') }
  function warning(msg) { return addToast(msg, 'warning') }

  return { toasts, addToast, removeToast, success, error, info, warning }
}
