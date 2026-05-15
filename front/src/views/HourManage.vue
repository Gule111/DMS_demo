<template>
  <div class="workbench-container">
    <!-- 顶部状态条：不再显示选项卡，仅显示当前模块标题 -->
    <div class="page-header">
      <div class="title-group">
        <h2>{{ currentModuleTitle }}</h2>
        <p v-if="currentCoach">{{ currentCoach.realName }} 教练，这是您的{{ currentModuleTitle }}模块</p>
      </div>
      <a-button type="primary" ghost @click="fetchInitialData">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </div>

    <!-- 模块 1：学员名册 (仅在 /app/roster 时显示) -->
    <div v-if="activeTab === 'roster'" class="module-card">
      <div class="search-bar">
        <a-input-search v-model:value="rosterSearch" placeholder="搜索学员姓名/手机号" style="width: 100%" />
      </div>
      <a-list :loading="loading" :data-source="filteredStudents" class="data-list">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="item.realName" :description="item.phone">
              <template #avatar>
                <a-avatar :size="48" :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${item.realName}`" />
              </template>
            </a-list-item-meta>
            <div class="tag-group">
              <a-tag color="blue">{{ item.licenseType }}</a-tag>
              <a-tag :color="item.status === 2 ? 'orange' : 'green'">{{ getStatusText(item.status) }}</a-tag>
            </div>
            <template #actions>
              <a :href="'tel:' + item.phone" class="action-btn tel"><PhoneOutlined /></a>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </div>

    <!-- 模块 2：进度录入 (仅在 /app/progress-entry 时显示) -->
    <div v-if="activeTab === 'progress'" class="module-card">
      <div class="quick-grid">
        <div v-for="student in students" :key="student.id" class="entry-card" @click="openRecordModal(student)">
          <a-avatar :size="56" :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${student.realName}`" />
          <div class="name">{{ student.realName }}</div>
          <div class="sub">录入学时</div>
        </div>
      </div>
      <a-empty v-if="students.length === 0" description="暂无带教学员" />
    </div>

    <!-- 模块 3：约课日程 (仅在 /app/schedule 时显示) -->
    <div v-if="activeTab === 'schedule'" class="module-card">
      <div class="alert-info">学员提交预约后，请在这里及时确认或拒绝</div>
      <a-table :data-source="appointments" :columns="appointmentColumns" :loading="loading" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getAppointStatusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <div class="action-row" v-if="record.status === 1">
              <a-button type="primary" size="small" @click="handleAppoint(record.id, 2)">接受</a-button>
              <a-button type="primary" danger size="small" @click="handleAppoint(record.id, 3)">拒绝</a-button>
            </div>
            <a-button v-else-if="record.status === 2" type="link" size="small" @click="openCompleteModal(record)">确认完成</a-button>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 模块 4：成绩反馈 (仅在 /app/feedback 时显示) -->
    <div v-if="activeTab === 'feedback'" class="module-card feedback-form-wrap">
      <a-form layout="vertical" class="form-container">
        <a-form-item label="选择学员" required>
          <a-select v-model:value="examForm.studentId" placeholder="选择学员">
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
        <a-button type="primary" block size="large" @click="handleExamSubmit" :loading="examLoading">提交反馈</a-button>
      </a-form>
    </div>

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
import { ReloadOutlined, PhoneOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const activeTab = ref('roster')
const loading = ref(false)
const students = ref<any[]>([])
const appointments = ref<any[]>([])
const currentCoach = ref<any>(null)
const rosterSearch = ref('')

const currentModuleTitle = computed(() => {
  const map: any = { roster: '学员名册', progress: '进度录入', schedule: '约课日程', feedback: '成绩反馈' }
  return map[activeTab.value] || '教练工作台'
})

const fetchInitialData = async () => {
  loading.value = true
  try {
    const coachRes: any = await request.get('/instructor/current')
    if (!coachRes || !coachRes.data) {
      message.warning('未找到教练信息，请确保已分配教练身份')
      loading.value = false
      return
    }
    
    currentCoach.value = coachRes.data
    const coachId = coachRes.data.id

    if (activeTab.value === 'roster' || activeTab.value === 'progress' || activeTab.value === 'feedback') {
      const studentRes: any = await request.get('/instructor/students/' + coachId)
      students.value = Array.isArray(studentRes.data) ? studentRes.data : []
    } else if (activeTab.value === 'schedule') {
      const appointRes: any = await request.get('/appointment/instructor/list')
      const studentRes: any = await request.get('/instructor/students/' + coachId)
      
      const studentData = Array.isArray(studentRes.data) ? studentRes.data : []
      const studentMap = new Map(studentData.map((s: any) => [s.id, s.realName]))
      
      const appointData = (Array.isArray(appointRes.data) ? appointRes.data : []).filter(Boolean)
      appointments.value = appointData.map((a: any) => ({
        ...a,
        studentName: studentMap.get(a.studentId) || '未知学员'
      }))
    }
  } catch (err: any) {
    console.error('[HourManage] Fetch error:', err)
    message.error(err.response?.data?.message || err.message || '数据加载失败')
  } finally {
    loading.value = false
  }
}

// 路由监听
watch(() => route.path, (path) => {
  if (!path) return
  const key = path.split('/').pop() || ''
  const routeToTabMap: any = { roster: 'roster', 'progress-entry': 'progress', schedule: 'schedule', feedback: 'feedback' }
  if (routeToTabMap[key]) {
    activeTab.value = routeToTabMap[key]
    fetchInitialData()
  }
}, { immediate: true })

// 约课逻辑
const appointmentColumns = [
  { title: '学员', dataIndex: 'studentName', key: 'studentName' },
  { title: '日期', dataIndex: 'appointmentDate', key: 'appointmentDate' },
  { title: '时段', dataIndex: 'timeSlot', key: 'timeSlot' },
  { title: '科目', dataIndex: 'subject', key: 'subject', customRender: ({ text }: any) => '科目' + text },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action' }
]

const getAppointStatusText = (s: number) => {
  const map: any = { 1: '待审核', 2: '已预约', 3: '已拒绝', 4: '已完成' }
  return map[s] || '未知'
}
const getStatusColor = (s: number) => {
  const map: any = { 1: 'orange', 2: 'green', 3: 'red', 4: 'blue' }
  return map[s] || 'default'
}

const handleAppoint = async (id: number, status: number) => {
  try {
    await request.post('/appointment/handle', null, { params: { id, status } })
    message.success('处理成功')
    fetchInitialData()
  } catch (err) {
    message.error('处理失败')
  }
}

const openCompleteModal = (record: any) => {
  handleAppoint(record.id, 4)
}

// 名册逻辑
const filteredStudents = computed(() => {
  const list = (Array.isArray(students.value) ? students.value : []).filter(Boolean)
  if (!rosterSearch.value) return list
  return list.filter(s => s && (s.realName?.includes(rosterSearch.value) || s.phone?.includes(rosterSearch.value)))
})
const getStatusText = (status: number) => {
  const map: any = { 0: '未报名', 1: '审核中', 2: '学习中', 3: '已拿证' }
  return map[status] || '未知'
}

// 进度录入逻辑 (Modal)
const recordVisible = ref(false)
const recordLoading = ref(false)
const currentStudent = ref<any>(null)
const selectedItems = ref([])
const recordForm = reactive({ subject: 2, hours: 2 })

const openRecordModal = (student: any) => {
  currentStudent.value = student
  recordVisible.value = true
}

const handleRecordSubmit = async () => {
  if (selectedItems.value.length === 0) return message.warning('请勾选练车内容')
  recordLoading.value = true
  try {
    await request.post('/progress/record', null, {
      params: { studentId: currentStudent.value.id, subject: recordForm.subject, hours: recordForm.hours, content: selectedItems.value.join('、') }
    })
    message.success('录入成功')
    recordVisible.value = false
  } catch (err) {
    message.error('录入失败')
  } finally {
    recordLoading.value = false
  }
}

// 成绩反馈逻辑
const examLoading = ref(false)
const examForm = reactive({ studentId: undefined, subject: 2, score: 90, remark: '' })
const handleExamSubmit = async () => {
  if (!examForm.studentId) return message.warning('请选择学员')
  examLoading.value = true
  try {
    await request.post('/progress/exam-result', null, { params: examForm })
    message.success('反馈成功')
    examForm.studentId = undefined
  } catch (err) {
    message.error('操作失败')
  } finally {
    examLoading.value = false
  }
}

</script>

<style scoped>
.workbench-container {
  padding: 24px;
  max-width: 1100px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}
.title-group h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}
.title-group p {
  color: #8c8c8c;
  margin: 4px 0 0;
}

.module-card {
  background: #fff;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.05);
  min-height: 500px;
}

.alert-info {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 24px;
  color: #0050b3;
}

.action-row {
  display: flex;
  gap: 8px;
}

.search-bar {
  margin-bottom: 24px;
}

.entry-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  background: #f5f5f5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}
.entry-card:hover {
  background: #e6f7ff;
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.entry-card .name {
  margin-top: 12px;
  font-weight: 600;
  font-size: 16px;
}
.entry-card .sub {
  font-size: 13px;
  color: #8c8c8c;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 20px;
}

.feedback-form-wrap {
  display: flex;
  justify-content: center;
}
.form-container {
  width: 100%;
  max-width: 500px;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
