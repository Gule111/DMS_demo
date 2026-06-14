<template>
  <div class="audit-manage-container">
    <a-page-header title="报名审核" sub-title="处理 AI 初审后的学员报名材料" />

    <div class="table-card">
      <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <a-tab-pane key="1" tab="AI 初审通过" />
        <a-tab-pane key="2" tab="AI 初审驳回" />
        <a-tab-pane key="all" tab="全部记录" />
      </a-tabs>

      <a-table 
        :columns="columns" 
        :data-source="list" 
        :loading="loading" 
        row-key="id"
        :pagination="{ pageSize: 10 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'auditStatus'">
            <a-tag :color="getStatusColor(record.auditStatus)">
              {{ getStatusText(record.auditStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="openAuditModal(record)">
              {{ activeTab === 'all' ? '查看' : '审核' }}
            </a-button>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 审核弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="activeTab === 'all' ? '查看报名材料' : '学员报名材料审核'"
      @ok="handleAuditSubmit"
      :confirmLoading="submitting"
      width="1000px"
    >
      <template #footer v-if="activeTab === 'all'">
        <a-button @click="modalVisible = false">关闭</a-button>
      </template>
      <div v-if="currentRecord" class="audit-modal-content">
        <a-row :gutter="24">
          <!-- 左侧：图片展示 -->
          <a-col :span="16">
            <div class="image-viewer">
              <div class="img-item">
                <div class="img-label">身份证人像面</div>
                <a-image :src="currentRecord.idCardFront" width="100%" />
              </div>
              <div class="img-item">
                <div class="img-label">身份证国徽面</div>
                <a-image :src="currentRecord.idCardBack" width="100%" />
              </div>
              <div class="img-item">
                <div class="img-label">体检合格证明</div>
                <a-image :src="currentRecord.healthCert" width="100%" />
              </div>
            </div>
          </a-col>

          <!-- 右侧：审核表单 -->
          <a-col :span="8">
            <div class="audit-info">
              <div class="info-item">
                <label>学员姓名：</label>
                <span>{{ currentRecord.student_name }}</span>
              </div>
              <div class="info-item">
                <label>报考车型：</label>
                <a-tag color="blue">{{ currentRecord.license_type }}</a-tag>
              </div>
              
              <!-- 未终审时展示 AI 建议 -->
              <div class="ai-suggestion-box" v-if="currentRecord.auditStatus === 1 || currentRecord.auditStatus === 2 || currentRecord.auditStatus === 0">
                <div class="box-title">🤖 AI 初审建议</div>
                <p>{{ currentRecord.auditRemark || '暂无建议' }}</p>
              </div>
              
              <!-- 已经终审时展示最终结果 -->
              <div class="ai-suggestion-box" style="background: #e6f7ff; border-color: #91d5ff;" v-else-if="currentRecord.auditStatus === 3 || currentRecord.auditStatus === 4">
                <div class="box-title" style="color: #0050b3;">📋 最终审核结果</div>
                <div style="margin-top: 8px; font-size: 14px;">
                  <span style="color: #0050b3; margin-right: 8px;"><strong>状态:</strong></span>
                  <a-tag :color="getStatusColor(currentRecord.auditStatus)">
                    {{ getStatusText(currentRecord.auditStatus) }}
                  </a-tag>
                </div>
                <div style="margin-top: 8px; font-size: 14px; color: #0050b3;">
                  <strong>处理意见:</strong> <br/>
                  <span style="white-space: pre-wrap; display: inline-block; margin-top: 4px;">{{ currentRecord.auditRemark || '无' }}</span>
                </div>
              </div>

              <a-form v-if="activeTab !== 'all'" layout="vertical" style="margin-top: 24px;">
                <a-form-item label="最终审核结果" required>
                  <a-radio-group v-model:value="auditForm.status">
                    <a-radio :value="3">通过 (最终成功)</a-radio>
                    <a-radio :value="4">驳回 (彻底拒绝)</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item label="审核备注/理由">
                  <a-textarea 
                    v-model:value="auditForm.remark" 
                    placeholder="请输入最终审核意见，学员可见"
                    :rows="4" 
                  />
                </a-form-item>
              </a-form>
            </div>
          </a-col>
        </a-row>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const activeTab = ref('1')
const loading = ref(false)
const list = ref([])
const modalVisible = ref(false)
const currentRecord = ref<any>(null)
const submitting = ref(false)

const auditForm = ref({
  status: 3,
  remark: ''
})

const columns = [
  { title: '学员姓名', dataIndex: 'student_name', key: 'student_name' },
  { title: '报考车型', dataIndex: 'license_type', key: 'license_type' },
  { title: '手机号', dataIndex: 'student_phone', key: 'student_phone' },
  { title: '当前状态', dataIndex: 'auditStatus', key: 'auditStatus' },
  { title: 'AI 备注', dataIndex: 'auditRemark', key: 'auditRemark', ellipsis: true },
  { title: '操作', key: 'action', fixed: 'right', width: 100 },
]

const fetchList = async () => {
  loading.value = true
  try {
    const status = activeTab.value === 'all' ? null : activeTab.value
    const res: any = await request.get('/enrollment/admin/list', { params: { status } })
    list.value = res.data
  } catch (err) {
    console.error('获取列表失败:', err)
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  fetchList()
}

const getStatusColor = (status: number) => {
  const colors: any = {
    0: 'default',
    1: 'processing',
    2: 'warning',
    3: 'success',
    4: 'error'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: any = {
    0: '待AI审核',
    1: 'AI初审通过',
    2: 'AI初审驳回',
    3: '终审成功',
    4: '终审驳回'
  }
  return texts[status] || '未知'
}

const openAuditModal = (record: any) => {
  currentRecord.value = record
  auditForm.value.status = record.auditStatus === 1 ? 3 : 4
  auditForm.value.remark = record.auditRemark
  modalVisible.value = true
}

const handleAuditSubmit = async () => {
  submitting.value = true
  try {
    await request.post('/enrollment/admin/audit', {
      enrollmentId: currentRecord.value.id,
      status: auditForm.value.status,
      remark: auditForm.value.remark
    })
    message.success('审核处理成功')
    modalVisible.value = false
    fetchList()
  } catch (err) {
    console.error('审核失败:', err)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.audit-manage-container {
  max-width: 1200px;
  margin: 0 auto;
}

.table-card {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.image-viewer {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 12px;
}

.img-item {
  border: 1px solid #eee;
  padding: 8px;
  border-radius: 4px;
}

.img-label {
  font-weight: bold;
  margin-bottom: 8px;
  color: #666;
}

.audit-info {
  padding: 0 12px;
}

.info-item {
  margin-bottom: 12px;
  font-size: 14px;
}

.info-item label {
  color: #999;
}

.ai-suggestion-box {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  padding: 16px;
  border-radius: 4px;
  margin-top: 20px;
}

.box-title {
  font-weight: bold;
  color: #856404;
  margin-bottom: 8px;
}

.ai-suggestion-box p {
  margin-bottom: 0;
  color: #856404;
}
</style>
