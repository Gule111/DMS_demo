<template>
  <div class="hour-manage-container">
    <div class="header-section">
      <div class="welcome-text">
        <h2>教练端工作台</h2>
        <p>欢迎回来，{{ currentCoach?.realName || '教练' }}。今天也要加油教学哦！</p>
      </div>
      <a-button type="primary" ghost @click="fetchInitialData">
        <template #icon><ReloadOutlined /></template>
        刷新数据
      </a-button>
    </div>

    <a-tabs v-model:activeKey="activeTab" class="custom-tabs" type="card" size="large" centered animated>
      
      <!-- 模块 1：学员名册 -->
      <a-tab-pane key="roster" tab="学员名册">
        <div class="tab-card">
          <div class="search-bar">
            <a-input-search v-model:value="rosterSearch" placeholder="搜索学员姓名/手机号" style="width: 100%" @search="onSearch" />
          </div>
          <a-list :loading="loading" :data-source="filteredStudents" class="student-list">
            <template #renderItem="{ item }">
              <a-list-item class="student-item">
                <a-list-item-meta :title="item.realName" :description="item.phone">
                  <template #avatar>
                    <a-avatar :size="48" :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${item.realName}`" />
                  </template>
                </a-list-item-meta>
                <div class="info-group">
                  <a-tag color="blue">{{ item.licenseType }}</a-tag>
                  <a-tag :color="item.status === 2 ? 'orange' : 'green'">{{ getStatusText(item.status) }}</a-tag>
                </div>
                <template #actions>
                  <a :href="'tel:' + item.phone" class="action-btn tel"><PhoneOutlined /></a>
                </template>
              </a-list-item>
            </template>
          </a-list>
          <a-empty v-if="filteredStudents.length === 0" description="没有找到学员" />
        </div>
      </a-tab-pane>

      <!-- 模块 2：进度录入 -->
      <a-tab-pane key="progress" tab="进度录入">
        <div class="tab-card">
          <div class="quick-grid">
            <div v-for="student in students" :key="student.id" class="entry-card" @click="openRecordModal(student)">
              <a-avatar :size="56" :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${student.realName}`" />
              <div class="name">{{ student.realName }}</div>
              <div class="sub">录入学时</div>
            </div>
          </div>
          <a-empty v-if="students.length === 0" description="暂无带教学员" />
        </div>
      </a-tab-pane>

      <!-- 模块 3：约课日程 -->
      <a-tab-pane key="schedule" tab="约课日程">
        <div class="tab-card">
          <div class="date-picker-mini">
            <a-radio-group v-model:value="scheduleDate" button-style="solid" @change="fetchSchedule">
              <a-radio-button :value="todayStr">今日</a-radio-button>
              <a-radio-button :value="tomorrowStr">明日</a-radio-button>
              <a-radio-button :value="dayAfterTomorrowStr">后天</a-radio-button>
            </a-radio-group>
          </div>
          <div class="slot-container">
            <div v-for="slot in timeSlots" :key="slot" class="slot-row" :class="{ busy: isSlotBusy(slot) }">
              <span class="time">{{ slot }}</span>
              <span class="status">{{ isSlotBusy(slot) ? '已忙碌/有约' : '空闲可约' }}</span>
              <a-switch :checked="!isSlotBusy(slot)" size="small" @change="(val: boolean) => toggleSlot(scheduleDate, slot, !val)" />
            </div>
          </div>
        </div>
      </a-tab-pane>

      <!-- 模块 4：成绩反馈 -->
      <a-tab-pane key="feedback" tab="成绩反馈">
        <div class="tab-card feedback-form">
          <a-form layout="vertical">
            <a-form-item label="选择学员" required>
              <a-select v-model:value="examForm.studentId" placeholder="点击选择学员">
                <a-select-option v-for="s in students" :key="s.id" :value="s.id">{{ s.realName }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="科目" required>
                  <a-select v-model:value="examForm.subject">
                    <a-select-option :value="1">科目一</a-select-option>
                    <a-select-option :value="2">科目二</a-select-option>
                    <a-select-option :value="3">科目三</a-select-option>
                    <a-select-option :value="4">科目四</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="得分" required>
                  <a-input-number v-model:value="examForm.score" :min="0" :max="100" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="评价/备注">
              <a-textarea v-model:value="examForm.remark" placeholder="录入简单的考后评价" :rows="3" />
            </a-form-item>
            <a-button type="primary" block size="large" @click="handleExamSubmit" :loading="examLoading">提交反馈并同步进度</a-button>
          </a-form>
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- 录入学时 Modal -->
    <a-modal v-model:open="recordVisible" :title="'录入学时 - ' + currentStudent?.realName" @ok="handleRecordSubmit" :confirmLoading="recordLoading">
      <a-form layout="vertical">
        <a-form-item label="训练内容" required>
          <a-checkbox-group v-model:value="selectedItems" class="checkbox-grid">
            <a-checkbox value="倒车入库">倒车入库</a-checkbox>
            <a-checkbox value="侧方停车">侧方停车</a-checkbox>
            <a-checkbox value="曲线行驶">曲线行驶</a-checkbox>
            <a-checkbox value="直角转弯">直角转弯</a-checkbox>
            <a-checkbox value="坡道起步">坡道起步</a-checkbox>
            <a-checkbox value="模拟考试">模拟考试</a-checkbox>
          </a-checkbox-group>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="学时数" required>
              <a-select v-model:value="recordForm.hours">
                <a-select-option :value="1">1 学时</a-select-option>
                <a-select-option :value="2">2 学时</a-select-option>
                <a-select-option :value="4">4 学时</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
             <a-form-item label="训练科目" required>
              <a-select v-model:value="recordForm.subject">
                <a-select-option :value="2">科目二</a-select-option>
                <a-select-option :value="3">科目三</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ReloadOutlined, PhoneOutlined, UserOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const activeTab = ref('roster')

// 建立路由与 Tab 的映射关系
const routeToTabMap: Record<string, string> = {
  'roster': 'roster',
  'progress-entry': 'progress',
  'schedule': 'schedule',
  'feedback': 'feedback'
}

const tabToRouteMap: Record<string, string> = {
  'roster': 'roster',
  'progress': 'progress-entry',
  'schedule': 'schedule',
  'feedback': 'feedback'
}

// 监听路由变化，同步 Tab
watch(() => route.path, (newPath) => {
  const key = newPath.split('/').pop() || ''
  if (routeToTabMap[key]) {
    activeTab.value = routeToTabMap[key]
  }
}, { immediate: true })

// 监听 Tab 变化，同步路由 (让侧边栏高亮正确)
watch(activeTab, (newTab) => {
  const targetRoute = tabToRouteMap[newTab]
  if (targetRoute && !route.path.endsWith(targetRoute)) {
    router.push('/app/' + targetRoute)
  }
})

const loading = ref(false)
const students = ref<any[]>([])
const currentCoach = ref<any>(null)
const rosterSearch = ref('')

const filteredStudents = computed(() => {
  if (!rosterSearch.value) return students.value
  return students.value.filter(s => 
    s.realName.includes(rosterSearch.value) || s.phone.includes(rosterSearch.value)
  )
})

const getStatusText = (status: number) => {
  const map: any = { 0: '未报名', 1: '审核中', 2: '学习中', 3: '已拿证' }
  return map[status] || '未知'
}

const fetchInitialData = async () => {
  loading.value = true
  try {
    // 获取教练本人信息
    const coachRes: any = await request.get('/instructor/current')
    if (coachRes.data) {
      currentCoach.value = coachRes.data
      // 获取名下学员
      const studentRes: any = await request.get('/instructor/students/' + coachRes.data.id)
      students.value = studentRes.data || []
      fetchSchedule()
    }
  } catch (err: any) {
    message.error(err.response?.data?.message || '加载数据失败')
  } finally {
    loading.value = false
  }
}

// === 约课日程 ===
const todayStr = dayjs().format('YYYY-MM-DD')
const tomorrowStr = dayjs().add(1, 'day').format('YYYY-MM-DD')
const dayAfterTomorrowStr = dayjs().add(2, 'day').format('YYYY-MM-DD')
const scheduleDate = ref(todayStr)
const timeSlots = ['08:00-10:00', '10:00-12:00', '14:00-16:00', '16:00-18:00', '19:00-21:00']
const busySlots = ref<Record<string, boolean>>({})

const fetchSchedule = async () => {
  try {
    const res: any = await request.get('/schedule/my', {
      params: { startDate: scheduleDate.value, endDate: scheduleDate.value }
    })
    const map: Record<string, boolean> = {}
    res.data?.forEach((item: any) => {
      map[item.timeSlot] = item.isBusy === 1
    })
    busySlots.value = map
  } catch (err) {}
}

const isSlotBusy = (slot: string) => busySlots.value[slot] || false

const toggleSlot = async (day: string, slot: string, busy: boolean) => {
  try {
    await request.post('/schedule/toggle', null, {
      params: { date: day, timeSlot: slot, isBusy: busy }
    })
    message.success('更新成功')
    fetchSchedule()
  } catch (err) {
    message.error('设置失败')
  }
}

// === 进度录入 ===
const recordVisible = ref(false)
const recordLoading = ref(false)
const currentStudent = ref<any>(null)
const selectedItems = ref([])
const recordForm = reactive({
  subject: 2,
  hours: 2,
  content: ''
})

const openRecordModal = (student: any) => {
  currentStudent.value = student
  selectedItems.value = []
  recordVisible.value = true
}

const handleRecordSubmit = async () => {
  if (selectedItems.value.length === 0) {
    message.warning('请勾选练车内容')
    return
  }
  recordLoading.value = true
  try {
    await request.post('/progress/record', null, {
      params: {
        studentId: currentStudent.value.id,
        subject: recordForm.subject,
        hours: recordForm.hours,
        content: selectedItems.value.join('、')
      }
    })
    message.success('录入成功')
    recordVisible.value = false
  } catch (err) {
    message.error('录入失败')
  } finally {
    recordLoading.value = false
  }
}

// === 成绩反馈 ===
const examLoading = ref(false)
const examForm = reactive({
  studentId: undefined,
  subject: 2,
  score: 90,
  remark: ''
})

const handleExamSubmit = async () => {
  if (!examForm.studentId) {
    message.warning('请选择学员')
    return
  }
  examLoading.value = true
  try {
    await request.post('/progress/exam-result', null, { params: examForm })
    message.success('反馈成功，已同步学员进度')
    examForm.studentId = undefined
    examForm.remark = ''
  } catch (err) {
    message.error('操作失败')
  } finally {
    examLoading.value = false
  }
}

onMounted(() => {
  fetchInitialData()
})
</script>

<style scoped>
.hour-manage-container {
  padding: 16px;
  max-width: 800px;
  margin: 0 auto;
  background: #f8fafc;
  min-height: 100vh;
}
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 8px;
}
.welcome-text h2 {
  margin: 0;
  font-size: 22px;
  color: #1e293b;
}
.welcome-text p {
  margin: 4px 0 0;
  color: #64748b;
}

.tab-card {
  background: #fff;
  padding: 20px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  min-height: 400px;
}

.search-bar {
  margin-bottom: 16px;
}

.student-list {
  margin-top: 8px;
}
.student-item {
  padding: 12px 0;
}
.info-group {
  display: flex;
  gap: 8px;
  margin-right: 12px;
}
.action-btn.tel {
  font-size: 20px;
  color: #3b82f6;
}

/* 快速录入网格 */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.entry-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  background: #f1f5f9;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.entry-card:hover {
  background: #e2e8f0;
  transform: translateY(-2px);
}
.entry-card .name {
  margin-top: 8px;
  font-weight: 600;
  font-size: 15px;
}
.entry-card .sub {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

/* 约课日程 */
.date-picker-mini {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}
.slot-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.slot-row {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}
.slot-row.busy {
  background: #fff1f0;
  border-color: #ffa39e;
}
.slot-row .time {
  font-weight: 600;
  width: 100px;
}
.slot-row .status {
  flex: 1;
  font-size: 13px;
  color: #64748b;
}

/* 弹窗样式 */
.checkbox-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.feedback-form {
  max-width: 500px;
  margin: 0 auto;
}

:deep(.ant-tabs-nav) {
  margin-bottom: 16px !important;
}
:deep(.ant-tabs-tab) {
  padding: 10px 20px !important;
  border-radius: 8px 8px 0 0 !important;
}
</style>
