import axios from 'axios'
import { useUserStore } from '@/store/user'
import { message } from 'ant-design-vue'
import router from '@/router'

/**
 * Axios 实例
 * baseURL 设为 /api，Vite 开发服务器会将 /api 代理到 http://localhost:8080
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器：自动携带 Token
request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器：处理错误和 Token 过期
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端返回 code !== 200 时视为业务错误
    if (res.code !== 200) {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      message.error('登录已过期，请重新登录')
      const userStore = useUserStore()
      userStore.clearUser()
      router.push('/login')
    } else {
      message.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
