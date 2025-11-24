import request from '@/utils/request'

// 用户登录
export function login(data) {
  return request({
    url: '/api/user/login',
    method: 'post',
    data
  })
}

// 用户注册
export function register(data) {
  return request({
    url: '/api/user/register',
    method: 'post',
    data
  })
}

// 获取用户信息
export function getUserInfo(username) {
  return request({
    url: `/api/user/info?username=${username}`,
    method: 'get'
  })
}

// 用户退出登录
export function logout(username) {
  return request({
    url: `/api/user/logout?username=${username}`,
    method: 'post'
  })
}

// 检查用户名是否存在并返回脱敏手机号
export function checkUsername(username) {
  return request({
    url: `/api/user/check-username?username=${username}`,
    method: 'get'
  })
}

// 验证手机号
export function verifyPhone(data) {
  return request({
    url: '/api/user/verify-phone',
    method: 'post',
    data
  })
}

// 重置密码
export function resetPassword(data) {
  return request({
    url: '/api/user/reset-password',
    method: 'post',
    data
  })
}