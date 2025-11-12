import service from '@/utils/request'

// 管理员登录
export function adminLogin(data) {
  return service({
    url: '/api/admins/login',
    method: 'post',
    data
  })
}

// 管理员退出登录
export function adminLogout(id) {
  return service({
    url: `/api/admins/logout/${id}`,
    method: 'post',
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
    }
  })
}

// 获取管理员信息
export function getAdminInfo(username) {
  return service({
    url: '/api/admins/info',
    method: 'get',
    params: { username },
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
    },
    transformResponse: [function (data) {
      try {
        if (!data || data.trim() === '') {
          throw new Error('空响应数据')
        }
        
        const parsed = JSON.parse(data)
        console.log('获取管理员信息原始数据:', parsed) // 调试信息

        // 验证响应结构
        if (!parsed || !parsed.data || !parsed.data.adminId) {
          console.error('无效的管理员信息结构:', parsed)
          throw new Error('无效的管理员信息结构')
        }

        // 确保包含role字段
        if (!parsed.data.role) {
          parsed.data.role = 'admin' // 默认角色
        }

        console.log('处理后的管理员信息:', parsed.data)
        return parsed
      } catch (err) {
        console.error('解析管理员信息失败:', err)
        // 返回标准错误结构
        return {
          code: 500,
          message: '解析管理员信息失败',
          data: null
        }
      }
    }]
  }).catch(error => {
    console.error('获取管理员信息请求失败:', error)
    throw new Error(error.response?.data?.message || '获取管理员信息失败')
  })
}

// 获取所有管理员
export function getAdmins() {
  return service({
    url: '/api/admins',
    method: 'get',
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
    }
  })
}

// 添加管理员
export function addAdmin(data) {
  return service({
    url: '/api/admins',
    method: 'post',
    data,
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
    }
  })
}

// 更新管理员
export function updateAdmin(data) {
  return service({
    url: '/api/admins',
    method: 'put',
    data,
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
    }
  })
}

// 删除管理员
export function deleteAdmin(id) {
  return service({
    url: `/api/admins/${id}`,
    method: 'delete',
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
    }
  })
}

export default {
  adminLogin,
  adminLogout,
  getAdminInfo,
  getAdmins,
  addAdmin,
  updateAdmin,
  deleteAdmin
}
