`<template>
  <div class="coach-container">
    <div class="header-section">
      <a-page-header 
        :title="userStore.role === 1 ? '教练分配管理' : '我的工作台'" 
        :sub-title="userStore.role === 1 ? '智能匹配与手动调度学员教练' : '欢迎回来，' + (currentCoach?.realName || '教练')"
      >
        <template #extra>
          <a-button key="1" type="primary" ghost @click="fetchInitialData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </template>
      </a-page-header>
    </div>

    <!-- 管理员视图：教练列表 (保持原有逻辑) -->
    <a-card :bordered="false" class="main-card admin-view" v-if="userStore.role === 1">
      <div style="margin-bottom: 16px; display: flex; justify-content: space-between;">
        <a-input-search placeholder="搜索教练姓名" style="width: 250px" />
        <div>
          <a-button type="primary" @click="handleAdd" style="margin-right: 8px;">新增教练</a-button>
        </div>
      </div>

      <a-table :dataSource="instructors" :columns="columns" rowKey="id" :loading="loading" bordered>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'teachType'">
            <a-tag :color="record.teachType === 'C1' ? 'blue' : 'green'">{{ record.teachType }}</a-tag>
          </template>
          <template v-if="column.key === 'currentLoad'">
            <a-badge 
              :status="getLoadStatus(record.currentLoad)" 
              :text="record.currentLoad + ' 人'" 
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleViewStudents(record)">查看学员</a-button>
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="link" size="small" danger @click="confirmDelete(record)">删除</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 教练视图：四大核心模块 -->
    <div v-else-if="userStore.role === 2" class="coach-workplace">
      <a-tabs v-model:activeKey="activeTab" class="custom-tabs" type="card" size="large" centered animated>
        
        <!-- 模块 1：学员名册 -->
        <a-tab-pane key="roster" tab="学员名册">
          <a-card class="module-card">
            <div class="search-bar">
              <a-input-search v-model:value="rosterSearch" placeholder="搜索学员姓名/电话" />
            </div>
            <a-list :loading="studentsLoading" :data-source="filteredStudents" :pagination="{ pageSize: 10 }">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta :title="item.realName" :description="item.phone">
                    <template #avatar>
                      <a-avatar :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${item.realName}`" />
                    </template>
                  </a-list-item-meta>
                  <div class="student-info-tags">
                    <a-tag color="blue">{{ item.licenseType }}</a-tag>
                    <a-tag :color="item.status === 2 ? 'orange' : 'green'">{{ getStudentStatusText(item.status) }}</a-tag>
                  </div>
                  <template #actions>
                    <a :href="'tel:' + item.phone"><PhoneOutlined /> 拨打</a>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-tab-pane>

        <!-- 模块 2：进度录入 -->
        <a-tab-pane key="progress" tab="学时管理">
          <a-card class="module-card">
            <div class="quick-entry-grid">
              <div v-for="student in coachStudents" :key="student.id" class="student-entry-card" @click="handleOpenRecord(student)">
                <a-avatar :size="64" :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${student.realName}`" />
                <div class="name">{{ student.realName }}</div>
                <div class="sub">{{ student.licenseType }}</div>
              </div>
            </div>
            <a-empty v-if="coachStudents.length === 0" description="暂无带教学员" />
          </a-card>
        </a-tab-pane>

        <!-- 模块 3：约课日程 -->
        <a-tab-pane key="schedule" tab="约课日程">
          <a-card class="module-card">
            <div class="date-selector">
              <a-radio-group v-model:value="scheduleDate" button-style="solid" @change="fetchSchedule">
                <a-radio-button :value="todayStr">今日 ({{ todayStr }})</a-radio-button>
                <a-radio-button :value="tomorrowStr">明日 ({{ tomorrowStr }})</a-radio-button>
              </a-radio-group>
            </div>
            <div class="slot-list">
              <div v-for="slot in timeSlots" :key="slot" class="slot-item" :class="{ busy: isSlotBusy(slot) }">
                <div class="time">{{ slot }}</div>
                <div class="status-tag">
                  <a-tag :color="isSlotBusy(slot) ? 'error' : 'success'">{{ isSlotBusy(slot) ? '已忙碌/已约' : '可预约' }}</a-tag>
                </div>
                <div class="action">
                  <a-switch :checked="!isSlotBusy(slot)" checked-children="空闲" un-checked-children="忙碌" @change="(val: boolean) => toggleSlotBusy(slot, !val)" />
                </div>
              </div>
            </div>
          </a-card>
        </a-tab-pane>

      </a-tabs>
    </div>

    <!-- 弹窗/抽屉组件 -->
    <a-modal v-model:open="recordVisible" :title="'录入学时 - ' + currentStudent?.realName" @ok="submitRecord" :confirmLoading="recordLoading">
      <a-form layout="vertical">
        <a-form-item label="训练科目" required>
          <a-radio-group v-model:value="recordForm.subject">
            <a-radio-button :value="1">科目一</a-radio-button>
            <a-radio-button :value="2" :disabled="!isSub1Passed">科目二</a-radio-button>
            <a-radio-button :value="3" :disabled="!isSub1Passed">科目三</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="训练时长 (小时)" required>
          <a-input-number v-model:value="recordForm.hours" :min="0.5" :max="4" :step="0.5" style="width: 100%" />
        </a-form-item>
        <a-form-item label="今日训练内容">
          <a-checkbox-group v-model:value="recordForm.contentList">
            <a-row :gutter="[16, 8]">
              <a-col :span="12" v-for="opt in currentSubjectOptions" :key="opt">
                <a-checkbox :value="opt">{{ opt }}</a-checkbox>
              </a-col>
            </a-row>
          </a-checkbox-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 原有管理员用 Modal/Drawer 保持不变 -->
    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑教练' : '新增教练'" @ok="submitForm">
       <a-form layout="vertical">
        <a-form-item label="教练姓名" required>
          <a-input v-model:value="formData.realName" />
        </a-form-item>
        <a-form-item label="联系电话" required>
          <a-input v-model:value="formData.phone" />
        </a-form-item>
        <a-form-item label="准教车型" required>
          <a-radio-group v-model:value="formData.teachType">
            <a-radio-button v-for="item in licenseTypes" :key="item.id" :value="item.dictCode">
              {{ item.dictValue }}
            </a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="drawerVisible" :title="currentCoach?.realName + ' 的学员列表'" width="450">
       <a-table :dataSource="coachStudents" :columns="studentColumns" rowKey="id" size="small" :pagination="false" />
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed, createVNode, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, PhoneOutlined, ExclamationCircleOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
const activeTab = ref('roster')

// === 基础数据 ===
const loading = ref(false)
const studentsLoading = ref(false)
const instructors = ref([])
const coachStudents = ref<any[]>([])
const currentCoach = ref<any>(null)
const currentStudent = ref<any>(null)

// === 学员名册模块 ===
const rosterSearch = ref('')
const filteredStudents = computed(() => {
  if (!rosterSearch.value) return coachStudents.value
  return coachStudents.value.filter(s => 
    s.realName?.includes(rosterSearch.value) || s.phone?.includes(rosterSearch.value)
  )
})
const getStudentStatusText = (status: number) => {
  const map: any = { 0: '未报名', 1: '审核中', 2: '学习中', 3: '已拿证' }
  return map[status] || '未知'
}

// === 学时管理模块 ===
const recordVisible = ref(false)
const recordLoading = ref(false)
const recordForm = reactive({
  subject: 2,
  hours: 2.0,
  contentList: [] as string[]
})

// 各个科目对应的训练内容选项
const subjectOneOptions = [
  '交通法规理论学习',
  '安全文明驾驶常识',
  '理论模拟考试练习',
  '易错题专项突破'
]

const subjectTwoOptions = [
  '倒车入库',
  '侧方停车',
  '坡道定点停车与起步',
  '直角转弯',
  '曲线行驶',
  '场地模拟考试'
]

const subjectThreeOptions = [
  '上车准备与起步',
  '直线行驶与加减挡',
  '变更车道与超车',
  '通过路口与学校区域',
  '掉头与会车',
  '靠边停车',
  '模拟夜间灯光使用',
  '道路模拟考试'
]

// 动态获取当前选中的科目训练内容
const currentSubjectOptions = computed(() => {
  if (recordForm.subject === 1) return subjectOneOptions
  if (recordForm.subject === 2) return subjectTwoOptions
  if (recordForm.subject === 3) return subjectThreeOptions
  return []
})

// 当切换科目时，自动清空已选的训练项目列表
watch(() => recordForm.subject, () => {
  recordForm.contentList = []
})
const isSub1Passed = ref(false)

const handleOpenRecord = async (student: any) => {
  currentStudent.value = student
  recordForm.contentList = []
  recordForm.subject = 1
  isSub1Passed.value = false
  
  try {
    const res: any = await request.get('/progress/student/' + student.id)
    const progressList = res.data || []
    const sub1 = progressList.find((p: any) => p.subject === 1)
    if (sub1 && sub1.status === 2) {
      isSub1Passed.value = true
      recordForm.subject = 2
    }
  } catch (err) {
    console.error('获取学员科目一进度失败:', err)
  }
  
  recordForm.hours = 2.0
  recordVisible.value = true
}
const submitRecord = async () => {
  if (recordForm.contentList.length === 0) {
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
        content: recordForm.contentList.join(', ')
      }
    })
    message.success('一键确认学时成功')
    recordVisible.value = false
  } catch (err: any) {
    message.error(err.response?.data?.message || '录入失败')
  } finally {
    recordLoading.value = false
  }
}

// === 约课日程模块 ===
const todayStr = dayjs().format('YYYY-MM-DD')
const tomorrowStr = dayjs().add(1, 'day').format('YYYY-MM-DD')
const scheduleDate = ref(todayStr)
const mySchedule = ref<any[]>([])
const timeSlots = ['08:00-10:00', '10:00-12:00', '14:00-16:00', '16:00-18:00', '19:00-21:00']

const fetchSchedule = async () => {
  try {
    const res: any = await request.get('/schedule/my', {
      params: { startDate: scheduleDate.value, endDate: scheduleDate.value }
    })
    mySchedule.value = res.data || []
  } catch (err) {
    console.error(err)
  }
}
const isSlotBusy = (slot: string) => {
  return mySchedule.value.some(s => s.timeSlot === slot && s.isBusy === 1)
}
const toggleSlotBusy = async (slot: string, busy: boolean) => {
  try {
    await request.post('/schedule/toggle', null, {
      params: { date: scheduleDate.value, timeSlot: slot, isBusy: busy }
    })
    message.success('状态已更新')
    fetchSchedule()
  } catch (err) {
    message.error('更新失败')
  }
}


// === 原有逻辑兼容 ===
const columns = [
  { title: '教练ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '教练姓名', dataIndex: 'realName', key: 'realName' },
  { title: '联系电话', dataIndex: 'phone', key: 'phone' },
  { title: '准教车型', dataIndex: 'teachType', key: 'teachType' },
  { title: '当前带教学员', dataIndex: 'currentLoad', key: 'currentLoad' },
  { title: '操作', key: 'action', width: 220, align: 'center' }
]
const studentColumns = [
  { title: '姓名', dataIndex: 'realName', key: 'realName' },
  { title: '车型', dataIndex: 'licenseType', key: 'licenseType' },
  { title: '电话', dataIndex: 'phone', key: 'phone' }
]

const fetchInitialData = async () => {
  if (userStore.role === 1) {
    loading.value = true
    try {
      const res: any = await request.get('/instructor/list')
      instructors.value = res.data || []
    } finally {
      loading.value = false
    }
  } else if (userStore.role === 2) {
    studentsLoading.value = true
    try {
      const coachRes: any = await request.get('/instructor/my')
      if (coachRes.data) {
        currentCoach.value = coachRes.data
        const studentRes: any = await request.get('/instructor/students/' + coachRes.data.id)
        coachStudents.value = studentRes.data || []
        fetchSchedule()
      }
    } finally {
      studentsLoading.value = false
    }
  }
}

// 管理员增删改逻辑 (简略保留)
const formVisible = ref(false)
const isEdit = ref(false)
const formData = reactive({ id: null, realName: '', phone: '', teachType: 'C1' })
const handleAdd = () => { isEdit.value = false; formData.id = null; formVisible.value = true }
const handleEdit = (record: any) => { isEdit.value = true; Object.assign(formData, record); formVisible.value = true }
const submitForm = async () => { 
  const api = isEdit.value ? '/instructor/update' : '/instructor/add'
  await request[isEdit.value ? 'put' : 'post'](api, formData)
  message.success('操作成功'); formVisible.value = false; fetchInitialData()
}
const handleDelete = async (id: number) => { 
  try {
    await request.delete('/instructor/delete/' + id)
    message.success('教练删除及清理操作已完成')
    fetchInitialData()
  } catch (err: any) {
    message.error(err.response?.data?.message || '删除失败')
  }
}

const confirmDelete = (record: any) => {
  Modal.confirm({
    title: `高危操作：确认删除教练【${record.realName}】吗？`,
    icon: createVNode(ExclamationCircleOutlined),
    content: createVNode('div', null, [
      createVNode('p', { style: 'color: #ff4d4f; font-weight: bold; margin-top: 10px;' }, '删除教练将引发以下连锁操作：'),
      createVNode('ol', { style: 'padding-left: 20px; line-height: 1.8;' }, [
        createVNode('li', null, '该教练的系统登录账号及角色权限将被永久注销。'),
        createVNode('li', null, '该教练名下的所有学员将被解绑，并由系统尝试自动重新分配给其他教练。'),
        createVNode('li', null, '请注意，此操作不可逆！')
      ])
    ]),
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await handleDelete(record.id)
    }
  })
}
const drawerVisible = ref(false)
const handleViewStudents = async (record: any) => {
  currentCoach.value = record; drawerVisible.value = true
  const res: any = await request.get('/instructor/students/' + record.id)
  coachStudents.value = res.data || []
}

const getLoadStatus = (load: number) => load >= 10 ? 'error' : load >= 5 ? 'warning' : 'success'

const licenseTypes = ref<any[]>([])
const fetchLicenseTypes = async () => {
  try {
    const res: any = await request.get('/dict/type/LICENSE_TYPE')
    licenseTypes.value = res.data || []
  } catch (err) {
    console.error('获取准教车型字典失败:', err)
  }
}

onMounted(() => {
  fetchInitialData()
  fetchLicenseTypes()
})
</script>

<style scoped>
.coach-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px;
  background: #f0f2f5;
  min-height: 100vh;
}
.header-section {
  background: #fff;
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.main-card {
  border-radius: 8px;
}
.coach-workplace {
  margin-top: 8px;
}
.module-card {
  border-radius: 12px;
  min-height: 500px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.search-bar {
  margin-bottom: 16px;
}
.student-info-tags {
  display: flex;
  gap: 8px;
  margin-right: 16px;
}

/* 进度录入网格 */
.quick-entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
  padding: 8px;
}
.student-entry-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}
.student-entry-card:hover {
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(24,144,255,0.15);
  transform: translateY(-2px);
}
.student-entry-card .name {
  margin-top: 8px;
  font-weight: bold;
  font-size: 16px;
}
.student-entry-card .sub {
  color: #8c8c8c;
  font-size: 12px;
}

/* 档期列表 */
.date-selector {
  margin-bottom: 20px;
  text-align: center;
}
.slot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.slot-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}
.slot-item.busy {
  background: #fff1f0;
  border-color: #ffa39e;
}
.slot-item .time {
  font-family: 'Courier New', Courier, monospace;
  font-weight: bold;
  font-size: 16px;
  width: 120px;
}
.slot-item .status-tag {
  flex: 1;
}

/* Tab 样式美化 */
:deep(.ant-tabs-nav) {
  margin-bottom: 16px !important;
}
:deep(.ant-tabs-tab) {
  padding: 12px 24px !important;
  font-size: 16px !important;
}
</style>
