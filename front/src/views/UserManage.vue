<template>
  <div class="user-manage">
    <div class="header">
      <h2>用户管理与角色调配</h2>
      <p>管理员可以在此查看系统所有用户，并手动调整其权限级别。</p>
    </div>

    <a-table 
      :columns="columns" 
      :data-source="users" 
      :loading="loading" 
      row-key="id"
      :pagination="{ pageSize: 10 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'roleName'">
          <a-tag :color="getRoleColor(record.roleCode)">
            {{ record.roleName || '未分配' }}
          </a-tag>
        </template>
        
        <template v-else-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'error'" :text="record.status === 1 ? '正常' : '禁用'" />
        </template>

        <template v-else-if="column.key === 'action'">
          <a-button type="link" @click="showRoleModal(record)">
            分配角色
          </a-button>
        </template>
      </template>
    </a-table>

    <!-- 角色分配弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      title="权限调配"
      @ok="handleUpdateRole"
      :confirm-loading="updating"
    >
      <a-form layout="vertical">
        <a-form-item label="当前用户">
          <a-input :value="editingUser?.username" disabled />
        </a-form-item>
        <a-form-item label="目标角色" required>
          <a-select v-model:value="selectedRoleId" placeholder="请选择新角色">
            <a-select-option :value="1">管理员</a-select-option>
            <a-select-option :value="2">教练员</a-select-option>
            <a-select-option :value="3">学员</a-select-option>
          </a-select>
        </a-form-item>
        <div v-if="selectedRoleId === 2" class="tip">
          <a-alert message="提示" description="设为教练员后，系统将自动为其创建教学档案，初始姓名为其用户名。" type="info" show-icon />
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getUserList, updateUserRole } from '@/api/admin'

const loading = ref(false)
const users = ref([])
const modalVisible = ref(false)
const editingUser = ref<any>(null)
const selectedRoleId = ref<number | null>(null)
const updating = ref(false)

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '当前角色', dataIndex: 'roleName', key: 'roleName' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', fixed: 'right', width: 120 },
]

const getRoleColor = (code: string) => {
  switch (code) {
    case 'admin': return 'purple'
    case 'instructor': return 'blue'
    case 'student': return 'green'
    default: return 'default'
  }
}

async function fetchUsers() {
  loading.value = true
  try {
    const res: any = await getUserList()
    users.value = res.data
  } catch {
    // 错误处理已在拦截器中
  } finally {
    loading.value = false
  }
}

function showRoleModal(record: any) {
  editingUser.value = record
  selectedRoleId.value = record.roleId
  modalVisible.value = true
}

async function handleUpdateRole() {
  if (!selectedRoleId.value) {
    message.warning('请选择角色')
    return
  }
  
  updating.value = true
  try {
    await updateUserRole(editingUser.value.id, selectedRoleId.value)
    message.success('角色调配成功')
    modalVisible.value = false
    fetchUsers()
  } catch {
  } finally {
    updating.value = false
  }
}

onMounted(fetchUsers)
</script>

<style scoped>
.user-manage {
  background: #fff;
}
.header {
  margin-bottom: 24px;
}
.header h2 {
  margin-bottom: 8px;
  font-weight: 600;
}
.header p {
  color: #666;
}
.tip {
  margin-top: 16px;
}
</style>
