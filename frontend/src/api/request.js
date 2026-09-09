import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

/**
 * axios 统一封装：
 *  - 自动携带 Authorization: Bearer <token>
 *  - 业务码 code !== 200 时统一弹错；401 自动登出回登录页
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const store = useUserStore()
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

function handleUnauthorized() {
  const store = useUserStore()
  store.logout()
  if (router.currentRoute.value.path !== '/login') {
    ElMessage.warning('登录已过期，请重新登录')
    router.push('/login')
  }
}

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error(res.msg || '请求失败')
    }
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const res = error.response?.data
    if (res && res.code === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error(res?.msg || error.message || '网络异常，请稍后再试')
    }
    return Promise.reject(error)
  }
)

export default request
