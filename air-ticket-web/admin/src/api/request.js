import axios from 'axios'
import { Message } from 'element-ui'
import store from '@/store'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = store.state.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) {
      store.commit('LOGOUT')
      Message.error('登录已过期，请重新登录')
      window.location.hash = '#/admin/login'
      return Promise.reject(new Error('未登录'))
    }
    if (res.code !== 200) {
      Message.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    if (error.response) {
      const status = error.response.status
      const res = error.response.data
      if (status === 401) {
        store.commit('LOGOUT')
        window.location.hash = '#/admin/login'
        Message.error('请重新登录')
      } else if (status === 500) {
        Message.error(res.message || '服务器内部错误')
      } else if (status === 404) {
        Message.error('请求接口不存在')
      } else {
        Message.error(res.message || `请求错误 (${status})`)
      }
    } else if (error.message.includes('timeout')) {
      Message.error('请求超时，请稍后重试')
    } else {
      Message.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request