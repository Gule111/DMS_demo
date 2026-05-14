<template>
  <div class="coach-container">
    <a-page-header title="教练分配管理" sub-title="智能匹配与手动调度学员教练" />

    <a-card :bordered="false" class="main-card">
      <div style="margin-bottom: 16px; display: flex; justify-content: space-between;">
        <a-input-search placeholder="搜索教练姓名" style="width: 250px" />
        <div>
          <a-button type="primary" @click="handleAdd" style="margin-right: 8px;">新增教练</a-button>
          <a-button @click="fetchInstructors">刷新列表</a-button>
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
            <a-popconfirm title="确定要删除这个教练吗？" @confirm="handleDelete(record.id)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑教练弹窗 -->
    <a-modal v-model:open="formVisible" :title="isEdit ? '编辑教练' : '新增教练'" @ok="submitForm" :confirmLoading="formLoading">
      <a-form layout="vertical">
        <a-form-item label="教练姓名" required>
          <a-input v-model:value="formData.realName" placeholder="输入姓名" />
        </a-form-item>
        <a-form-item label="联系电话" required>
          <a-input v-model:value="formData.phone" placeholder="输入电话号码" />
        </a-form-item>
        <a-form-item label="准教车型" required>
          <a-radio-group v-model:value="formData.teachType" button-style="solid">
            <a-radio-button value="C1">C1</a-radio-button>
            <a-radio-button value="C2">C2</a-radio-button>
            <a-radio-button value="D">D</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看名下学员抽屉 -->
    <a-drawer v-model:open="drawerVisible" :title="currentCoach?.realName + ' 的学员列表'" placement="right" width="500">
      <a-table 
        :dataSource="coachStudents" 
        :columns="studentColumns" 
        rowKey="id" 
        :loading="studentsLoading" 
        size="small" 
        bordered
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'licenseType'">
            <a-tag :color="record.licenseType === 'C1' ? 'blue' : 'green'">{{ record.licenseType }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const loading = ref(false)
const instructors = ref([])

const columns = [
  { title: '教练ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '教练姓名', dataIndex: 'realName', key: 'realName' },
  { title: '联系电话', dataIndex: 'phone', key: 'phone' },
  { title: '准教车型', dataIndex: 'teachType', key: 'teachType' },
  { title: '当前已带学员人数', dataIndex: 'currentLoad', key: 'currentLoad' },
  { title: '操作', key: 'action', width: 220, align: 'center' }
]

const studentColumns = [
  { title: '姓名', dataIndex: 'realName', key: 'realName' },
  { title: '联系电话', dataIndex: 'phone', key: 'phone' },
  { title: '车型', dataIndex: 'licenseType', key: 'licenseType', align: 'center' }
]

// 颜色指示器
const getLoadStatus = (load: number) => {
  if (load >= 10) return 'error'
  if (load >= 5) return 'warning'
  return 'success'
}

// === 列表查询 ===
const fetchInstructors = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/instructor/list')
    instructors.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

// === 增删改逻辑 ===
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive({
  id: null,
  realName: '',
  phone: '',
  teachType: 'C1'
})

const handleAdd = () => {
  isEdit.value = false
  formData.id = null
  formData.realName = ''
  formData.phone = ''
  formData.teachType = 'C1'
  formVisible.value = true
}

const handleEdit = (record: any) => {
  isEdit.value = true
  formData.id = record.id
  formData.realName = record.realName
  formData.phone = record.phone
  formData.teachType = record.teachType
  formVisible.value = true
}

const submitForm = async () => {
  if (!formData.realName || !formData.phone) {
    message.warning('请填写完整信息')
    return
  }
  formLoading.value = true
  try {
    if (isEdit.value) {
      await request.put('/instructor/update', formData)
      message.success('修改成功')
    } else {
      await request.post('/instructor/add', formData)
      message.success('新增成功')
    }
    formVisible.value = false
    fetchInstructors()
  } catch (err) {
    console.error(err)
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    const res: any = await request.delete('/instructor/delete/' + id)
    message.success(res.data || '删除成功')
    fetchInstructors()
  } catch (err: any) {
    // 错误由 axios 拦截器处理
    console.error(err)
  }
}

// === 查看学员逻辑 ===
const drawerVisible = ref(false)
const studentsLoading = ref(false)
const coachStudents = ref<any[]>([])
const currentCoach = ref<any>(null)

const handleViewStudents = async (record: any) => {
  currentCoach.value = record
  drawerVisible.value = true
  studentsLoading.value = true
  try {
    const res: any = await request.get('/instructor/students/' + record.id)
    coachStudents.value = res.data || []
  } catch (err) {
    console.error(err)
  } finally {
    studentsLoading.value = false
  }
}

onMounted(() => {
  fetchInstructors()
})
</script>

<style scoped>
.coach-container {
  max-width: 1200px;
  margin: 0 auto;
}
.main-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
</style>
