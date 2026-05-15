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

// 是否正在刷新的标识
let isRefreshing = false
// 存储因为 token 过期而挂起的请求
let requests: any[] = []

// 响应拦截器：处理错误和 Token 过期
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端返回 code !== 200 时视为业务错误
    if (res.code !== 200) {
      // 特殊处理 401 情况（有些后端在业务 code 里返回 401 而不是 HTTP 状态码）
      if (res.code === 401) {
        return handle401(response.config)
      }
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  async (error) => {
    const config = error.config
    if (error.response?.status === 401) {
      return handle401(config)
    } else {
      message.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

/**
 * 核心逻辑：处理 401 过期，尝试无感刷新
 */
async function handle401(config: any) {
  const userStore = useUserStore()

  // 如果没有刷新令牌，直接跳转登录
  if (!userStore.refreshToken) {
    userStore.clearUser()
    router.push('/login')
    return Promise.reject(new Error('令牌失效'))
  }

  if (!isRefreshing) {
    isRefreshing = true
    try {
      // 调用刷新接口（注意：这里必须用 axios 原生或者另一个实例，防止死循环）
      const res = await axios.post('/api/auth/refresh', {
        refreshToken: userStore.refreshToken
      })

      if (res.data.code === 200) {
        const newToken = res.data.data
        // 更新 Pinia 和 LocalStorage
        userStore.token = newToken
        localStorage.setItem('dms_token', newToken)

        // 刷新成功，执行之前挂起的请求
        requests.forEach((cb) => cb(newToken))
        requests = []
        
        // 执行当前触发刷新的这个请求
        config.headers.Authorization = `Bearer ${newToken}`
        return request(config)
      }
    } catch (refreshError) {
      // 刷新也失败了（比如 refreshToken 也过期了）
      userStore.clearUser()
      router.push('/login')
      message.error('会话已过期，请重新登录')
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  } else {
    // 如果正在刷新中，将当前请求包装成 Promise 挂起
    return new Promise((resolve) => {
      requests.push((token: string) => {
        config.headers.Authorization = `Bearer ${token}`
        resolve(request(config))
      })
    })
  }
}

export default request
