<template>
  <div class="statistics-container">
    <a-page-header title="数据统计分析" sub-title="实时监控驾校报名、考试及带教负荷" />

    <!-- 顶层指标卡 -->
    <a-row :gutter="16">
      <a-col :span="6" v-for="(val, key) in overviewMap" :key="key">
        <a-card class="stat-card">
          <a-statistic :title="val.title" :value="(stats.overview as any)[key]" :value-style="{ color: val.color }" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="24" style="margin-top: 24px;">
      <a-col :span="16">
        <a-card title="趋势分析：近7天报名量" :bordered="false">
          <div ref="enrollChart" style="height: 400px;"></div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="考试通过率 (各科目 %)" :bordered="false">
          <div ref="passChart" style="height: 400px;"></div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="24" style="margin-top: 24px;">
      <a-col :span="24">
        <a-card title="教练带教负荷分布" :bordered="false">
          <div ref="loadChart" style="height: 350px;"></div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, nextTick } from 'vue'
import request from '@/utils/request'
import * as echarts from 'echarts'

const loading = ref(false)
const enrollChart = ref<HTMLElement | null>(null)
const passChart = ref<HTMLElement | null>(null)
const loadChart = ref<HTMLElement | null>(null)

const stats = reactive({
  overview: { totalStudents: 0, totalInstructors: 0, pendingEnrollments: 0, pendingExams: 0 },
  enrollmentTrend: [],
  passRates: [],
  coachLoad: []
})

const overviewMap: any = {
  totalStudents: { title: '总学员数', color: '#1890ff' },
  totalInstructors: { title: '总教练员', color: '#52c41a' },
  pendingEnrollments: { title: '待审报名', color: '#faad14' },
  pendingExams: { title: '待审考试', color: '#f5222d' }
}

const fetchStats = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/stats/admin/dashboard')
    Object.assign(stats, res.data)
    nextTick(() => {
      initEnrollChart()
      initPassChart()
      initLoadChart()
    })
  } finally {
    loading.value = false
  }
}

const initEnrollChart = () => {
  if (!enrollChart.value) return
  const chart = echarts.init(enrollChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: stats.enrollmentTrend.map((d: any) => d.statDate) },
    yAxis: { type: 'value' },
    series: [{
      name: '报名人数',
      type: 'line',
      smooth: true,
      data: stats.enrollmentTrend.map((d: any) => d.count),
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
          { offset: 1, color: 'rgba(24, 144, 255, 0)' }
        ])
      },
      lineStyle: { width: 3 }
    }]
  })
}

const initPassChart = () => {
  if (!passChart.value) return
  const chart = echarts.init(passChart.value)
  const subjects = ['科一', '科二', '科三', '科四']
  const rates = [1, 2, 3, 4].map(s => {
    const item: any = stats.passRates.find((r: any) => r.subject === s)
    return item ? Math.round((item.passed / item.total) * 100) : 0
  })

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    radar: {
      indicator: subjects.map(s => ({ name: s, max: 100 })),
      splitArea: { show: false }
    },
    series: [{
      type: 'radar',
      data: [{ value: rates, name: '通过率' }],
      areaStyle: { color: 'rgba(82, 196, 26, 0.2)' },
      lineStyle: { color: '#52c41a' }
    }]
  })
}

const initLoadChart = () => {
  if (!loadChart.value) return
  const chart = echarts.init(loadChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: stats.coachLoad.map((d: any) => d.name) },
    yAxis: { type: 'value', title: '带教人数' },
    series: [{
      name: '当前带教人数',
      type: 'bar',
      barWidth: '40%',
      data: stats.coachLoad.map((d: any) => d.value),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ])
      }
    }]
  })
}

onMounted(fetchStats)
</script>

<style scoped>
.statistics-container { padding: 0 12px; }
.stat-card { border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
</style>
