import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
  },
  {
    path: '/app',
    name: 'Layout',
    component: () => import('@/views/Layout.vue'),
    meta: { requiresAuth: true },
    redirect: '/app/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页概览', icon: '📊' },
      },
      {
        path: 'registration',
        name: 'Registration',
        component: () => import('@/views/Registration.vue'),
        meta: { title: '在线报名', icon: '📝' },
      },
      {
        path: 'coach',
        name: 'Coach',
        component: () => import('@/views/Coach.vue'),
        meta: { title: '教练分配', icon: '👨‍🏫' },
      },
      {
        path: 'progress',
        name: 'Progress',
        component: () => import('@/views/Progress.vue'),
        meta: { title: '学习进度', icon: '📚' },
      },
      {
        path: 'exam',
        name: 'Exam',
        component: () => import('@/views/Exam.vue'),
        meta: { title: '考试管理', icon: '🏆' },
      },
      {
        path: 'base-info',
        name: 'BaseInfo',
        component: () => import('@/views/BaseInfo.vue'),
        meta: { title: '基础信息', icon: '⚙️' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register' || to.path === '/') && userStore.token) {
    // 已登录访问登录/注册页，直接进主系统
    next('/app/dashboard')
  } else {
    next()
  }
})

export default router
