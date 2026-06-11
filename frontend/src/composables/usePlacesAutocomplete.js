import { ref } from 'vue'

const mapsReady = ref(false)

window.initGoogleMaps = () => {
  mapsReady.value = true
}

export function usePlacesAutocomplete() {
  function waitForMaps() {
    return new Promise((resolve) => {
      if (mapsReady.value && window.google?.maps?.places) return resolve()
      const check = () => {
        if (mapsReady.value && window.google?.maps?.places) resolve()
        else setTimeout(check, 200)
      }
      check()
    })
  }

  async function attachAutocomplete(inputEl, onPlaceSelected) {
    if (!inputEl) return
    await waitForMaps()
    const autocomplete = new google.maps.places.Autocomplete(inputEl, {
      componentRestrictions: { country: 'cl' },
      fields: ['address_components', 'formatted_address', 'geometry'],
      types: ['address']
    })
    autocomplete.addListener('place_changed', () => {
      const place = autocomplete.getPlace()
      if (place.formatted_address && onPlaceSelected) {
        onPlaceSelected(place)
      }
    })
  }

  return { attachAutocomplete }
}
