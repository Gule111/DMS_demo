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

    <!-- 账户与材料信息行 -->
    <a-row :gutter="24" style="margin-top: 24px;">
      <!-- 左侧：当前账户信息 -->
      <a-col :span="16">
        <a-card title="当前账户信息" :bordered="false" class="info-card">
          <a-descriptions bordered :column="2">
            <a-descriptions-item label="用户ID">{{ userStore.userId }}</a-descriptions-item>
            <a-descriptions-item label="用户名">{{ userStore.username }}</a-descriptions-item>
            <a-descriptions-item label="手机号">{{ userStore.phone || '未绑定' }}</a-descriptions-item>
            <a-descriptions-item label="当前角色标识">{{ userStore.role }}</a-descriptions-item>
            <a-descriptions-item label="角色名称">{{ userStore.getRoleName() }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <!-- 右侧：我的材料 (学员专享) -->
      <a-col :span="8" v-if="userStore.role === 3">
        <a-card title="我的材料" :bordered="false" class="info-card">
          <div style="display: flex; flex-direction: column; gap: 16px;">
            <div class="doc-item">
              <span>📄 驾校报名表</span>
              <a-button type="link" @click="handleGeneratePdf('enrollment')" :loading="generatingPdf === 'enrollment'">生成并导出</a-button>
            </div>
            <div class="doc-item">
              <span>🏥 体检合格表</span>
              <a-button type="link" disabled>待体检</a-button>
            </div>
            <div class="doc-item">
              <span>🎫 考试准考证</span>
              <a-button type="link" disabled>暂无考试</a-button>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/store/user'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const userStore = useUserStore()
const generatingPdf = ref<string>('')

const shortcuts = [
  { path: '/app/registration', icon: '📝', title: '在线报名', desc: '提交报名材料，快速入学' },
  { path: '/app/coach', icon: '👨‍🏫', title: '教练分配', desc: '根据需求选择合适教练' },
  { path: '/app/progress', icon: '📚', title: '学习进度', desc: '查看各科目学习情况' },
  { path: '/app/exam', icon: '🏆', title: '考试管理', desc: '预约考试、查询成绩' },
]

// 触发后端生成 PDF
const handleGeneratePdf = async (type: string) => {
  generatingPdf.value = type
  try {
    // 目前只实现了报名表生成
    const res: any = await request.post('/document/generate/enrollment')
    if (res.data && res.data.fileUrl) {
      message.success('PDF 提取成功！')
      window.open(res.data.fileUrl, '_blank')
    }
  } catch (err: any) {
    console.error('PDF生成失败:', err)
  } finally {
    generatingPdf.value = ''
  }
}
</script>

<style scoped>
.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
}
.welcome-card {
  text-align: center;
  padding: 40px 0;
  border-radius: 12px;
}
.feature-card {
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}
.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.icon-wrapper {
  font-size: 32px;
  margin-bottom: 16px;
}
.info-card {
  border-radius: 8px;
  height: 100%;
}
.doc-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}
</style>
