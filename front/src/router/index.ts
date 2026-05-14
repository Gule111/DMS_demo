import { createRouter, createWebHistory } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/store/user'

/**
 * 静态路由配置
 * 通过 meta.roles 控制哪些角色可以看到该菜单
 * 角色: 1=管理员, 2=教练员, 3=学员
 */
const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
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
        meta: { title: '工作台', icon: '📊', roles: [1, 2, 3] }
      },
      {
        path: 'registration',
        name: 'Registration',
        component: () => import('@/views/Registration.vue'),
        meta: { title: '在线报名', icon: '📝', roles: [1, 3] }
      },
      {
        path: 'coach',
        name: 'Coach',
        component: () => import('@/views/Coach.vue'),
        meta: { title: '教练管理', icon: '👨‍🏫', roles: [1, 2, 3] }
      },
      {
        path: 'assign',
        name: 'Assign',
        component: () => import('@/views/Assign.vue'),
        meta: { title: '分配管理', icon: '🎯', roles: [1] }
      },
      {
        path: 'progress',
        name: 'Progress',
        component: () => import('@/views/Progress.vue'),
        meta: { title: '学习进度', icon: '📚', roles: [1, 2, 3] }
      },
      {
        path: 'exam',
        name: 'Exam',
        component: () => import('@/views/Exam.vue'),
        meta: { title: '考试管理', icon: '🏆', roles: [1, 3] }
      },
      {
        path: 'baseinfo',
        name: 'BaseInfo',
        component: () => import('@/views/BaseInfo.vue'),
        meta: { title: '基础信息', icon: '⚙️', roles: [1] }
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/UserManage.vue'),
        meta: { title: '用户管理', icon: '👥', roles: [1] }
      },
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  // 1. 需要认证但未登录 → 跳转登录页
  if (to.meta.requiresAuth && !userStore.token) {
    return next('/login')
  }

  // 2. 已登录时访问登录/注册页 → 跳转工作台
  if (userStore.token && (to.path === '/login' || to.path === '/register' || to.path === '/')) {
    return next('/app/dashboard')
  }

  // 3. 权限检查：如果路由定义了 roles，检查当前用户角色是否在允许列表中
  const roles = to.meta.roles as number[] | undefined
  if (roles && userStore.token) {
    const userRole = Number(userStore.role)
    if (!roles.includes(userRole)) {
      message.warning('您没有权限访问该页面')
      return next('/app/dashboard')
    }
  }

  next()
})

export default router
