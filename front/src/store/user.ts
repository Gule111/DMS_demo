import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 用户状态管理 Store
 * token 持久化到 localStorage，刷新页面后自动恢复
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('dms_token') || '')
  const refreshToken = ref<string>(localStorage.getItem('dms_refresh_token') || '')
  const userId = ref<number>(Number(localStorage.getItem('dms_userId')) || 0)
  const username = ref<string>(localStorage.getItem('dms_username') || '')
  const role = ref<number>(Number(localStorage.getItem('dms_role')) || 0)
  const phone = ref<string>(localStorage.getItem('dms_phone') || '')

  /** 登录/注册成功后保存用户信息 */
  function setUser(data: any) {
    const r = Number(data.role)
    token.value = data.token
    refreshToken.value = data.refreshToken
    userId.value = Number(data.userId)
    username.value = data.username
    role.value = r
    phone.value = data.phone

    localStorage.setItem('dms_token', data.token)
    localStorage.setItem('dms_refresh_token', data.refreshToken || '')
    localStorage.setItem('dms_userId', String(data.userId))
    localStorage.setItem('dms_username', data.username)
    localStorage.setItem('dms_role', String(r))
    localStorage.setItem('dms_phone', data.phone || '')
  }

  /** 退出登录时清除所有用户信息 */
  function clearUser() {
    token.value = ''
    refreshToken.value = ''
    userId.value = 0
    username.value = ''
    role.value = 0
    phone.value = ''

    localStorage.removeItem('dms_token')
    localStorage.removeItem('dms_refresh_token')
    localStorage.removeItem('dms_userId')
    localStorage.removeItem('dms_username')
    localStorage.removeItem('dms_role')
    localStorage.removeItem('dms_phone')
  }

  /** 获取角色中文名 */
  function getRoleName() {
    const r = Number(role.value)
    if (r === 1) return '管理员'
    if (r === 2) return '教练员'
    if (r === 3) return '学员'
    return '普通用户'
  }

  return { token, refreshToken, userId, username, role, phone, setUser, clearUser, getRoleName }
})
