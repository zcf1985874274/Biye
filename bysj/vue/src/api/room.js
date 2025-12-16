import request from '@/utils/request'

export function getRooms(params) {
  return request({
    url: '/api/rooms',
    method: 'get',
    params
  }).then(response => {
    // 保持完整响应结构
    return {
      code: response.data?.code || 200,
      message: response.data?.message || '操作成功',
      data: response.data?.data || response.data // 兼容直接返回数组的情况
    }
  }).catch(error => {
    // 统一错误格式
    return Promise.reject({
      code: error.response?.status || 500,
      message: error.response?.data?.message || error.message,
      data: error.response?.data
    })
  })
}

export function getAvailableRooms(params) {
  return request({
    url: '/api/rooms/available',
    method: 'get',
    params
  }).then(response => {
    // 保持完整响应结构
    return {
      code: response.data?.code || 200,
      message: response.data?.message || '操作成功',
      data: response.data?.data || response.data // 兼容直接返回数组的情况
    }
  }).catch(error => {
    // 统一错误格式
    return Promise.reject({
      code: error.response?.status || 500,
      message: error.response?.data?.message || error.message,
      data: error.response?.data
    })
  })
}

export function getAvailableRoomsByStoreId(storeId) {
  return request({
    url: '/api/rooms/available',
    method: 'get',
    params: { storeId }
  }).then(response => {
    // 保持完整响应结构
    return {
      code: response.data?.code || 200,
      message: response.data?.message || '操作成功',
      data: response.data?.data || response.data // 兼容直接返回数组的情况
    }
  }).catch(error => {
    // 统一错误格式
    return Promise.reject({
      code: error.response?.status || 500,
      message: error.response?.data?.message || error.message,
      data: error.response?.data
    })
  })
}

export function addRoom(data) {
  return request({
    url: '/api/rooms',
    method: 'post',
    data
  })
}

export function updateRoom(data) {
  return request({
    url: `/api/rooms/${data.roomId}`,
    method: 'put',
    data
  })
}

export function deleteRoom(roomId) {
  return request({
    url: `/api/rooms/${roomId}`,
    method: 'delete'
  })
}

export function getRoomsByStoreId(storeId, params = {}) {
  if (!storeId) {
    return Promise.reject({ message: 'storeId is required' });
  }
  return request({
    url: `/api/rooms/store/${storeId}`,
    method: 'get',
    params
  }).then(response => {
    return {
      code: response.data?.code || 200,
      message: response.data?.message || '操作成功',
      data: response.data?.data || response.data
    }
  }).catch(error => {
    return Promise.reject({
      code: error.response?.status || 500,
      message: error.response?.data?.message || error.message,
      data: error.response?.data
    })
  })
}
