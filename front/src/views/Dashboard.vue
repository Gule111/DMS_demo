<template>
  <div>
    <!-- 欢迎区域 -->
    <a-card style="margin-bottom: 24px;">
      <a-result status="success"
        :title="`欢迎回来，${userStore.username}！`"
        sub-title="您已成功登录 DMS 驾校报名系统，请从左侧菜单选择功能模块">
      </a-result>
    </a-card>

    <!-- 快捷功能入口 -->
    <a-row :gutter="16">
      <a-col :span="6" v-for="item in shortcuts" :key="item.path">
        <a-card hoverable @click="$router.push(item.path)">
          <template #cover>
            <div style="text-align: center; padding: 24px 0; font-size: 40px;">{{ item.icon }}</div>
          </template>
          <a-card-meta :title="item.title" :description="item.desc" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 用户信息 -->
    <a-card title="当前账户信息" style="margin-top: 24px;">
      <a-descriptions bordered :column="2">
        <a-descriptions-item label="用户ID">{{ userStore.userId }}</a-descriptions-item>
        <a-descriptions-item label="用户名">{{ userStore.username }}</a-descriptions-item>
        <a-descriptions-item label="手机号">{{ userStore.phone }}</a-descriptions-item>
        <a-descriptions-item label="角色">{{ userStore.getRoleName() }}</a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const shortcuts = [
  { path: '/registration', icon: '📝', title: '在线报名', desc: '提交报名材料，快速入学' },
  { path: '/coach', icon: '👨‍🏫', title: '教练分配', desc: '根据需求选择合适教练' },
  { path: '/progress', icon: '📚', title: '学习进度', desc: '查看各科目学习情况' },
  { path: '/exam', icon: '🏆', title: '考试管理', desc: '预约考试、查询成绩' },
]
</script>
