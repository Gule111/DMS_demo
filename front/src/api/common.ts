import request from '@/utils/request'

export function getDictByType(type: string) {
  return request.get(`/dict/type/${type}`)
}

export function getAllDicts() {
  return request.get('/dict/all')
}

export function saveDict(data: any) {
  return request.post('/dict/save', data)
}

export function deleteDict(id: number) {
  return request.delete(`/dict/${id}`)
}
