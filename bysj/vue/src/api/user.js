import axios from 'axios'

// 创建axios实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || 'http://localhost:8080',
  timeout: 5000
})

// 请求拦截器
service.interceptors.request.use(
    config => {
      // 在请求头中添加token
      const token = localStorage.getItem('token')
      if (token) {
        config.headers['Authorization'] = 'Bearer ' + token
      }
      return config
    },
    error => {
      console.error(error)
      return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    response => {
      const res = response.data
      // 如果返回的状态码不是200，说明接口请求有误
      if (res.code !== 200) {
        // 登录超时或token无效
        if (res.code === 401) {
          // 清除token并跳转登录页
          localStorage.removeItem('token')
          localStorage.removeItem('username')
          location.reload()
        }
        return Promise.reject(new Error(res.message || '未知错误'))
      } else {
        return res
      }
    },
    error => {
      console.error('请求错误：' + error)
      return Promise.reject(error)
    }
)

// 用户注册
export function register(data) {
  return service({
    url: '/api/user/register',
    method: 'post',
    data
  })
}

// 用户登录
export function login(data) {
  return service({
    url: '/api/user/login',
    method: 'post',
    data
  })
}

// 获取用户信息
export function getUserInfo(username) {
  return service({
    url: '/api/user/info',
    method: 'get',
    params: { username },
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('token')
    }
  })
}

// 用户退出登录
export function logout(username) {
  try {
    const token = localStorage.getItem('token')
    console.log('当前token:', token, 'username:', username) // 调试日志

    if (!username) {
      throw new Error('退出失败：缺少用户名参数')
    }

    const config = {
      url: '/api/user/logout',
      method: 'post',
      params: { username }
    }

    // 严格检查token有效性
    if (token && typeof token === 'string' && token.includes('.')) {
      config.headers = {
        'Authorization': 'Bearer ' + token
      }
      console.log('发送退出请求，携带token和username:', username) // 调试日志
    } else {
      console.warn('无效或缺失的token，将发送无token退出请求') // 调试日志
    }

    return service(config)
  } catch (error) {
    console.error('退出登录时发生错误:', error)
    return Promise.reject(error)
  }
}

// 获取所有用户信息 (需要管理员权限)
export function getAllUsers() {
  return service({
    url: '/api/user/all',
    method: 'get',
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
    }
  })
}

// 删除用户 (需要管理员权限)
export function deleteUser(userId) {
  return service({
    url: `/api/user/${userId}`,
    method: 'delete',
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
    }
  })
}

// 更新用户信息 (需要管理员权限)
export function updateUser(data) {
  return service({
    url: '/api/user',
    method: 'put',
    data,
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
    }
  })
}

// 更新用户自己的信息 (普通用户权限)
export function updateSelfInfo(data) {
  return service({
    url: '/api/user/self',
    method: 'put',
    data,
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('token')
    }
  })
}

export default service
