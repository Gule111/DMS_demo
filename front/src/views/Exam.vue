<template>
  <div class="exam-manage-container">
    <a-page-header :title="userStore.role === 1 ? '考试管理' : '预约考场'" :sub-title="userStore.role === 1 ? '管理考试预约及录入成绩' : '模拟考试，正式考试预约入口'" />

    <a-card :bordered="false" class="main-card">
      <a-tabs v-model:activeKey="activeTab">
        <!-- 学员端标签页 -->
        <template v-if="userStore.role === 3">
          <a-tab-pane key="myExams" tab="我的预约/历史记录">
            <div class="table-operations">
              <a-button type="primary" @click="bookingVisible = true">发起新预约</a-button>
              <a-button @click="fetchMyExams" style="margin-left: 8px;">刷新</a-button>
            </div>
            <a-table :dataSource="myExams" :columns="studentColumns" rowKey="id" :loading="loading">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'examType'">
                  <a-tag :color="record.examType === 2 ? 'purple' : 'blue'">
                    {{ record.examType === 2 ? '模拟考试' : '正式考试' }}
                  </a-tag>
                </template>
                <template v-if="column.key === 'subject'">
                  科目 {{ subjectMap[record.subject] }}
                </template>
                <template v-if="column.key === 'examDate'">
                  {{ formatDate(record.examDate) }}
                </template>
                <template v-if="column.key === 'status'">
                  <a-tag :color="statusColorMap[record.status]">{{ statusTextMap[record.status] }}</a-tag>
                </template>
                <template v-if="column.key === 'score'">
                  <span v-if="record.score !== null" :class="record.score >= 90 ? 'pass' : 'fail'">
                    {{ record.score }}
                  </span>
                  <span v-else>-</span>
                </template>
                <template v-if="column.key === 'action'">
                  <a-popconfirm v-if="record.status === 0" title="确定要取消预约吗？" @confirm="cancelBooking(record.id)">
                    <a-button type="link" danger>取消预约</a-button>
                  </a-popconfirm>
                  <span v-else>-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </template>

        <!-- 管理员端标签页 -->
        <template v-if="userStore.role === 1">
          <a-tab-pane key="audit" tab="预约审批与考场分配">
            <a-table :dataSource="adminExams" :columns="adminColumns" rowKey="id" :loading="loading">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'examType'">
                  <a-tag :color="record.examType === 2 ? 'purple' : 'blue'">
                    {{ record.examType === 2 ? '模拟考试' : '正式考试' }}
                  </a-tag>
                </template>
                <template v-if="column.key === 'subject'">
                  科目 {{ subjectMap[record.subject] }}
                </template>
                <template v-if="column.key === 'examDate'">
                  {{ formatDate(record.examDate) }}
                </template>
                <template v-if="column.key === 'status'">
                  <a-tag :color="statusColorMap[record.status]">{{ statusTextMap[record.status] }}</a-tag>
                </template>
                <template v-if="column.key === 'action'">
                  <div v-if="record.status === 0">
                    <a-button type="link" @click="showAssignModal(record)">分配并批准</a-button>
                    <a-divider type="vertical" />
                    <a-button type="link" danger @click="handleAudit(record.id, 3)">拒绝</a-button>
                  </div>
                  <a-button v-else-if="record.status === 1" type="link" @click="showScoreModal(record)">
                    录入成绩
                  </a-button>
                  <span v-else>-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </template>
      </a-tabs>
    </a-card>

    <!-- 学员：预约考试弹窗 -->
    <a-modal v-model:open="bookingVisible" title="发起考试预约" @ok="submitBooking" :confirmLoading="submitting">
      <a-form :model="bookingForm" layout="vertical">
        <a-form-item label="考试类型" required>
          <a-radio-group v-model:value="bookingForm.examType">
            <a-radio :value="1">正式考试</a-radio>
            <a-radio :value="2">模拟考试</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="预约科目" required>
          <a-select v-model:value="bookingForm.subject">
            <a-select-option :value="1">科目一 (理论)</a-select-option>
            <a-select-option :value="2">科目二 (场内)</a-select-option>
            <a-select-option :value="3">科目三 (道路)</a-select-option>
            <a-select-option :value="4">科目四 (文明)</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="期望日期" required>
          <a-date-picker v-model:value="bookingForm.examDate" style="width: 100%" :disabled-date="disabledDate" />
        </a-form-item>
        <a-form-item label="期望考场" required>
          <a-select v-model:value="bookingForm.examSite" placeholder="请选择考场">
            <a-select-option v-for="site in examSites" :key="site.id" :value="site.dictValue">
              {{ site.dictValue }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 管理员：考场分配弹窗 -->
    <a-modal v-model:open="assignVisible" title="考场分配与批准" @ok="submitAssign" :confirmLoading="submitting">
      <a-form layout="vertical">
        <a-form-item label="分配考场" required>
          <a-select v-model:value="assignForm.examSite">
            <a-select-option v-for="site in examSites" :key="site.id" :value="site.dictValue">
              {{ site.dictValue }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="确认日期" required>
          <a-date-picker v-model:value="assignForm.examDate" style="width: 100%" :disabled-date="disabledDate" />
        </a-form-item>
        <p style="color: #8c8c8c; font-size: 12px;">提示：您可以修改学员申请的考场和日期进行正式派位。</p>
      </a-form>
    </a-modal>

    <!-- 管理员：录入成绩弹窗 -->
    <a-modal v-model:open="scoreVisible" title="成绩录入" @ok="submitScore" :confirmLoading="submitting">
      <a-form layout="vertical">
        <a-form-item label="考试成绩 (0-100)" required>
          <a-input-number v-model:value="currentScore" :min="0" :max="100" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/store/user'
import * as examApi from '@/api/exam'
import { getDictByType } from '@/api/common'
import dayjs from 'dayjs'

const userStore = useUserStore()
const activeTab = ref(userStore.role === 3 ? 'myExams' : 'audit')
const loading = ref(false)
const submitting = ref(false)

const myExams = ref([])
const adminExams = ref([])
const examSites = ref<any[]>([])

const bookingVisible = ref(false)
const assignVisible = ref(false)
const scoreVisible = ref(false)

const bookingForm = reactive({ subject: 1, examType: 1, examDate: null as any, examSite: undefined })
const assignForm = reactive({ id: null as any, examSite: '', examDate: null as any })

const currentExamId = ref<number | null>(null)
const currentScore = ref<number>(90)

// 禁止选择今天之前的日期
const disabledDate = (current: any) => {
  return current && current < dayjs().endOf('day').subtract(1, 'day')
}

const subjectMap: any = { 1: '一', 2: '二', 3: '三', 4: '四' }
const statusTextMap: any = { 0: '待审核', 1: '预约成功', 2: '考试完成', 3: '已拒绝' }
const statusColorMap: any = { 0: 'orange', 1: 'blue', 2: 'green', 3: 'red' }

const studentColumns = [
  { title: '类型', key: 'examType' },
  { title: '科目', key: 'subject' },
  { title: '考试日期', key: 'examDate' },
  { title: '考试地点', dataIndex: 'examSite', key: 'examSite' },
  { title: '状态', key: 'status' },
  { title: '成绩', key: 'score' },
  { title: '操作', key: 'action' }
]

const adminColumns = [
  { title: '学员姓名', dataIndex: 'studentName', key: 'studentName' },
  { title: '类型', key: 'examType' },
  { title: '科目', key: 'subject' },
  { title: '申请日期', key: 'examDate' },
  { title: '申请地点', dataIndex: 'examSite', key: 'examSite' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action' },
]

const fetchMyExams = async () => {
  loading.value = true
  try {
    const res: any = await examApi.getMyExams()
    myExams.value = res.data
  } finally {
    loading.value = false
  }
}

const fetchAdminExams = async () => {
  loading.value = true
  try {
    const res: any = await examApi.getAdminExamList()
    adminExams.value = res.data
  } finally {
    loading.value = false
  }
}

const fetchExamSites = async () => {
  try {
    const res: any = await getDictByType('EXAM_SITE')
    examSites.value = res.data || []
  } catch (err) { console.error(err) }
}

const submitBooking = async () => {
  if (!bookingForm.examDate) return message.warning('请选择日期')
  submitting.value = true
  try {
    await examApi.bookExam({ ...bookingForm, examDate: bookingForm.examDate.valueOf() })
    message.success('预约申请已提交')
    bookingVisible.value = false
    fetchMyExams()
  } catch (err: any) {
    message.error(err.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const cancelBooking = async (id: number) => {
  try {
    await examApi.cancelExam(id)
    message.success('取消预约成功')
    fetchMyExams()
  } catch (err: any) {
    message.error(err.response?.data?.message || '操作失败')
  }
}

const showAssignModal = (record: any) => {
  assignForm.id = record.id
  assignForm.examSite = record.examSite
  assignForm.examDate = record.examDate ? dayjs(record.examDate) : dayjs()
  assignVisible.value = true
}

const submitAssign = async () => {
  submitting.value = true
  try {
    await examApi.auditExam({
      id: assignForm.id,
      status: 1,
      examSite: assignForm.examSite,
      examDate: assignForm.examDate.valueOf()
    })
    message.success('考场分配成功并已批准')
    assignVisible.value = false
    fetchAdminExams()
  } catch (err) {
    message.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleAudit = async (id: number, status: number) => {
  try {
    await examApi.auditExam({ id, status })
    message.success('已处理')
    fetchAdminExams()
  } catch (err) { message.error('操作失败') }
}

const showScoreModal = (record: any) => {
  currentExamId.value = record.id
  currentScore.value = 90
  scoreVisible.value = true
}

const submitScore = async () => {
  if (currentExamId.value === null) return
  submitting.value = true
  try {
    await examApi.recordScore({ id: currentExamId.value, score: currentScore.value })
    message.success('成绩录入成功')
    scoreVisible.value = false
    fetchAdminExams()
  } catch (err) { message.error('操作失败') } finally { submitting.value = false }
}

const formatDate = (date: any) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD')
}

onMounted(() => {
  if (userStore.role === 3) { fetchMyExams(); fetchExamSites(); }
  if (userStore.role === 1) { fetchAdminExams(); fetchExamSites(); }
})
</script>

<style scoped>
.exam-manage-container { max-width: 1200px; margin: 0 auto; }
.main-card { border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.table-operations { margin-bottom: 16px; }
.pass { color: #52c41a; font-weight: bold; }
.fail { color: #f5222d; font-weight: bold; }
</style>
