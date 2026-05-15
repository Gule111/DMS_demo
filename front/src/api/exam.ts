import request from '@/utils/request'

export function bookExam(data: any) {
  return request.post('/exam/book', data)
}

export function getMyExams() {
  return request.get('/exam/my')
}

export function getAdminExamList(status?: number) {
  return request.get('/exam/admin/list', { params: { status } })
}

export function auditExam(data: { id: number; status: number }) {
  return request.post('/exam/admin/audit', data)
}

export function recordScore(data: { id: number; score: number }) {
  return request.post('/exam/admin/score', data)
}
