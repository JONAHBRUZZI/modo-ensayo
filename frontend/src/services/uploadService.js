import api from './api'

export default {
  async uploadFile(file, type = 'documents') {
    const formData = new FormData()
    formData.append('file', file)
    const res = await api.post(`/upload?type=${type}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return res.data
  }
}
