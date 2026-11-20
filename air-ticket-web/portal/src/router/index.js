import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '../store'

Vue.use(VueRouter)

import Home from '../views/Home.vue'
import CompanyList from '../views/CompanyList.vue'
import CompanyDetail from '../views/CompanyDetail.vue'
import NewsList from '../views/NewsList.vue'
import NewsDetail from '../views/NewsDetail.vue'
import FlightSearch from '../views/FlightSearch.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import UserCenter from '../views/UserCenter.vue'
import MyOrders from '../views/MyOrders.vue'
import AiPlanner from '../views/AiPlanner.vue'

const Booking = () => import('../views/Booking.vue')

const routes = [
  { path: '/user/orders/:id', name: 'OrderDetail', component: () => import('../views/OrderDetail.vue'), meta: { requiresAuth: true } },
  {
    path: '/planner',
    name: 'AiPlanner',
    component: AiPlanner,
    meta: { requiresAuth: true },
  },
  {
    path: '/booking/:holdId',
    name: 'Booking',
    component: Booking,
    meta: { requiresAuth: true },
  },
  { path: '/', name: 'Home', component: Home },
  { path: '/company', name: 'CompanyList', component: CompanyList },
  { path: '/company/:id', name: 'CompanyDetail', component: CompanyDetail },
  { path: '/news', name: 'NewsList', component: NewsList },
  { path: '/news/:id', name: 'NewsDetail', component: NewsDetail },
  { path: '/flight', name: 'FlightSearch', component: FlightSearch },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  {
    path: '/user/center',
    name: 'UserCenter',
    component: UserCenter,
    meta: { requiresAuth: true },
  },
  {
    path: '/user/orders',
    name: 'MyOrders',
    component: MyOrders,
    meta: { requiresAuth: true },
  },
]

const router = new VueRouter({
  mode: 'hash',
  scrollBehavior() {
    return { x: 0, y: 0 }
  },
  routes,
})

router.beforeEach((to, from, next) => {
  const user = store.state.user
  if (to.matched.some((record) => record.meta.requiresAuth)) {
    if (!user || !user.token) {
      next({ path: '/login', query: { redirect: to.fullPath } })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
