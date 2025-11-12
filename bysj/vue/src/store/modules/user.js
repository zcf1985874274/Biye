import { login, logout, getUserInfo } from '@/api/user'

const state = {
  token: localStorage.getItem('token') || '',
  username: localStorage.getItem('username') || '',
  userInfo: null
}

const mutations = {
  SET_TOKEN: (state, token) => {
    state.token = token
    localStorage.setItem('token', token)
  },
  SET_USERNAME: (state, username) => {
    state.username = username
    localStorage.setItem('username', username)
  },
  SET_USER_INFO: (state, userInfo) => {
    state.userInfo = userInfo
  },
  CLEAR_AUTH: (state) => {
    try {
      // 清除Vuex状态
      state.token = ''
      state.username = ''
      state.userInfo = null

      // 清除所有存储的认证数据
      const clearStorage = () => {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('username')

        // 清除所有cookie
        document.cookie.split(';').forEach(cookie => {
          const eqPos = cookie.indexOf('=')
          const name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie
          document.cookie = name + '=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/'
        })
      }

      clearStorage()
      console.log('已强制清除所有认证信息')

      // 确保清除后状态
      console.log('清除后检查:',
          'localStorage.token:', localStorage.getItem('token'),
          'sessionStorage.token:', sessionStorage.getItem('token')
      )
    } catch (error) {
      console.error('清除认证信息时出错:', error)
    }
  },

  // 添加手动重置状态的方法
  RESET_STATE: (state) => {
    state.token = ''
    state.username = ''
    state.userInfo = null
    console.log('已重置用户模块状态')
  }
}

const actions = {
  // 用户登录
  login({ commit }, userInfo) {
    return new Promise((resolve, reject) => {
      login(userInfo).then(response => {
        console.log('登录响应:', response) // 调试日志

        // 检查响应结构
        if (!response || !response.data) {
          reject(new Error('无效的响应格式'))
          return
        }

        // 检查token是否存在
        const token = response.data.token || response.data
        if (!token) {
          reject(new Error('登录失败：未获取到有效token'))
          return
        }

        console.log('登录成功，获取到的token:', token)
        commit('SET_TOKEN', token)
        commit('SET_USERNAME', userInfo.username)
        resolve()
      }).catch(error => {
        console.error('登录请求失败:', error)
        reject(error)
      })
    })
  },
  // 获取用户信息
  getUserInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      getUserInfo(state.username).then(response => {
        commit('SET_USER_INFO', response.data)
        resolve(response.data)
      }).catch(error => {
        reject(error)
      })
    })
  },
  // 用户退出
  logout({ commit, state }) {
    return new Promise((resolve, reject) => {
      if (!state.username) {
        reject(new Error('退出失败：无法获取用户名'))
        return
      }
      logout(state.username).then(() => {
        commit('CLEAR_AUTH')
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  }
}

const getters = {
  token: state => state.token,
  username: state => state.username,
  userInfo: state => state.userInfo
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
