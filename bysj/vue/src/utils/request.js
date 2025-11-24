import axios from 'axios'
import { Message } from 'element-ui'
import store from '@/store'
import router from "@/router";

// 创建axios实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || 'http://localhost:8080', // 默认本地开发环境
  timeout: 10000 // 增加超时时间
})

// 请求拦截器 - 添加日志
service.interceptors.request.use(
  config => {
    console.log('[Request]', {
      url: config.url,
      method: config.method,
      params: config.params,
      data: config.data,
      headers: config.headers
    })
    
    // 检查是否是忘记密码相关的接口，这些接口不需要token
    const isForgotPasswordApi = config.url && (
      config.url.includes('/api/user/check-username') ||
      config.url.includes('/api/user/verify-phone') ||
      config.url.includes('/api/user/reset-password')
    )
    
    // 只有非忘记密码接口才添加token
    if (!isForgotPasswordApi) {
      // 添加token - 优先检查管理员token
      const adminToken = store.getters.adminToken
      const userToken = store.getters.token
      
      if (adminToken) {
        config.headers['Authorization'] = 'Bearer ' + adminToken
        console.log('使用管理员token:', adminToken.substring(0, 20) + '...')
      } else if (userToken) {
        config.headers['Authorization'] = 'Bearer ' + userToken
        console.log('使用用户token:', userToken.substring(0, 20) + '...')
      } else {
        console.log('未找到token，请求可能未授权')
      }
    } else {
      console.log('忘记密码接口，不添加token')
    }
    
    // 确保PATCH请求有正确的Content-Type
    if (config.method === 'patch' && config.data) {
      config.headers['Content-Type'] = 'application/json'
    }
    
    return config
  },
  error => {
    console.error('[Request Error]', error)
    return Promise.reject(error)
  }
)

// 响应拦截器 - 增强错误处理
service.interceptors.response.use(
  response => {
    console.log('[Response]', response.data)
    const res = response.data
    
    // 处理业务错误 (200和201都视为成功)
    if (res.code && res.code !== 200 && res.code !== 201) {
      const errorMsg = res.message || '请求失败'
      Message({
        message: errorMsg,
        type: 'error',
        duration: 5000
      })
      return Promise.reject(new Error(errorMsg))
    }

    // 对于201状态码，确保返回成功响应
    if (res.code === 201) {
      return {
        code: 200, // 转换为前端期望的成功状态码
        message: res.message || '操作成功',
        data: res.data || res
      }
    }
    
    // 保持完整响应结构
    return {
      code: res.code || 200,
      message: res.message || '操作成功',
      data: res.data || res // 兼容直接返回数组的情况
    }
  },
  error => {
    console.error('[Response Error]', {
      message: error.message,
      config: error.config,
      response: error.response
    })
    
    let errorMsg = '请求失败'
    if (error.response) {
      // 服务器返回了错误响应
      console.error('完整错误响应:', error.response.data)
      errorMsg = error.response.data?.message || 
                error.response.data?.error ||
                `服务器错误: ${error.response.status}`
      
      console.error('API请求错误:', error)
      
      // 处理401/403未授权错误（JWT过期或无效）
      if (error.response && (error.response.status === 401 || error.response.status === 403)) {
        console.log('检测到未授权错误，执行登出流程')
        
        // 检查当前路由判断是管理员还是普通用户
        const isAdminRoute = router.currentRoute.path.startsWith('/admin')
        
        if (isAdminRoute) {
          // 管理员登出
          store.dispatch('admin/adminLogout').then(() => {
            router.push('/adminlogin')
          })
        } else {
          // 普通用户登出
          store.dispatch('user/logout').then(() => {
            router.push('/login')
          })
        }
        
        errorMsg = error.response.status === 401 ? 
          '登录已过期，请重新登录' : '权限不足，请重新登录'
      } else {
        // 其他错误处理
        errorMsg = error.response?.data?.message || 
                  error.message || 
                  '请求失败，请稍后重试'
      }
    } else if (error.request) {
      // 请求已发出但没有收到响应
      errorMsg = '网络错误: 无法连接到服务器'
    } else {
      // 请求配置出错
      errorMsg = `请求错误: ${error.message}`
    }
    
    Message({
      message: errorMsg,
      type: 'error',
      duration: 5000
    })
    
    return Promise.reject(error)
  }
)

export default service
