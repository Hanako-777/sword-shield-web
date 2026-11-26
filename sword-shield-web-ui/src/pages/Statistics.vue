<template>
  <div class="statistics-container">
    <div class="page-head">
      <h1 class="page-title">数据看板</h1>
      <p class="page-subtitle">各场次报名人数、报名率与渠道来源统计</p>
    </div>

    <el-card class="stats-overview" shadow="never">
      <h2>总体概览</h2>
      <el-row :gutter="16" class="overview-row">
        <el-col :xs="12" :sm="6">
          <div class="stat-box">
            <div class="stat-label">活动总数</div>
            <div class="stat-value">{{ totalActivities }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-box">
            <div class="stat-label">总报名人数</div>
            <div class="stat-value">{{ totalRegistrations }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-box">
            <div class="stat-label">总热度</div>
            <div class="stat-value">{{ totalHeat }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="stat-box">
            <div class="stat-label">平均报名率</div>
            <div class="stat-value">{{ averageRate }}%</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="activity-stats">
      <h2>活动详细统计</h2>
      <el-table :data="activities" v-loading="loading" stripe class="stats-table">
        <el-table-column prop="activityTitle" label="活动名称" min-width="200" />
        <el-table-column label="报名人数" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="success">{{ row.registrationCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报名上限" width="120" align="center">
          <template #default="{ row }">
            {{ row.maxRegistrations || '无限制' }}
          </template>
        </el-table-column>
        <el-table-column label="报名率" width="120" align="center">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.registrationRate" 
              :color="getProgressColor(row.registrationRate)"
              :stroke-width="12"
            />
          </template>
        </el-table-column>
        <el-table-column prop="heat" label="热度" width="100" align="center" sortable />
        <el-table-column label="开始时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              link
              @click="viewDetail(row.activityId)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="channel-card">
      <div class="channel-head">
        <h2>报名渠道溯源</h2>
        <el-select v-model="channelActivityId" style="width: 280px" @change="loadChannels">
          <el-option
            v-for="a in activities"
            :key="a.activityId"
            :label="a.activityTitle"
            :value="a.activityId"
          />
        </el-select>
      </div>
      <div v-loading="channelLoading" class="channel-list">
        <div v-for="c in channelStats" :key="c.key" class="channel-row">
          <span class="channel-name">{{ c.label }}</span>
          <el-progress :percentage="c.percent" :stroke-width="14" :color="c.color" class="channel-bar" />
          <span class="channel-count">{{ c.count }} 人</span>
        </div>
        <div v-if="!channelLoading && !channelStats.length" class="channel-empty">暂无报名数据</div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 数据看板（活动统计）。
 *
 * - 汇总展示各活动的报名数、报名率、热度等统计指标，并提供跳转到活动详情的入口。
 *
 * 依赖接口：
 * - GET `/activity/statistics/all`
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const activities = ref<any[]>([])
const loading = ref(false)

const channelActivityId = ref<number | null>(null)
const channelRaw = ref<Record<string, number>>({})
const channelLoading = ref(false)

const channelLabels: Record<string, string> = {
  web: '官网 Web',
  miniprogram: '微信小程序',
  onsite: '现场扫码',
  invite: '邀请链接',
  unknown: '其他'
}
const channelColors: Record<string, string> = {
  web: '#0b2545',
  miniprogram: '#0f766e',
  onsite: '#b45309',
  invite: '#334155',
  unknown: '#94a3b8'
}

const channelStats = computed(() => {
  const entries = Object.entries(channelRaw.value)
  const total = entries.reduce((s, [, n]) => s + n, 0)
  return entries
    .map(([key, count]) => ({
      key,
      count,
      label: channelLabels[key] || key,
      color: channelColors[key] || '#909399',
      percent: total ? Math.round((count / total) * 10000) / 100 : 0
    }))
    .sort((a, b) => b.count - a.count)
})

async function loadChannels() {
  if (!channelActivityId.value) return
  channelLoading.value = true
  try {
    const { data } = await api.get('/activity/statistics/channels', {
      params: { id: channelActivityId.value }
    })
    if (data.code === 0) channelRaw.value = data.data || {}
  } catch {
    channelRaw.value = {}
  } finally {
    channelLoading.value = false
  }
}

const totalActivities = computed(() => activities.value.length)
const totalRegistrations = computed(() => 
  activities.value.reduce((sum, a) => sum + (a.registrationCount || 0), 0)
)
const totalHeat = computed(() => 
  activities.value.reduce((sum, a) => sum + (a.heat || 0), 0)
)
const averageRate = computed(() => {
  const validActivities = activities.value.filter(a => a.maxRegistrations > 0)
  if (validActivities.length === 0) return 0
  const sum = validActivities.reduce((s, a) => s + (a.registrationRate || 0), 0)
  return Math.round((sum / validActivities.length) * 100) / 100
})

function getProgressColor(percentage: number) {
  if (percentage >= 80) return '#f56c6c'
  if (percentage >= 50) return '#e6a23c'
  return '#67c23a'
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function viewDetail(activityId: number) {
  router.push(`/activity/${activityId}`)
}

async function loadStatistics() {
  loading.value = true
  try {
    const { data } = await api.get('/activity/statistics/all')
    if (data.code === 0) {
      activities.value = data.data
      if (activities.value.length && !channelActivityId.value) {
        channelActivityId.value = activities.value[0].activityId
        await loadChannels()
      }
    } else {
      ElMessage.error(data.message || '加载统计数据失败')
    }
  } catch (error) {
    ElMessage.error('加载统计数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.statistics-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-head {
  padding: 8px 0 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #d8e0e5;
}

.page-title {
  margin: 0 0 6px 0;
  font-size: 26px;
  color: #0b2545;
  font-weight: 700;
}

.page-subtitle {
  margin: 0;
  color: #506273;
  font-size: 14px;
}

.stats-overview {
  margin-bottom: 20px;
  border-radius: 8px;
  border: 1px solid #d8e0e5;
}

.stats-overview h2 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #0b2545;
  font-weight: 600;
}

.overview-row {
  margin-top: 8px;
}

.stat-box {
  background: #f8fafc;
  border: 1px solid #e2e8ee;
  border-top: 3px solid #0b2545;
  border-radius: 6px;
  padding: 18px 20px;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 13px;
  color: #506273;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #0b2545;
  line-height: 1.1;
}

.activity-stats {
  border-radius: 8px;
  border: 1px solid #d8e0e5;
  box-shadow: none;
}

.activity-stats h2 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #0b2545;
  font-weight: 600;
}

.stats-table {
  border-radius: 6px;
  overflow: hidden;
}

.stats-table :deep(.el-progress__text) {
  font-size: 12px !important;
  font-weight: 600;
}

.channel-card {
  margin-top: 20px;
  border-radius: 8px;
  border: 1px solid #d8e0e5;
  box-shadow: none;
}

.channel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.channel-head h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
  font-weight: 600;
}

.channel-list {
  min-height: 100px;
}

.channel-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
}

.channel-name {
  width: 110px;
  font-size: 14px;
  color: #444;
  flex-shrink: 0;
}

.channel-bar {
  flex: 1;
}

.channel-count {
  width: 80px;
  text-align: right;
  font-size: 14px;
  color: #666;
}

.channel-empty {
  text-align: center;
  color: #999;
  padding: 24px;
}
</style>
