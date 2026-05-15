<template>
  <div class="base-info-container">
    <a-page-header title="基础信息管理" sub-title="管理考场、车型及系统常量" />

    <a-card :bordered="false" class="main-card">
      <a-tabs v-model:activeKey="activeTab">
        <!-- 字典管理 -->
        <a-tab-pane key="dict" tab="数据字典">
          <div class="table-operations">
            <a-button type="primary" @click="handleAdd">新增配置项</a-button>
          </div>
          <a-table :dataSource="dicts" :columns="columns" rowKey="id" :loading="loading">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'dictType'">
                <a-tag :color="getTypeColor(record.dictType)">{{ typeMap[record.dictType] || record.dictType }}</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" @click="handleEdit(record)">编辑</a-button>
                <a-divider type="vertical" />
                <a-popconfirm title="确定删除此项吗？" @confirm="handleDelete(record.id)">
                  <a-button type="link" danger>删除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 价格/车型标准 (占位) -->
        <a-tab-pane key="pricing" tab="收费标准">
          <a-empty description="收费标准管理模块正在同步，请稍后。" />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 字典编辑弹窗 -->
    <a-modal v-model:open="modalVisible" :title="form.id ? '编辑配置' : '新增配置'" @ok="submitForm" :confirmLoading="submitting">
      <a-form :model="form" layout="vertical">
        <a-form-item label="配置类型 (Type)" required extra="例如: EXAM_SITE, LICENSE_TYPE">
          <a-auto-complete v-model:value="form.dictType" :options="typeOptions" placeholder="请输入或选择类型" />
        </a-form-item>
        <a-form-item label="配置编码 (Code)" required extra="内部使用的唯一编码">
          <a-input v-model:value="form.dictCode" placeholder="例如: SITE_NORTH" />
        </a-form-item>
        <a-form-item label="展示数值 (Value)" required extra="在页面上展示给用户的文本">
          <a-input v-model:value="form.dictValue" placeholder="例如: 北郊分考场" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import * as commonApi from '@/api/common'

const activeTab = ref('dict')
const loading = ref(false)
const submitting = ref(false)
const dicts = ref<any[]>([])

const typeMap: any = {
  'EXAM_SITE': '考场地点',
  'LICENSE_TYPE': '报考车型'
}

const modalVisible = ref(false)
const form = reactive({
  id: null as number | null,
  dictType: '',
  dictCode: '',
  dictValue: ''
})

const columns = [
  { title: '类型', dataIndex: 'dictType', key: 'dictType', width: 150 },
  { title: '编码', dataIndex: 'dictCode', key: 'dictCode' },
  { title: '展示值', dataIndex: 'dictValue', key: 'dictValue' },
  { title: '操作', key: 'action', width: 150 },
]

const typeOptions = computed(() => {
  const types = Array.from(new Set(dicts.value.map(d => d.dictType)))
  return types.map(t => ({ value: t }))
})

const fetchDicts = async () => {
  loading.value = true
  try {
    const res: any = await commonApi.getAllDicts()
    dicts.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  form.id = null
  form.dictType = ''
  form.dictCode = ''
  form.dictValue = ''
  modalVisible.value = true
}

const handleEdit = (record: any) => {
  Object.assign(form, record)
  modalVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    await commonApi.deleteDict(id)
    message.success('已删除')
    fetchDicts()
  } catch (err) {
    message.error('删除失败')
  }
}

const submitForm = async () => {
  if (!form.dictType || !form.dictCode || !form.dictValue) {
    return message.warning('请填写完整信息')
  }
  submitting.value = true
  try {
    await commonApi.saveDict(form)
    message.success('保存成功')
    modalVisible.value = false
    fetchDicts()
  } catch (err) {
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

const getTypeColor = (type: string) => {
  if (type === 'EXAM_SITE') return 'blue'
  if (type === 'LICENSE_TYPE') return 'orange'
  return 'default'
}

onMounted(fetchDicts)
</script>

<style scoped>
.base-info-container {
  max-width: 1200px;
  margin: 0 auto;
}
.main-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.table-operations {
  margin-bottom: 16px;
}
</style>
