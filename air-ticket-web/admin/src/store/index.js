function persisted(key, fallback) { try { return JSON.parse(localStorage.getItem(key) || 'null') || fallback } catch(e) { localStorage.removeItem(key); return fallback } }
import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    token: localStorage.getItem('adminToken') || '',
    admin: persisted('adminInfo', null),
    menus: persisted('adminMenus', [])
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
    },
    SET_ADMIN(state, admin) {
      state.admin = admin
    },
    SET_MENUS(state, menus) {
      state.menus = menus
    },
    LOGOUT(state) {
      state.token = ''
      state.admin = null
      state.menus = []
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminInfo')
      localStorage.removeItem('adminMenus')
    }
  },
  actions: {
    login({ commit }, data) {
      commit('SET_TOKEN', data.token)
      commit('SET_ADMIN', data.admin)
      commit('SET_MENUS', data.menus)
      localStorage.setItem('adminToken', data.token)
      localStorage.setItem('adminInfo', JSON.stringify(data.admin))
      localStorage.setItem('adminMenus', JSON.stringify(data.menus))
    }
  }
})