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
  }
})
