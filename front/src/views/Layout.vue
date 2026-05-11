<template>
  <a-layout class="home-container" style="min-height: 100vh;">
    <!-- 侧边栏 -->
    <a-layout-sider v-model:collapsed="collapsed" collapsible theme="dark">
      <div style="height: 48px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; margin: 8px 0;">
        {{ collapsed ? '🚗' : '🚗 DMS' }}
      </div>
      <a-menu theme="dark" mode="inline" :selectedKeys="selectedKeys" @click="handleMenuClick">
        <a-menu-item v-for="route in menuRoutes" :key="route.path">
          <span>{{ route.meta?.icon }}</span>
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

// 从路由配置中提取菜单项
const menuRoutes = computed(() => {
  const layoutRoute = router.options.routes.find((r) => r.path === '/app')
  return layoutRoute?.children || []
})

// 当前选中的菜单
const selectedKeys = computed(() => {
  const path = route.path.replace('/app/', '')
  return [path]
})

// 当前页面标题
const currentTitle = computed(() => {
  return (route.meta?.title as string) || 'DMS 驾校报名系统'
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
    // 即使后端报错，前端也要清除状态
  }
  userStore.clearUser()
  message.success('已退出登录')
  router.push('/')
}
</script>
