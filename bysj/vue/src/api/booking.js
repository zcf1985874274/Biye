import request from '@/utils/request'

// 更新房间状态
export function updateRoomStatus(roomId, status) {
  return request({
    url: `/api/rooms/${roomId}/status`,
    method: 'patch',
    data: { status }
  })
}

// 创建预订
export function createBooking(data) {
  return request({
    url: '/api/bookings',
    method: 'post',
    data
  }).then(response => {
    // 预订创建成功后更新房间状态
    return updateRoomStatus(data.roomId, '使用中')
      .then(() => response) // 返回原始响应
      .catch(error => {
        // 状态更新失败时回滚预订
        return request({
          url: `/api/bookings/${response.data.id}`,
          method: 'delete'
        }).finally(() => Promise.reject(error))
      })
  })
}

// 获取用户预订记录
export function getUserBookings(userId) {
  return request({
    url: `/api/users/${userId}/bookings`,
    method: 'get'
  })
}

// 取消预订
export function cancelBooking(bookingId) {
  return request({
    url: `/api/bookings/${bookingId}`,
    method: 'get'
  }).then(response => {
    // 获取预订详情后更新房间状态
    const roomId = response.data.roomId
    return updateRoomStatus(roomId, '空闲')
      .then(() => {
        // 状态更新成功后取消预订
        return request({
          url: `/api/bookings/${bookingId}/cancel`,
          method: 'put'
        })
      })
  })
}

// 获取房间可用时间段
export function getRoomAvailability(roomId, date) {
  return request({
    url: `/api/rooms/${roomId}/availability`,
    method: 'get',
    params: { date }
  })
}
