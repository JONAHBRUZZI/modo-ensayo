import api from './api'

export default {
  getSchedule(venueId) {
    return api.get(`/venues/${venueId}/schedule`).then(r => r.data)
  },
  saveSchedule(venueId, schedules) {
    return api.put(`/venues/${venueId}/schedule`, schedules).then(r => r.data)
  },
  getBlockConfig(venueId) {
    return api.get(`/venues/${venueId}/block-config`).then(r => r.data)
  },
  saveBlockConfig(venueId, config) {
    return api.put(`/venues/${venueId}/block-config`, config).then(r => r.data)
  },
  generateBlocks(venueId) {
    return api.post(`/venues/${venueId}/generate-blocks`).then(r => r.data)
  },
  getRoomSchedule(roomId, from, to) {
    return api.get(`/venues/rooms/${roomId}/schedule`, { params: { from, to } }).then(r => r.data)
  },
  markMaintenance(roomId, blockId, reason) {
    return api.post(`/venues/rooms/${roomId}/blocks/${blockId}/maintenance`, { reason }).then(r => r.data)
  },
  releaseMaintenance(blockId) {
    return api.delete(`/venues/rooms/blocks/${blockId}/maintenance`).then(r => r.data)
  }
}
