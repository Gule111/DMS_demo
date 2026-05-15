<template>
  <a-layout class="home-container" style="min-height: 100vh;">
    <!-- 侧边栏 -->
    <a-layout-sider v-model:collapsed="collapsed" collapsible theme="dark">
      <div style="height: 48px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; margin: 8px 0;">
        {{ collapsed ? 'D' : 'DMS 管理系统' }}
      </div>
      <a-menu theme="dark" mode="inline" :selectedKeys="selectedKeys" @click="handleMenuClick">
        <a-menu-item v-for="route in visibleMenus" :key="route.path">
          <span>{{ route.meta?.title }}</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <!-- 右侧区域 -->
    <a-layout>
      <!-- 顶部导航 -->
      <a-layout-header style="background: #fff; padding: 0 24px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 2px 8px rgba(0,0,0,0.06);">
        <span style="font-size: 16px; color: #333;">{{ currentTitle }}</span>
        <div style="display: flex; align-items: center; gap: 16px;">
          <span>{{ userStore.username }}（{{ userStore.getRoleName() }}）</span>
          <a-button type="text" danger @click="handleLogout">退出登录</a-button>
        </div>
      </a-layout-header>

      <!-- 内容区域 -->
      <a-layout-content style="margin: 16px; padding: 24px; background: #fff; border-radius: 8px; min-height: 360px;">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { logout } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const collapsed = ref(false)

/**
 * 根据当前用户角色过滤可见的菜单项
 * 从 Layout 路由的 children 中读取，只显示 meta.roles 包含当前角色的路由
 */
const visibleMenus = computed(() => {
  const userRole = Number(userStore.role)
  
  // 直接从路由原始配置中获取 Layout 的子路由
  const layoutRoute = router.options.routes.find(r => r.name === 'Layout')
  const children = layoutRoute?.children || []

  return children
    .filter(child => {
      // 过滤掉重定向路由和没有 title 的路由
      if (child.redirect || !child.meta?.title) return false
      
      const roles = child.meta?.roles as number[] | undefined
      if (!roles) return true
      return roles.includes(userRole)
    })
    .map(child => ({
      path: child.path,
      meta: child.meta
    }))
})

// 当前选中的菜单
const selectedKeys = computed(() => {
  const path = route.path.replace('/app/', '')
  return [path]
})

// 当前页面标题
const currentTitle = computed(() => {
  return (route.meta?.title as string) || 'DMS 驾校管理系统'
})

// 菜单点击
function handleMenuClick({ key }: { key: string }) {
  router.push('/app/' + key)
}

// 退出登录
async function handleLogout() {
  try {
    await logout()
  } catch {
  }
  userStore.clearUser()
  message.success('已退出登录')
  router.push('/')
}
</script>
