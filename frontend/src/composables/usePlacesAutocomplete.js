let debounceTimer = null

export function usePlacesAutocomplete() {
  function attachAutocomplete(inputEl, onPlaceSelected) {
    if (!inputEl) return

    const wrapper = document.createElement('div')
    wrapper.className = 'relative'
    inputEl.parentNode.insertBefore(wrapper, inputEl)
    wrapper.appendChild(inputEl)

    const dropdown = document.createElement('div')
    dropdown.className = 'absolute z-50 w-full mt-1 bg-[var(--bg-elevated)] border border-[var(--border-subtle)] rounded-xl shadow-xl max-h-48 overflow-y-auto hidden'
    wrapper.appendChild(dropdown)

    inputEl.setAttribute('autocomplete', 'off')

    inputEl.addEventListener('input', () => {
      clearTimeout(debounceTimer)
      const query = inputEl.value.trim()
      if (query.length < 3) {
        dropdown.classList.add('hidden')
        return
      }
      debounceTimer = setTimeout(() => searchNominatim(query, dropdown, inputEl, onPlaceSelected), 500)
    })

    inputEl.addEventListener('blur', () => {
      setTimeout(() => dropdown.classList.add('hidden'), 200)
    })
  }

  return { attachAutocomplete }
}

async function searchNominatim(query, dropdown, inputEl, onPlaceSelected) {
  try {
    const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&addressdetails=1&limit=5&countrycodes=cl`
    const res = await fetch(url)
    const data = await res.json()

    if (!data || data.length === 0) {
      dropdown.classList.add('hidden')
      return
    }

    dropdown.innerHTML = data.map(item => {
      const city = item.address?.city || item.address?.town || item.address?.county || ''
      return `<div class="px-3 py-2 cursor-pointer hover:bg-[var(--text-primary)]/5 text-sm text-[var(--text-secondary)] border-b border-[var(--text-primary)]/5 last:border-0"
        data-address="${item.display_name.replace(/"/g, '&quot;')}"
        data-city="${city.replace(/"/g, '&quot;')}">
        ${item.display_name}
      </div>`
    }).join('')

    dropdown.querySelectorAll('div').forEach(div => {
      div.addEventListener('mousedown', (e) => {
        e.preventDefault()
        const address = div.dataset.address
        const city = div.dataset.city
        inputEl.value = address
        dropdown.classList.add('hidden')
        if (onPlaceSelected) {
          onPlaceSelected({ formatted_address: address, city })
        }
      })
    })

    dropdown.classList.remove('hidden')
  } catch {
    dropdown.classList.add('hidden')
  }
}
