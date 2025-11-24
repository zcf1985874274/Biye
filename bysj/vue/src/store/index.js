import Vue from 'vue'
import Vuex from 'vuex'
import user from './modules/user'
import admin from './modules/admin'
import room from './modules/room'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    user,
    admin,
    room
  },
  getters: {
    token: state => state.user.token,
    username: state => state.user.username,
    userInfo: state => state.user.userInfo,
    adminToken: state => state.admin.adminToken,
    adminInfo: state => state.admin.adminInfo,
    adminUsername: state => state.admin.adminInfo?.username,
    storeId: state => state.admin.storeId
  }
})
