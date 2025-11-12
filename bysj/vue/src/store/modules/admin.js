import { adminLogin, adminLogout, getAdminInfo } from '@/api/admin'

const state = {
    adminToken: localStorage.getItem('adminToken') || '',
    adminInfo: null,
    storeId: localStorage.getItem('storeId') || null
}

const mutations = {
    SET_ADMIN_TOKEN: (state, token) => {
        state.adminToken = token
        localStorage.setItem('adminToken', token)
    },
    SET_ADMIN_INFO: (state, info) => {
        state.adminInfo = info
    },
    SET_STORE_ID: (state, storeId) => {
        state.storeId = storeId
        localStorage.setItem('storeId', storeId)
    },
    CLEAR_ADMIN_STATE: (state) => {
        state.adminToken = ''
        state.adminInfo = null
        state.storeId = null
        localStorage.removeItem('adminToken')
        localStorage.removeItem('storeId')
    }
}

const actions = {
    adminLogin({ commit }, adminInfo) {
        const { username, password } = adminInfo
        console.log('开始管理员登录流程，用户名:', username.trim()) // 调试信息

        return new Promise((resolve, reject) => {
            adminLogin({ username: username.trim(), password })
                .then(response => {
                    console.log('登录API响应:', response) // 调试信息
                    const { data: token } = response

                    if (!token) {
                        throw new Error('未获取到token')
                    }

                    commit('SET_ADMIN_TOKEN', token)
                    console.log('Token已设置:', token) // 调试信息

                    // 确保token已设置后再获取管理员信息
                    getAdminInfo(username.trim())
                        .then(infoResponse => {
                            console.log('管理员信息API响应:', infoResponse) // 调试信息

                            if (!infoResponse.data) {
                                throw new Error('获取管理员信息失败: 响应数据为空')
                            }

                            // 确保响应数据存在且包含必要字段
                            if (!infoResponse.data.adminId) {
                                throw new Error('获取管理员信息不完整: 缺少adminId')
                            }

                            // 设置完整的adminInfo，包括role和storeId
                            const adminInfo = {
                                username: username.trim(),
                                id: infoResponse.data.adminId,
                                role: infoResponse.data.role || 'admin', // 默认角色
                                storeId: infoResponse.data.storeId // 添加店铺ID
                            }

                            console.log('准备设置adminInfo:', adminInfo) // 调试信息
                            commit('SET_ADMIN_INFO', adminInfo)
                            
                            // 如果有店铺ID，也设置到store中
                            if (infoResponse.data.storeId) {
                                commit('SET_STORE_ID', infoResponse.data.storeId)
                            }
                            
                            console.log('adminInfo设置完成，当前store状态:', this.state) // 调试信息
                            resolve(response)
                        })
                        .catch(error => {
                            console.error('获取管理员信息失败:', error) // 调试信息
                            commit('CLEAR_ADMIN_STATE')
                            reject(new Error('获取管理员信息失败: ' + (error.response?.data?.message || error.message)))
                        })
                })
                .catch(error => {
                    console.error('登录失败:', error) // 调试信息
                    reject(error)
                })
        })
    },
    adminLogout({ commit, state }) {
        return new Promise((resolve, reject) => {
            const adminToken = localStorage.getItem('adminToken')
            if (!adminToken) {
                commit('CLEAR_ADMIN_STATE')
                return resolve()
            }

            const adminId = state.adminInfo?.id
            if (!adminId) {
                commit('CLEAR_ADMIN_STATE')
                return reject(new Error('无法获取管理员ID'))
            }

            adminLogout(adminId)
                .then(() => {
                    commit('CLEAR_ADMIN_STATE')
                    resolve()
                })
                .catch(error => {
                    commit('CLEAR_ADMIN_STATE')
                    reject(new Error(error.response?.data?.message || '退出失败'))
                })
        })
    }
}

const getters = {
    adminToken: state => state.adminToken,
    adminInfo: state => state.adminInfo,
    storeId: state => state.storeId
}

export default {
    namespaced: true,
    state,
    mutations,
    actions,
    getters
}
