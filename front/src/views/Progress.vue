<template>
  <div class="progress-container">
    <a-page-header title="学习进度追踪" sub-title="实时同步练车学时与考试状态" />

    <a-card :bordered="false" class="main-card">
      <template #extra>
        <div class="level-badge">
          <span class="label">当前等级:</span>
          <a-tag color="blue">{{ studentLevel }}</a-tag>
        </div>
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
        <a-row :gutter="[24, 24]">
          <a-col :xs="24" :sm="12" v-for="item in progressList" :key="item.subject">
            <div class="subject-card" :class="{ 'card-locked': isLocked(item.subject) }">
              <div class="subject-header">
                <div class="title-group">
                  <span class="subject-title">科目 {{ subjectMap[item.subject] }}</span>
                  <span class="subject-subtitle">{{ getSubjectSubtitle(item.subject) }}</span>
                </div>
                <a-tag :color="statusMap[item.status].color">{{ statusMap[item.status].text }}</a-tag>
              </div>
              
              <div class="progress-body">
                <!-- 理论与实操科目显示进度条 -->
                <div class="progress-bar-wrapper" v-if="item.subject === 1 || item.subject === 2 || item.subject === 3">
                  <div class="progress-info">
                    <span>已累计学时: <b>{{ item.hoursDone }}</b> / {{ requiredHours[item.subject] }}h</span>
                    <span class="percent">{{ Math.round((item.hoursDone / requiredHours[item.subject]) * 100) }}%</span>
                  </div>
                  <a-progress 
                    :percent="Math.min(100, Math.round((item.hoursDone / requiredHours[item.subject]) * 100))" 
                    :status="item.status === 2 ? 'success' : (isLocked(item.subject) ? 'normal' : 'active')"
                    :stroke-color="isLocked(item.subject) ? '#d9d9d9' : ''"
                    :show-info="false"
                  />
                </div>
                
                <!-- 理论科目显示提示文本 -->
                <div class="theory-hint" v-else>
                  <a-alert message="不计学时，直接联系预约考试" type="info" ghost />
                </div>

                <div class="exam-info" v-if="item.latestScore !== null">
                  <span class="exam-label">最近成绩:</span>
                  <span class="exam-score" :class="item.latestScore >= 90 ? 'pass' : 'fail'">{{ item.latestScore }} 分</span>
                </div>
                <div class="exam-info" v-else-if="item.status === 1">
                  <span class="exam-label">考试状态:</span>
                  <span class="exam-status-hint">学时已达标，可预约考试</span>
                </div>
              </div>
              
              <div class="lock-overlay" v-if="isLocked(item.subject)">
                <div class="lock-content">
                  <span class="lock-icon">🔒</span>
                  <p>前一科目通过后解锁</p>
                </div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>
    </a-card>

    <!-- 训练记录时间线 -->
    <a-card title="🚗 最近训练记录" :bordered="false" style="margin-top: 24px;" class="record-card">
      <a-empty v-if="records.length === 0" description="暂无训练记录" />
      <div class="timeline-container" v-else>
        <a-timeline mode="alternate">
          <a-timeline-item v-for="record in records" :key="record.id" :color="record.hours >= 2 ? 'green' : 'blue'">
            <template #dot>
              <div class="custom-dot"><div class="dot-inner"></div></div>
            </template>
            <div class="record-item">
              <div class="record-date">{{ formatDate(record.trainingDate) }}</div>
              <div class="record-header">
                <span class="record-subject">科目 {{ subjectMap[record.subject] }} 训练</span>
                <span class="record-hours">+{{ record.hours }}h</span>
              </div>
              <div class="record-content">{{ record.content || '常规基础驾驶训练' }}</div>
            </div>
          </a-timeline-item>
        </a-timeline>
      </div>
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
  1: { text: '学时达标', color: 'orange' },
  2: { text: '已通过', color: 'green' }
}

const getSubjectSubtitle = (subject: number) => {
  const subtitles: any = { 1: '理论知识', 2: '场地驾驶', 3: '道路驾驶', 4: '文明驾驶' }
  return subtitles[subject]
}

const isLocked = (subject: number) => {
  if (subject === 1) return false
  const prevSubject = progressList.value.find(p => p.subject === subject - 1)
  return !prevSubject || prevSubject.status !== 2
}

// 计算当前进行到哪一步 (0-3)
const currentStep = computed(() => {
  // 查找第一个未通过的科目索引
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
    // 确保按科目排序
    progressList.value = (res.data || []).sort((a: any, b: any) => a.subject - b.subject)
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
  max-width: 1200px;
  margin: 0 auto;
}
.main-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}
.level-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}
.level-badge .label {
  color: #8c8c8c;
  font-size: 13px;
}
.steps-wrapper {
  padding: 32px 0 12px;
}

.subject-card {
  position: relative;
  background: #fff;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
  height: 100%;
}
.subject-card:hover {
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);
}

.subject-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.subject-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  display: block;
}
.subject-subtitle {
  font-size: 12px;
  color: #8c8c8c;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 8px;
}
.progress-info b {
  font-size: 18px;
  color: #1890ff;
}
.percent {
  font-weight: 600;
  color: #262626;
}

.exam-info {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.exam-label {
  color: #8c8c8c;
  font-size: 13px;
}
.exam-score {
  font-size: 18px;
  font-weight: 700;
}
.exam-score.pass { color: #52c41a; }
.exam-score.fail { color: #ff4d4f; }
.exam-status-hint {
  color: #fa8c16;
  font-size: 13px;
  font-weight: 500;
}

.theory-hint {
  margin-bottom: 24px;
}
.theory-hint :deep(.ant-alert) {
  padding: 8px 12px;
  background-color: #f0f5ff;
  border: 1px solid #adc6ff;
}
.theory-hint :deep(.ant-alert-message) {
  font-size: 13px;
  color: #1d39c4;
}

/* 锁定状态样式 */
.card-locked {
  background: #fafafa;
  filter: grayscale(0.8);
  opacity: 0.8;
}
.lock-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  z-index: 10;
}
.lock-content {
  text-align: center;
  color: #bfbfbf;
}
.lock-icon {
  font-size: 24px;
  margin-bottom: 8px;
  display: block;
}

/* 时间线样式 */
.record-card {
  border-radius: 12px;
}
.timeline-container {
  padding: 24px 0;
}
.custom-dot {
  width: 20px;
  height: 20px;
  background: #e6f7ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dot-inner {
  width: 8px;
  height: 8px;
  background: #1890ff;
  border-radius: 50%;
}
.record-item {
  background: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
  text-align: left;
}
.record-date {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}
.record-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.record-subject {
  font-weight: 600;
  color: #262626;
}
.record-hours {
  color: #1890ff;
  font-weight: 700;
}
.record-content {
  color: #595959;
  font-size: 13px;
  line-height: 1.5;
}
</style>
