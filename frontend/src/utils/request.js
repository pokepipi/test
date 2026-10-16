import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({ baseURL: '', timeout: 15000 })

// 请求拦截: 自动携带 token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

// 响应拦截: 统一处理业务码
request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== 200) {
      if (data.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data.data
  },
  err => {
    ElMessage.error(err.response?.data?.message || err.message || '网络异常')
    return Promise.reject(err)
  }
)

export default request
