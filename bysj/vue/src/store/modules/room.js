import { getRooms, getAvailableRooms, getRoomsByStoreId } from '@/api/room'
import request from '@/utils/request'
import store from '@/store'

const state = {
  rooms: [],
  totalRooms: 0,
  availableRooms: []
}

const mutations = {
  SET_ROOMS(state, { list, total }) {
    state.rooms = [...list]
    state.totalRooms = total
  },
  SET_AVAILABLE_ROOMS(state, rooms) {
    state.availableRooms = [...rooms]
  }
}

const actions = {
  async fetchRooms({ commit }, params = {}) {
    try {
      console.log('Vuex fetchRooms 调用参数:', params);
      const { filterType = 'all', page = 1, size = 8, storeId, ...otherParams } = params
      let response;
      let apiParams = { page, size, ...otherParams };
      
      // 根据是否有storeId和filterType选择合适的API
      if (storeId) {
        // 有storeId时，使用getRoomsByStoreId API
        response = await getRoomsByStoreId(storeId, apiParams);
      } else {
        // 没有storeId时，根据filterType选择API
        const api = filterType === 'all' ? getRooms : getAvailableRooms;
        response = await api(apiParams);
      }
      
      console.log('Vuex fetchRooms API响应:', response);
      
      if (response.code === 200) {
        // 处理分页响应
        if (response.data && typeof response.data === 'object' && 'records' in response.data) {
          // 分页响应格式: { records: [...], total: xx, current: xx, size: xx, pages: xx }
          commit('SET_ROOMS', {
            list: response.data.records,
            total: response.data.total
          })
          console.log('Vuex fetchRooms 分页响应 - 记录数:', response.data.records.length, '总数:', response.data.total, '当前页:', response.data.current);
          return { 
            code: 200, 
            data: {
              records: response.data.records,
              total: response.data.total,
              current: response.data.current,
              size: response.data.size,
              pages: response.data.pages
            }
          }
        } else {
          // 兼容旧格式，直接是数组
          const data = Array.isArray(response.data) ? response.data : []
          commit('SET_ROOMS', {
            list: data,
            total: data.length
          })
          return { code: 200, data }
        }
      }
      throw new Error(response.message || '获取房间失败')
    } catch (error) {
      console.error('Vuex fetchRooms 错误:', error);
      throw new Error(error.message || '网络异常，请稍后重试')
    }
  },

  async createBooking({ commit }, bookingData) {
    try {
      const response = await request({
        url: '/api/usage-records',
        method: 'post',
        data: {
          roomId: bookingData.roomId,
          userId: store.state.userInfo.userId,
          hours: bookingData.hours,
          totalPrice: bookingData.totalPrice,
          status: 'paid'
        },
        headers: {
          'Authorization': 'Bearer ' + store.state.admin.adminToken
        }
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || '创建预订记录失败')
    }
  },

  async updateRoomStatus({ commit }, { roomId, status }) {
    try {
      const response = await request({
        url: `/api/rooms/${roomId}`,
        method: 'put',
        data: { status },
        headers: {
          'Authorization': 'Bearer ' + store.state.admin.adminToken
        }
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || '更新房间状态失败')
    }
  },

  // 更新单个房间状态
  updateSingleRoomStatus({ commit, state }, { roomId, newStatus }) {
    const rooms = [...state.rooms]
    const roomIndex = rooms.findIndex(room => room.roomId === roomId)
    
    if (roomIndex !== -1) {
      const statusMap = {
        '空闲': 'available',
        '使用中': 'occupied'
      }
      
      rooms[roomIndex] = {
        ...rooms[roomIndex],
        status: statusMap[newStatus] || newStatus
      }
      
      commit('SET_ROOMS', {
        list: rooms,
        total: state.totalRooms
      })
    }
  },

  // 静默获取房间数据（用于轮询）
  async fetchRoomsSilently({ commit }, params = {}) {
    try {
      const { filterType = 'all', page = 1, size = 8, ...otherParams } = params
      const api = filterType === 'all' ? getRooms : getAvailableRooms
      const response = await api({ page, size, ...otherParams }) // 传递所有参数，包括timestamp
      
      if (response.code === 200) {
        const data = Array.isArray(response.data) ? response.data : []
        return { code: 200, data }
      }
      return { code: response.code, data: [] }
    } catch (error) {
      console.warn('静默获取房间数据失败:', error)
      return { code: 500, data: [] }
    }
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
