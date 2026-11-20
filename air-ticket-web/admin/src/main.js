import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import '../../shared/theme.css'

Vue.use(ElementUI)
Vue.config.productionTip = false

// A compact action column leaves room for row content on phones.
const compactMedia = window.matchMedia('(max-width: 600px)')
const viewport = Vue.observable({ compact: compactMedia.matches })
const updateViewport = event => { viewport.compact = event.matches }
if (compactMedia.addEventListener) compactMedia.addEventListener('change', updateViewport)
else compactMedia.addListener(updateViewport)
Vue.prototype.$viewport = viewport

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')