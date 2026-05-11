import request from '@/utils/request'

/** Send SMS verification code */
export function sendCode(phone: string) {
  return request.post('/auth/sendCode', { phone })
}

/** Login with phone + password + code */
export function login(phone: string, password: string, code: string) {
  return request.post('/auth/login', { phone, password, code })
}

/** Register with username + phone + password + code */
export function register(username: string, phone: string, password: string, code: string) {
  return request.post('/auth/register', { username, phone, password, code })
}

/** Logout */
export function logout() {
  return request.post('/auth/logout')
}

/** Get current user info */
export function getUserInfo() {
  return request.get('/auth/info')
}
