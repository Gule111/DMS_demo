<template>
  <div class="registration-container">
    <a-page-header title="在线报名" sub-title="提交报名材料，等待系统审核" />

    <div class="form-card">
      <a-steps :current="currentStep" style="margin-bottom: 40px">
        <a-step title="基本要求" description="确认准驾车型" />
        <a-step title="材料上传" description="上传身份及体检证明" />
        <a-step title="提交审核" description="等待系统结果" />
      </a-steps>

      <!-- 第一步：基本要求 -->
      <div v-show="currentStep === 0" class="step-content">
        <a-form layout="vertical">
          <a-form-item label="请选择您要报考的准驾车型" required>
            <a-radio-group v-model:value="formData.licenseType" button-style="solid" size="large">
              <a-radio-button value="C1">🚗 小型汽车 (C1)</a-radio-button>
              <a-radio-button value="C2">🚙 小型自动挡汽车 (C2)</a-radio-button>
              <a-radio-button value="D">🏍️ 普通二轮摩托车 (D)</a-radio-button>
            </a-radio-group>
          </a-form-item>
          <div style="margin-top: 24px;">
            <a-button type="primary" @click="nextStep" :disabled="!formData.licenseType">下一步</a-button>
          </div>
        </a-form>
      </div>

      <!-- 第二步：材料上传 -->
      <div v-show="currentStep === 1" class="step-content">
        <a-alert message="请上传清晰、无反光的原件照片，支持 jpg/png 格式" type="info" show-icon style="margin-bottom: 24px;" />
        
        <a-row :gutter="24">
          <a-col :span="8">
            <div class="upload-box">
              <div class="upload-title">身份证人像面</div>
              <a-upload
                v-model:file-list="fileLists.idCardFront"
                :before-upload="beforeUpload"
                list-type="picture-card"
                :max-count="1"
              >
                <div v-if="fileLists.idCardFront.length < 1">
                  <div style="font-size: 24px;">🪪</div>
                  <div style="margin-top: 8px">点击上传</div>
                </div>
              </a-upload>
            </div>
          </a-col>
          
          <a-col :span="8">
            <div class="upload-box">
              <div class="upload-title">身份证国徽面</div>
              <a-upload
                v-model:file-list="fileLists.idCardBack"
                :before-upload="beforeUpload"
                list-type="picture-card"
                :max-count="1"
              >
                <div v-if="fileLists.idCardBack.length < 1">
                  <div style="font-size: 24px;">🏛️</div>
                  <div style="margin-top: 8px">点击上传</div>
                </div>
              </a-upload>
            </div>
          </a-col>

          <a-col :span="8">
            <div class="upload-box">
              <div class="upload-title">体检合格证明</div>
              <a-upload
                v-model:file-list="fileLists.healthCert"
                :before-upload="beforeUpload"
                list-type="picture-card"
                :max-count="1"
              >
                <div v-if="fileLists.healthCert.length < 1">
                  <div style="font-size: 24px;">🏥</div>
                  <div style="margin-top: 8px">点击上传</div>
                </div>
              </a-upload>
            </div>
          </a-col>
        </a-row>

        <div style="margin-top: 32px; display: flex; gap: 16px;">
          <a-button @click="currentStep--">上一步</a-button>
          <a-button type="primary" @click="handleSubmit" :loading="submitting" :disabled="!canSubmit">
            提交审核
          </a-button>
        </div>
      </div>

      <!-- 第三步：提交完成/审核中/结果回显 -->
      <div v-show="currentStep === 2" class="step-content">
        <!-- 情况 A：正在审核 (0-待AI, 1-AI过待人工, 2-AI驳待人工) -->
        <a-result
          v-if="!latestEnrollment || latestEnrollment.auditStatus === 0 || latestEnrollment.auditStatus === 1 || latestEnrollment.auditStatus === 2"
          status="info"
          :title="latestEnrollment?.auditStatus === 0 ? '资料提交成功，系统正在排队审核中...' : 'AI 初审已完成，正在等待人工确认...'"
          sub-title="AI 初审通常需要 1-3 分钟，人工复审通常在 24 小时内完成。"
        >
          <template #icon>
            <div class="loading-icon">{{ latestEnrollment?.auditStatus === 0 ? '🤖⏳' : '👮‍♂️🔍' }}</div>
          </template>
          <template #extra>
            <a-button type="primary" @click="$router.push('/app/dashboard')">返回工作台</a-button>
            <a-button @click="fetchStatus" :loading="statusLoading">刷新状态</a-button>
          </template>
          <div v-if="latestEnrollment?.auditStatus === 1 || latestEnrollment?.auditStatus === 2" class="ai-suggestion">
            <a-alert :message="latestEnrollment.auditRemark" type="warning" show-icon />
          </div>
        </a-result>

        <!-- 情况 B：终审通过 (Status 3) -->
        <a-result
          v-else-if="latestEnrollment.auditStatus === 3"
          status="success"
          title="恭喜！您的报名最终审核已通过"
          sub-title="系统已自动生成您的电子档案，您可以现在去查看分配的教练，或者开始科目一的学习。"
        >
          <div class="docs-container" v-if="generatedDocs.length > 0">
            <h3 style="margin-bottom: 16px;">📚 电子档案下载</h3>
            <div style="display: flex; gap: 16px; justify-content: center; flex-wrap: wrap;">
              <a-button 
                v-for="doc in generatedDocs" 
                :key="doc.id"
                type="dashed" 
                @click="downloadDoc(doc.fileUrl)"
              >
                <template #icon>📄</template>
                {{ getDocName(doc.docType) }}
              </a-button>
            </div>
          </div>
          
          <template #extra>
            <a-button type="primary" @click="$router.push('/app/my-coach')">查看教练</a-button>
            <a-button @click="$router.push('/app/dashboard')">返回工作台</a-button>
          </template>
        </a-result>

        <!-- 情况 C：终审驳回 (Status 4) -->
        <a-result
          v-else-if="latestEnrollment.auditStatus === 4"
          status="error"
          title="报名审核未通过"
          :sub-title="`最终处理结果：${latestEnrollment.auditRemark || '资料不符合规范'}`"
        >
          <template #extra>
            <a-button type="primary" @click="handleResubmit">重新修改资料</a-button>
            <a-button @click="$router.push('/app/dashboard')">返回工作台</a-button>
          </template>
        </a-result>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
const currentStep = ref(0)
const submitting = ref(false)
const statusLoading = ref(false)
const latestEnrollment = ref<any>(null)
const generatedDocs = ref<any[]>([])

// 表单数据
const formData = ref({
  licenseType: ''
})

// 文件列表状态
const fileLists = ref({
  idCardFront: [] as any[],
  idCardBack: [] as any[],
  healthCert: [] as any[]
})

// 查询最新状态
const fetchStatus = async () => {
  statusLoading.value = true
  try {
    const res: any = await request.get('/enrollment/status')
    latestEnrollment.value = res.data
    
    if (latestEnrollment.value) {
      currentStep.value = 2
      // 如果已通过，尝试拉取电子档案
      if (latestEnrollment.value.auditStatus === 3) {
        fetchDocs()
      }
    }
  } catch (err) {
    console.error('获取状态失败:', err)
  } finally {
    statusLoading.value = false
  }
}

// 拉取电子档案列表
const fetchDocs = async () => {
  try {
    const res: any = await request.get('/docs/my')
    generatedDocs.value = res.data || []
  } catch (err) {
    console.error('获取电子档案失败:', err)
  }
}

// 文档类型转中文
const getDocName = (type: string) => {
  const map: any = {
    'EnrollmentForm': '机动车驾驶证申请表',
    'HealthCert': '驾驶人身体条件证明',
    'ExamTicket': '机动车考试准考证'
  }
  return map[type] || '电子档案'
}

// 下载/预览文档
const downloadDoc = (url: string) => {
  window.open(url, '_blank')
}

// 重新提交
const handleResubmit = () => {
  currentStep.value = 0
  // 清空之前的文件（可选）
  fileLists.value.idCardFront = []
  fileLists.value.idCardBack = []
  fileLists.value.healthCert = []
}

onMounted(() => {
  fetchStatus()
})

// 阻止组件自带的上传请求，改为我们手动统一上传
const beforeUpload = () => {
  return false 
}

const nextStep = () => {
  currentStep.value++
}

// 检查是否三个文件都选了
const canSubmit = computed(() => {
  return fileLists.value.idCardFront.length > 0 &&
         fileLists.value.idCardBack.length > 0 &&
         fileLists.value.healthCert.length > 0
})

// 提交整个表单
const handleSubmit = async () => {
  if (!canSubmit.value) {
    message.warning('请上传所有必需的图片材料')
    return
  }

  submitting.value = true
  
  // 使用 FormData 包装文件和文本数据
  const uploadData = new FormData()
  uploadData.append('licenseType', formData.value.licenseType)
  // 获取实际的文件对象 (originFileObj)
  uploadData.append('idCardFront', fileLists.value.idCardFront[0].originFileObj)
  uploadData.append('idCardBack', fileLists.value.idCardBack[0].originFileObj)
  uploadData.append('healthCert', fileLists.value.healthCert[0].originFileObj)

  try {
    const res: any = await request.post('/enrollment/submit', uploadData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    message.success(res.data || '提交成功')
    // 清除旧的记录缓存，以免显示上次的驳回状态
    latestEnrollment.value = null
    // 拉取最新的报名记录（此时应该是待审核状态）
    await fetchStatus()
    // 跳到审核中步骤
    currentStep.value = 2
  } catch (err: any) {
    // 错误已由 axios 拦截器统一提示
    console.error('提交失败:', err)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.registration-container {
  max-width: 900px;
  margin: 0 auto;
}

.form-card {
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.step-content {
  min-height: 300px;
  padding: 20px 0;
}

.upload-box {
  text-align: center;
  padding: 20px;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  background: #fafafa;
  transition: border-color 0.3s;
}

.upload-box:hover {
  border-color: #1890ff;
}

.upload-title {
  font-weight: bold;
  margin-bottom: 16px;
  color: #333;
}

/* 深度覆盖 ant-upload 样式让它居中更大一点 */
:deep(.ant-upload.ant-upload-select-picture-card) {
  width: 100%;
  height: 140px;
  margin-right: 0;
  margin-bottom: 0;
  background: transparent;
  border: none;
}

.loading-icon {
  font-size: 64px;
  animation: pulse 2s infinite;
}

.ai-suggestion {
  margin-top: 24px;
  text-align: left;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
}

@keyframes pulse {
  0% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.1); }
  100% { opacity: 1; transform: scale(1); }
}
</style>
