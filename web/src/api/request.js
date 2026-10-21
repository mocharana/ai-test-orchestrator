import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// axios 实例：basePath 走 Vite proxy 转发到后端 8081
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：自动携带 token（后端拦截器认 Authorization: Bearer）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

// 响应拦截：统一拆信封；401 踢回登录页
request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body.code !== 200) {
      ElMessage.error(body.msg || '操作失败')
      return Promise.reject(new Error(body.msg))
    }
    return body.data
  },
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('token')
      ElMessage.error('登录已过期，请重新登录')
      // 硬跳转而非 router.push：热更新/首次导航的竞态下 push 可能被吞，
      // 导致接口 JSON 被当页面渲染——整页跳登录保证干净状态
      location.href = '/login'
    } else {
      ElMessage.error('网络错误：' + (err.message || '未知'))
    }
    return Promise.reject(err)
  }
)

export default request
