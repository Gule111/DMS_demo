import { createRouter, createWebHistory } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/store/user'
import { getMyRoutes } from '@/api/menu'

// 动态导入工具
const views = import.meta.glob('@/views/**/*.vue')

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
    children: []
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由加载标识，防止重复请求
let isRoutesLoaded = false

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  // 1. 未登录处理
  if (to.meta.requiresAuth && !userStore.token) {
    return next('/login')
  }

  // 2. 已登录且未加载动态路由
  if (userStore.token && !isRoutesLoaded) {
    try {
      const res: any = await getMyRoutes()
      const menus = res.data || []
      userStore.setMenus(menus) // 保存到 Store
      
      menus.forEach((menu: any) => {
        let componentPath = ''
        if (menu.component.includes('Dashboard')) componentPath = '/src/views/Dashboard.vue'
        else if (menu.component.includes('UserManage')) componentPath = '/src/views/UserManage.vue'
        else if (menu.component.includes('Enrollment') || menu.component.includes('Registration')) componentPath = '/src/views/Registration.vue'
        else if (menu.component.includes('Coach') || menu.component.includes('AssignInstructor')) componentPath = '/src/views/Coach.vue'
        else if (menu.component.includes('Progress')) componentPath = '/src/views/Progress.vue'
        else if (menu.component.includes('Exam')) componentPath = '/src/views/Exam.vue'
        else if (menu.component.includes('BaseInfo')) componentPath = '/src/views/BaseInfo.vue'

        if (views[componentPath]) {
          router.addRoute('Layout', {
            path: menu.path.replace('/admin/', '').replace('/student/', '').replace('/instructor/', ''),
            name: menu.menuName,
            component: views[componentPath],
            meta: { title: menu.menuName, icon: menu.icon || '📍' }
          })
        }
      })

      isRoutesLoaded = true
      // 动态添加完路由后，必须用 next(to.fullPath) 触发一次重新匹配，否则当前导航会失败
      return next({ ...to, replace: true })
    } catch (e) {
      console.error('动态路由加载失败:', e)
      userStore.clearUser()
      return next('/login')
    }
  }

  // 3. 登录页重复进入处理
  if (userStore.token && (to.path === '/login' || to.path === '/register' || to.path === '/')) {
    return next('/app/dashboard')
  }

  next()
})

/**
 * 退出登录时调用，重置加载标识
 */
export function resetRouter() {
  isRoutesLoaded = false
}

export default router
