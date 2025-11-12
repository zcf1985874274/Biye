import Vue from 'vue'
import VueRouter from 'vue-router'
import Login from '@/components/auth/Login.vue'
import Register from '@/components/auth/Register.vue'
import Home from '@/components/Home.vue'
import roomRouter from './modules/room'
import store from "@/store";

Vue.use(VueRouter)

const routes = [
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('@/components/OrderRecords.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true }
  },
  {
    path: '/adminhome',
    name: 'AdminHome',
    component: () => import('@/components/AdminHome.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'admin-management',
        name: 'AdminManagement',
        component: () => import('@/views/admin/AdminManagement.vue'),
        meta: { requiresSuperAdmin: true }
      },
      {
        path: 'user-management',
        name: 'UserManagement',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'room-management',
        name: 'RoomManagement',
        component: () => import('@/views/admin/RoomManagement.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'usage-record-management',
        name: 'UsageRecordManagement',
        component: () => import('@/views/admin/UsageRecordManagement.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/admin/StatisticsView.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'store-management',
        name: 'StoreManagement',
        component: () => import('@/views/admin/StoreManagement.vue'),
        meta: { requiresAdmin: true }
      }
    ]
  },
  // 添加房间相关路由
  {
    ...roomRouter,
    meta: { requiresAuth: true }
  },
  // 管理员登录路由
  {
    path: '/adminlogin',
    name: 'AdminLogin',
    component: () => import('@/components/auth/AdminLogin.vue')
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// 导航守卫
router.beforeEach((to, from, next) => {
  const token = store.getters['user/token']
  const adminToken = store.getters['admin/adminToken']

  // 防止重定向到登录或注册页
  if (token && (to.path === '/login' || to.path === '/register')) {
    return next({ path: '/home', replace: true })
  }
  if (adminToken && (to.path === '/adminlogin')) {
    return next({ path: '/adminhome', replace: true })
  }

  // 需要认证的路由
  if (to.matched.some(record => record.meta.requiresAuth)) {
    // 优先检查管理员token
    if (adminToken) {
      if (to.path === '/adminlogin') {
        return next({ path: '/adminhome', replace: true })
      }
      // 验证token是否有效
      return next()
    }

    // 普通用户token检查
    if (!token) {
      // 避免重定向到当前路径或登录/注册页
      const isInvalidRedirect = to.path === '/login' || to.path === '/register' || to.path === '/adminlogin'
      return next({
        path: '/login',
        query: isInvalidRedirect ? {} : { redirect: to.fullPath },
        replace: true
      })
    }

    // 检查管理员权限
    if (to.matched.some(record => record.meta.requiresAdmin)) {
      if (!adminToken) {
        return next({ path: '/home', replace: true })
      }
      // 确保有adminToken时不跳转到普通用户页面
      if (token) {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
      }
    }
  }

  next()
})

export default router
