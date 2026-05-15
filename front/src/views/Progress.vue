<template>
  <div class="progress-container">
    <a-card title="📊 学习进度追踪" :bordered="false" class="main-card">
      <template #extra>
        <a-tag color="blue">当前等级: {{ studentLevel }}</a-tag>
      </template>

      <!-- 步骤条：展示四个科目阶段 -->
      <div class="steps-wrapper">
        <a-steps :current="currentStep" status="process">
          <a-step title="科目一" description="理论知识学习" />
          <a-step title="科目二" description="场内技能训练" />
          <a-step title="科目三" description="道路驾驶技能" />
          <a-step title="科目四" description="文明驾驶常识" />
        </a-steps>
      </div>

      <a-divider />

      <!-- 详细学时进度 -->
      <div class="progress-details">
        <a-row :gutter="24">
          <a-col :span="12" v-for="item in progressList" :key="item.subject">
            <div class="subject-card">
              <div class="subject-header">
                <span class="subject-title">科目 {{ subjectMap[item.subject] }}</span>
                <a-tag :color="statusMap[item.status].color">{{ statusMap[item.status].text }}</a-tag>
              </div>
              <div class="progress-bar-wrapper">
                <div class="progress-info">
                  <span>已累计学时: {{ item.hoursDone }} / {{ requiredHours[item.subject] }}h</span>
                  <span>{{ Math.round((item.hoursDone / requiredHours[item.subject]) * 100) }}%</span>
                </div>
                <a-progress 
                  :percent="Math.min(100, Math.round((item.hoursDone / requiredHours[item.subject]) * 100))" 
                  :status="item.status === 2 ? 'success' : 'active'"
                  :show-info="false"
                />
              </div>
            </div>
          </a-col>
        </a-row>
      </div>
    </a-card>

    <!-- 训练记录时间线 -->
    <a-card title="🕒 最近训练记录" :bordered="false" style="margin-top: 24px;">
      <a-empty v-if="records.length === 0" description="暂无训练记录" />
      <a-timeline v-else mode="alternate">
        <a-timeline-item v-for="record in records" :key="record.id" :color="record.hours >= 2 ? 'green' : 'blue'">
          <template #dot>
            <span style="font-size: 16px;">🚗</span>
          </template>
          <div class="record-item">
            <div class="record-date">{{ formatDate(record.trainingDate) }}</div>
            <div class="record-content">
              <strong>科目 {{ subjectMap[record.subject] }} 训练</strong>
              <span class="record-hours">+{{ record.hours }} 学时</span>
            </div>
            <p class="record-desc">{{ record.content || '教练未填写评价' }}</p>
          </div>
        </a-timeline-item>
      </a-timeline>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

const progressList = ref<any[]>([])
const records = ref<any[]>([])

const subjectMap: any = { 1: '一', 2: '二', 3: '三', 4: '四' }
const requiredHours: any = { 1: 12, 2: 16, 3: 24, 4: 10 }
const statusMap: any = {
  0: { text: '学习中', color: 'blue' },
  1: { text: '已达标', color: 'orange' },
  2: { text: '已通过', color: 'green' }
}

// 计算当前进行到哪一步
const currentStep = computed(() => {
  const index = progressList.value.findIndex(p => p.status !== 2)
  return index === -1 ? 4 : index
})

const studentLevel = computed(() => {
  if (currentStep.value >= 4) return '准驾驶员'
  return `科目${subjectMap[currentStep.value + 1]}学员`
})

const fetchProgress = async () => {
  try {
    const res: any = await request.get('/progress/my')
    progressList.value = res.data || []
  } catch (err) {
    console.error('获取进度失败:', err)
  }
}

const fetchRecords = async () => {
  try {
    const res: any = await request.get('/progress/records')
    records.value = res.data || []
  } catch (err) {
    console.error('获取记录失败:', err)
  }
}

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  fetchProgress()
  fetchRecords()
})
</script>

<style scoped>
.progress-container {
  padding: 12px;
}
.main-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.steps-wrapper {
  padding: 24px 0;
}
.progress-details {
  margin-top: 24px;
}
.subject-card {
  background: #f8fafc;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
}
.subject-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.subject-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}
.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #64748b;
  margin-bottom: 8px;
}
.record-item {
  text-align: left;
}
.record-date {
  font-size: 12px;
  color: #94a3b8;
}
.record-content {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 4px 0;
}
.record-hours {
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
  color: #3b82f6;
  font-weight: 500;
}
.record-desc {
  color: #64748b;
  font-style: italic;
  font-size: 13px;
}
</style>
