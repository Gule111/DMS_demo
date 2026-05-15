<template>
  <div class="my-coach-container">
    <a-row :gutter="24">
      <!-- 左侧：教练信息卡片 -->
      <a-col :xs="24" :lg="8">
        <a-card :loading="loading" class="coach-info-card" :bordered="false">
          <div v-if="coach" class="coach-profile">
            <a-avatar :size="120" :src="coach.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'" />
            <h2 class="coach-name">{{ coach.realName }}</h2>
            <div class="coach-tag">
              <a-tag color="gold">金牌教练</a-tag>
              <a-tag color="blue">{{ coach.teachType }} 教练</a-tag>
            </div>
            <a-rate v-model:value="coach.rating" disabled allow-half />
            <div class="coach-stats">
              <div class="stat-item">
                <span class="stat-value">{{ coach.experienceYears }}年</span>
                <span class="stat-label">教龄</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-value">{{ coach.currentLoad }}人</span>
                <span class="stat-label">带教中</span>
              </div>
            </div>
            <p class="coach-intro">{{ coach.intro || '该教练很神秘，暂无个人介绍。' }}</p>
            <div class="coach-contact">
              <a-button type="primary" block size="large">
                <template #icon>📞</template>
                联系教练：{{ coach.phone }}
              </a-button>
            </div>
          </div>
          <div v-else>
            <a-empty description="您暂未被分配专属教练，后台正在为您匹配中" />
            <div style="margin-top: 24px; text-align: left;" v-if="allCoaches.length > 0">
              <h3 style="margin-bottom: 16px;">✨ 驾校优质教练一览</h3>
              <a-list item-layout="horizontal" :data-source="allCoaches">
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-list-item-meta
                      :description="'教龄: ' + item.experienceYears + '年 | 带教人数: ' + item.currentLoad"
                    >
                      <template #title>
                        <span>{{ item.realName }}</span>
                        <a-tag color="blue" style="margin-left: 8px;">{{ item.teachType }}</a-tag>
                      </template>
                      <template #avatar>
                        <a-avatar :src="item.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + item.id" />
                      </template>
                    </a-list-item-meta>
                    <div><a-rate :value="item.rating" disabled style="font-size: 14px;" /></div>
                  </a-list-item>
                </template>
              </a-list>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- 右侧：约课预约 -->
      <a-col :xs="24" :lg="16" v-if="coach">
        <a-card title="📅 约课中心" class="appointment-card" :bordered="false">
          <template #extra>
            <a-button type="link" @click="fetchAppointments">刷新预约列表</a-button>
          </template>

          <a-tabs v-model:activeKey="activeTab">
            <a-tab-pane key="book" tab="我要约课">
              <div class="booking-section">
                <a-form layout="vertical">
                  <a-form-item label="选择预约日期">
                    <a-date-picker 
                      v-model:value="bookingForm.date" 
                      style="width: 100%" 
                      :disabled-date="disabledDate"
                    />
                  </a-form-item>
                  <a-form-item label="选择预约时段">
                    <a-radio-group v-model:value="bookingForm.timeSlot" button-style="solid">
                      <a-radio-button value="08:00-10:00">08:00 - 10:00</a-radio-button>
                      <a-radio-button value="10:00-12:00">10:00 - 12:00</a-radio-button>
                      <a-radio-button value="14:00-16:00">14:00 - 16:00</a-radio-button>
                      <a-radio-button value="16:00-18:00">16:00 - 18:00</a-radio-button>
                    </a-radio-group>
                  </a-form-item>
                  <a-form-item label="练习科目">
                    <a-select v-model:value="bookingForm.subject">
                      <a-select-option :value="2">科目二 (场内技训)</a-select-option>
                      <a-select-option :value="3">科目三 (道路驾驶)</a-select-option>
                    </a-select>
                  </a-form-item>
                  <a-button type="primary" block size="large" :loading="bookingLoading" @click="handleBook">
                    立即提交预约
                  </a-button>
                </a-form>
              </div>
            </a-tab-pane>
            
            <a-tab-pane key="list" tab="我的预约">
              <a-table 
                :columns="appointmentColumns" 
                :data-source="appointments" 
                :pagination="{ pageSize: 5 }"
                size="middle"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'subject'">
                    科目{{ record.subject === 2 ? '二' : '三' }}
                  </template>
                  <template v-if="column.key === 'status'">
                    <a-tag :color="getStatusColor(record.status)">
                      {{ getStatusText(record.status) }}
                    </a-tag>
                  </template>
                  <template v-if="column.key === 'action'">
                    <a-popconfirm
                      v-if="record.status === 1"
                      title="确定要取消这次约课吗？"
                      @confirm="handleCancel(record.id)"
                    >
                      <a-button type="link" danger>取消</a-button>
                    </a-popconfirm>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import dayjs, { Dayjs } from 'dayjs'

const loading = ref(false)
const coach = ref<any>(null)
const allCoaches = ref<any[]>([])
const activeTab = ref('book')
const appointments = ref([])
const bookingLoading = ref(false)

const bookingForm = reactive({
  date: null as Dayjs | null,
  timeSlot: '08:00-10:00',
  subject: 2
})

const appointmentColumns = [
  { title: '预约日期', dataIndex: 'appointmentDate', key: 'date' },
  { title: '时段', dataIndex: 'timeSlot', key: 'time' },
  { title: '科目', key: 'subject' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action' }
]

// 禁止选择今天之前的日期
const disabledDate = (current: Dayjs) => {
  return current && current < dayjs().endOf('day').subtract(1, 'day')
}

const fetchCoachInfo = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/instructor/my')
    if (res.data) {
      coach.value = res.data
    } else {
      // 未分配专属教练，则拉取全部教练列表展示
      fetchAllCoaches()
    }
  } catch (err) {
    console.error('获取教练信息失败:', err)
  } finally {
    loading.value = false
  }
}

const fetchAllCoaches = async () => {
  try {
    const res: any = await request.get('/instructor/list')
    allCoaches.value = res.data || []
  } catch (err) {
    console.error('拉取教练列表失败:', err)
  }
}

const fetchAppointments = async () => {
  try {
    const res: any = await request.get('/appointment/my')
    appointments.value = res.data
  } catch (err) {
    console.error('获取预约记录失败:', err)
  }
}

const handleBook = async () => {
  if (!bookingForm.date) {
    return message.warning('请选择预约日期')
  }
  
  bookingLoading.value = true
  try {
    await request.post('/appointment/book', {
      appointmentDate: bookingForm.date.format('YYYY-MM-DD'),
      timeSlot: bookingForm.timeSlot,
      subject: bookingForm.subject
    })
    message.success('预约成功！请按时前往驾校练车')
    activeTab.value = 'list'
    fetchAppointments()
  } catch (err: any) {
    message.error(err.response?.data?.message || '预约失败')
  } finally {
    bookingLoading.value = false
  }
}

const handleCancel = async (id: number) => {
  try {
    await request.post(`/appointment/cancel/${id}`)
    message.success('预约已取消')
    fetchAppointments()
  } catch (err) {
    message.error('取消失败')
  }
}

const getStatusColor = (status: number) => {
  switch (status) {
    case 1: return 'processing'
    case 2: return 'success'
    case 3: return 'default'
    default: return 'default'
  }
}

const getStatusText = (status: number) => {
  switch (status) {
    case 1: return '已预约'
    case 2: return '已完成'
    case 3: return '已取消'
    default: return '未知'
  }
}

onMounted(() => {
  fetchCoachInfo()
  fetchAppointments()
})
</script>

<style scoped>
.my-coach-container {
  padding: 12px;
}

.coach-info-card {
  text-align: center;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.coach-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.coach-name {
  margin: 16px 0 8px;
  font-size: 24px;
  font-weight: bold;
}

.coach-tag {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}

.coach-stats {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 24px 0;
  width: 100%;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #1890ff;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: #eee;
}

.coach-intro {
  color: #666;
  line-height: 1.6;
  margin-bottom: 24px;
  text-align: left;
}

.coach-contact {
  width: 100%;
}

.appointment-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  min-height: 500px;
}

.booking-section {
  padding: 16px 0;
  max-width: 500px;
}
</style>
