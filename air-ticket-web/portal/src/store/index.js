function persisted(key, fallback) { try { return JSON.parse(localStorage.getItem(key) || 'null') || fallback } catch(e) { localStorage.removeItem(key); return fallback } }
import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    user: persisted('portalUser', null)
  },
  mutations: {
    SET_USER(state, user) {
      state.user = user
    }
  },
  actions: {
    login({ commit }, user) {
      commit('SET_USER', user)
      localStorage.setItem('portalUser', JSON.stringify(user))
    },
    async logout({ commit, state }) {
      const token=state.user && state.user.token
      if (token) { try { await fetch('/api/user/logout', { method: 'POST', headers: { Authorization: `Bearer ${token}` } }) } catch(e) {} }
      commit('SET_USER', null)
      localStorage.removeItem('portalUser')
    }
  }
})