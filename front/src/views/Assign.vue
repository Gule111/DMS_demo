<template>
  <div class="assign-container">
    <a-page-header title="分配管理" sub-title="为审核通过的学员分配教练，或强制调配已有教练的学员" />

    <a-card :bordered="false" class="main-card">
      <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <!-- 标签页 1：未分配教练 -->
        <a-tab-pane key="pending" tab="未分配教练的学员">
          <div style="margin-bottom: 16px; display: flex; justify-content: space-between;">
            <a-input-search placeholder="搜索学员姓名" style="width: 250px" />
            <a-button type="primary" @click="fetchPendingStudents">刷新列表</a-button>
          </div>
          <a-table :dataSource="pendingStudents" :columns="columns" rowKey="id" :loading="loading" bordered>
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'licenseType'">
                <a-tag :color="record.licenseType === 'C1' ? 'blue' : 'green'">{{ record.licenseType }}</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="primary" @click="handleAutoAssign(record)" :loading="assigningId === record.id">
                  智能分配
                </a-button>
                <a-button type="link" @click="handleManualModal(record)" style="margin-left: 8px;">
                  手动指定
                </a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 标签页 2：已分配教练 -->
        <a-tab-pane key="assigned" tab="已分配教练的学员">
          <div style="margin-bottom: 16px; display: flex; justify-content: space-between;">
            <a-input-search placeholder="搜索学员姓名" style="width: 250px" />
            <a-button type="primary" @click="fetchAssignedStudents">刷新列表</a-button>
          </div>
          <a-table :dataSource="assignedStudents" :columns="assignedColumns" rowKey="id" :loading="loading" bordered>
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'licenseType'">
                <a-tag :color="record.licenseType === 'C1' ? 'blue' : 'green'">{{ record.licenseType }}</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" @click="handleManualModal(record)">
                  强制调配换教练
                </a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 手动指定/强制调配教练弹窗 -->
    <a-modal v-model:open="manualVisible" :title="'为学员 [' + selectedStudent?.realName + '] 指定教练'" @ok="submitManualAssign" :confirmLoading="modalLoading">
      <a-form layout="vertical">
        <a-form-item label="请选择教练">
          <a-select v-model:value="selectedInstructorId" placeholder="选择教练 (仅显示同车型教练)" style="width: 100%">
            <a-select-option v-for="inst in availableInstructors" :key="inst.id" :value="inst.id">
              {{ inst.realName }} ({{ inst.teachType }}) - 已带学员: {{ inst.currentLoad }} 人
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-alert v-if="selectedStudent?.instructorId" message="注意：该学员已有教练，重新指定将自动扣减原教练的带教名额。" type="warning" show-icon style="margin-top: 10px;" />
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, createVNode } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const activeTab = ref('pending')
const loading = ref(false)
const pendingStudents = ref([])
const assignedStudents = ref([])
const assigningId = ref<number | null>(null)

// 弹窗相关
const manualVisible = ref(false)
const modalLoading = ref(false)
const selectedStudent = ref<any>(null)
const selectedInstructorId = ref<number | null>(null)
const allInstructors = ref<any[]>([])
const availableInstructors = ref<any[]>([])

const columns = [
  { title: '学员ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '学员姓名', dataIndex: 'realName', key: 'realName' },
  { title: '联系电话', dataIndex: 'phone', key: 'phone' },
  { title: '报考车型', dataIndex: 'licenseType', key: 'licenseType' },
  { title: '操作', key: 'action', width: 200, align: 'center' }
]

const assignedColumns = [
  ...columns.slice(0, 4),
  { title: '当前教练ID', dataIndex: 'instructorId', key: 'instructorId' },
  { title: '操作', key: 'action', width: 150, align: 'center' }
]

const handleTabChange = (key: string) => {
  if (key === 'pending') fetchPendingStudents()
  if (key === 'assigned') fetchAssignedStudents()
}

const fetchPendingStudents = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/student/pending')
    pendingStudents.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const fetchAssignedStudents = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/student/assigned')
    assignedStudents.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const fetchAllInstructors = async () => {
  try {
    const res: any = await request.get('/instructor/list')
    allInstructors.value = res.data || []
  } catch (err) {
    console.error(err)
  }
}

// 智能分配弹窗预览与执行
const handleAutoAssign = async (record: any) => {
  assigningId.value = record.id
  try {
    // 1. 先查询最匹配的教练
    const matchRes: any = await request.get('/instructor/best-match?licenseType=' + record.licenseType)
    const bestCoach = matchRes.data

    if (!bestCoach) {
      message.error('未找到合适的教练')
      return
    }

    // 2. 弹出确认框
    Modal.confirm({
      title: '智能分配推荐',
      icon: createVNode(ExclamationCircleOutlined),
      content: `系统为学员 [${record.realName}] 推荐了最闲的教练: ${bestCoach.realName} (目前已带学员: ${bestCoach.currentLoad} 人)。确认分配吗？`,
      okText: '确认分配',
      cancelText: '取消',
      onOk: async () => {
        // 3. 执行分配 (直接调用 manual 接口传入选定的教练 ID，逻辑一样)
        const formData = new URLSearchParams()
        formData.append('studentId', record.id.toString())
        formData.append('instructorId', bestCoach.id.toString())
        
        try {
          const assignRes: any = await request.post('/instructor/assign/manual', formData)
          message.success(assignRes.data || '智能分配成功！')
          fetchPendingStudents()
          fetchAllInstructors()
        } catch (err) {
          console.error(err)
        }
      }
    })
  } catch (err: any) {
    // 错误会被 axios 拦截并提示
    console.error('查询最佳匹配失败', err)
  } finally {
    assigningId.value = null
  }
}

// 打开手动分配/强制调配弹窗
const handleManualModal = (record: any) => {
  selectedStudent.value = record
  selectedInstructorId.value = record.instructorId || null
  availableInstructors.value = allInstructors.value.filter(
    (inst) => inst.teachType === record.licenseType
  )
  manualVisible.value = true
}

// 提交手动/强制分配
const submitManualAssign = async () => {
  if (!selectedInstructorId.value) {
    message.warning('请选择一位教练')
    return
  }
  
  if (selectedInstructorId.value === selectedStudent.value.instructorId) {
    message.warning('学员当前的教练已经是此人，无需更改')
    return
  }
  
  modalLoading.value = true
  try {
    const formData = new URLSearchParams()
    formData.append('studentId', selectedStudent.value.id.toString())
    formData.append('instructorId', selectedInstructorId.value.toString())

    const res: any = await request.post('/instructor/assign/manual', formData)
    message.success(res.data)
    manualVisible.value = false
    
    // 刷新数据
    if (activeTab.value === 'pending') fetchPendingStudents()
    if (activeTab.value === 'assigned') fetchAssignedStudents()
    fetchAllInstructors()
  } catch (err: any) {
    console.error('手动分配失败', err)
  } finally {
    modalLoading.value = false
  }
}

onMounted(() => {
  fetchPendingStudents()
  fetchAllInstructors()
})
</script>

<style scoped>
.assign-container {
  max-width: 1200px;
  margin: 0 auto;
}
.main-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
</style>
