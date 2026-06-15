<template>
  <div class="dashboard-container">
    <!-- 欢迎区域 -->
    <a-card class="welcome-card" :bordered="false">
      <a-result status="success"
        :title="`欢迎回来，${userStore.username}！`"
        sub-title="您已成功登录 DMS 驾校管理系统，请从下方快捷入口开始工作。">
      </a-result>
    </a-card>

    <!-- 快捷功能入口 -->
    <a-row :gutter="[24, 24]" style="margin-top: 24px;">
      <a-col :xs="12" :sm="8" :md="6" v-for="item in shortcuts" :key="item.path">
        <a-card hoverable class="feature-card" @click="$router.push(item.path)">
          <div class="icon-wrapper">
            <component :is="item.icon" />
          </div>
          <div class="feature-title">{{ item.title }}</div>
          <div class="feature-desc">{{ item.desc }}</div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 账户与材料信息行 -->
    <a-row :gutter="24" style="margin-top: 24px;">
      <!-- 左侧：当前账户信息 -->
      <a-col :span="userStore.role === 3 ? 16 : 24">
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
              <span>驾校报名表</span>
              <a-button type="link" v-if="getDoc('EnrollmentForm')" @click="downloadDoc(getDoc('EnrollmentForm'))">导出 PDF</a-button>
              <a-button type="link" disabled v-else>待生成</a-button>
            </div>
            <div class="doc-item">
              <span>体检合格表</span>
              <a-button type="link" v-if="getDoc('HealthCert')" @click="downloadDoc(getDoc('HealthCert'))">导出 PDF</a-button>
              <a-button type="link" disabled v-else>待生成</a-button>
            </div>
            <div class="doc-item">
              <span>考试准考证</span>
              <a-button type="link" v-if="getDoc('ExamTicket')" @click="downloadDoc(getDoc('ExamTicket'))">导出 PDF</a-button>
              <a-button type="link" disabled v-else>待生成</a-button>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { 
  FormOutlined, 
  TeamOutlined, 
  ScheduleOutlined, 
  LineChartOutlined, 
  SafetyCertificateOutlined,
  BarChartOutlined,
  SolutionOutlined
} from '@ant-design/icons-vue'

const userStore = useUserStore()
const generatedDocs = ref<any[]>([])

const shortcuts = computed(() => {
  const currentRole = Number(userStore.role)
  const allShortcuts = [
    { path: '/app/registration', title: '在线报名', desc: '提交报名材料，快速入学', icon: FormOutlined, roles: [3] },
    { path: '/app/roster', title: '学员名册', desc: '查看名下所有学员', icon: TeamOutlined, roles: [2] },
    { path: '/app/progress-entry', title: '进度录入', desc: '一键录入练车学时', icon: FormOutlined, roles: [2] },
    { path: '/app/schedule', title: '约课日程', desc: '设置个人档期时间', icon: ScheduleOutlined, roles: [2] },
    { path: '/app/feedback', title: '成绩反馈', desc: '录入考试成绩与评价', icon: SafetyCertificateOutlined, roles: [2] },
    { path: '/app/coach', title: '教练管理', desc: '管理全校教练师资', icon: TeamOutlined, roles: [1] },
    { path: '/app/assign', title: '分配管理', desc: '审核报名并分配教练', icon: SolutionOutlined, roles: [1] },
    { path: '/app/statistics', title: '统计分析', desc: '全局经营与考试数据统计', icon: BarChartOutlined, roles: [1] },
    { path: '/app/progress', title: '学习进度', desc: '查看各科目学习情况', icon: LineChartOutlined, roles: [3] },
    { path: '/app/exam', title: '预约考场', desc: '预约考试、查询成绩', icon: SafetyCertificateOutlined, roles: [1, 3] },
    { path: '/app/baseinfo', title: '基础信息', desc: '管理考场与配置', icon: ScheduleOutlined, roles: [1] },
  ]
  return allShortcuts.filter(item => item.roles.includes(currentRole))
})

// 获取已生成的文档
const fetchDocs = async () => {
  try {
    const res: any = await request.get('/docs/my')
    generatedDocs.value = res.data || []
  } catch (err) {
    console.error('获取材料失败:', err)
  }
}

const getDoc = (type: string) => {
  return generatedDocs.value.find(d => d.docType === type)?.fileUrl
}

const downloadDoc = (url: string) => {
  window.open(url, '_blank')
}

onMounted(() => {
  if (userStore.role === 3) {
    fetchDocs()
  }
})
</script>

<style scoped>
.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
}
.welcome-card {
  text-align: center;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.feature-card {
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  height: 100%;
}
.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}
.icon-wrapper {
  font-size: 32px;
  margin-bottom: 12px;
  color: #1890ff;
}
.feature-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #262626;
}
.feature-desc {
  font-size: 13px;
  color: #8c8c8c;
}
.info-card {
  border-radius: 12px;
  height: 100%;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.doc-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
</style>
