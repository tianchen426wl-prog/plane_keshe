import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '@/store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/admin/login'
  },
  {
    path: '/admin/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/admin',
    component: () => import('@/views/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Index.vue')
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/UserList.vue')
      },
      {
        path: 'company',
        name: 'CompanyManage',
        component: () => import('@/views/company/CompanyManage.vue')
      },
      {
        path: 'flight',
        name: 'FlightManage',
        component: () => import('@/views/hbxx/FlightManage.vue')
      },
      {
        path: 'order',
        name: 'OrderManage',
        component: () => import('@/views/jpdd/OrderManage.vue')
      },
      {
        path: 'refund',
        name: 'RefundManage',
        component: () => import('@/views/tdjp/RefundManage.vue')
      },
      {
        path: 'news',
        name: 'NewsManage',
        component: () => import('@/views/news/NewsManage.vue')
      },
      {
        path: 'menu',
        name: 'MenuManage',
        component: () => import('@/views/menu/MenuManage.vue')
      },
      {
        path: 'role',
        name: 'RoleManage',
        component: () => import('@/views/role/RoleManage.vue')
      }
    ]
  }
]

const router = new VueRouter({
  mode: 'hash',
  routes
})

router.beforeEach((to, from, next) => {
  if (to.path === '/admin/login') {
    next()
  } else if (to.path.startsWith('/admin')) {
    const token = store.state.token
    if (!token) {
      next('/admin/login')
    } else {
      const admin=store.state.admin
      const allowed=(store.state.menus || []).some(m => m.path === to.path)
      if (admin && admin.role !== 0 && to.path !== '/admin/dashboard' && !allowed) {
        next('/admin/dashboard')
      } else next()
    }
  } else {
    next()
  }
})

export default router