let readyPromise = null

function waitForGoogleMaps() {
  if (readyPromise) return readyPromise
  readyPromise = new Promise((resolve) => {
    if (window.google?.maps?.places) return resolve()
    const check = () => {
      if (window.google?.maps?.places) resolve()
      else setTimeout(check, 200)
    }
    check()
  })
  return readyPromise
}

export function usePlacesAutocomplete() {
  async function attachAutocomplete(inputEl, onPlaceSelected) {
    if (!inputEl) return
    await waitForGoogleMaps()
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
