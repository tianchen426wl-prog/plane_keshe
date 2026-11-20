import axios from 'axios'
import store from '../store'
import { Message } from 'element-ui'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  try { const user=JSON.parse(localStorage.getItem('portalUser') || 'null'); if(user && user.token) config.headers.Authorization=`Bearer ${user.token}` } catch(e) { localStorage.removeItem('portalUser') }
  return config
})
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      Message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('portalUser')
      store.commit('SET_USER', null)
      window.location.hash = '#/login'
    }
    Message.error((error.response && error.response.data && error.response.data.message) || '网络错误')
    return Promise.reject(error)
  }
)

export default request