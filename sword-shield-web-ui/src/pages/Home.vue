<template>
  <div class="home-page">
    <ConferenceHero :events="events" />

    <div id="home-explore" class="home-below" tabindex="-1" role="region" aria-label="探索活动">
      <!-- 1. 活动回顾 -->
      <div v-reveal class="home-section home-reveal">
        <HomeActivities
          :events="events"
          :now="now"
          :loading="eventsLoading"
          :error="eventsError"
          @retry="reloadEvents"
        />
      </div>

      <!-- 2. 即将举办 -->
      <div v-reveal class="home-section home-reveal">
        <HomeUpcoming :items="featuredConferences" />
      </div>

      <!-- 3. 最新资讯（含分类） -->
      <div v-reveal class="home-section home-reveal">
        <HomeArticles />
      </div>

      <!-- 4. 我报名的活动（仅登录后显示） -->
      <section v-if="isLoggedIn" v-reveal class="home-section home-reveal" aria-labelledby="home-mine-title">
        <HomeSectionHead title="我报名的活动" title-id="home-mine-title">
          <template #links>
            <button type="button" class="home-section-link" :disabled="myActivitiesLoading" @click="loadMyActivities">
              <span>{{ myActivitiesLoading ? '刷新中...' : '刷新' }}</span>
            </button>
            <router-link v-if="myActivitiesTotal > 5" to="/profile" class="home-section-link">
              <span>查看全部 ({{ myActivitiesTotal }})</span>
              <Icon name="arrow-right" :size="16" />
            </router-link>
          </template>
        </HomeSectionHead>

        <div v-if="myActivitiesLoading && !myActivities.length" class="mine-grid" aria-busy="true">
          <div v-for="n in 4" :key="n" class="mine-card skeleton-card">
            <div class="ss-skeleton sk-date-box" />
            <div class="mine-info">
              <div class="ss-skeleton sk-line-1" />
              <div class="ss-skeleton sk-line-2" />
            </div>
          </div>
        </div>

        <div v-else-if="myActivitiesError && !myActivities.length" class="mine-state">
          <p>{{ myActivitiesError }}</p>
          <button type="button" class="mine-btn" @click="loadMyActivities">重新加载</button>
        </div>

        <div v-else-if="myActivities.length === 0" class="mine-state">
          <p>您还没有报名任何活动</p>
          <router-link to="/activities" class="mine-btn mine-btn--dark">去报名活动</router-link>
        </div>

        <div v-else class="mine-grid">
          <button
            v-for="activity in myActivities"
            :key="activity.id"
            type="button"
            class="mine-card"
            @click="$router.push(`/activity/${activity.id}`)"
          >
            <div class="ss-date-block mine-date-box" aria-hidden="true">
              <span class="ss-date-block__m">{{ activityDateParts(activity.startTime).month }}</span>
              <span class="ss-date-block__d mine-date-day">{{ activityDateParts(activity.startTime).day }}</span>
            </div>
            <div class="mine-info">
              <div class="mine-title-row">
                <span
                  v-if="getActivityStatus(activity).label === '进行中'"
                  class="mine-live-dot"
                  aria-hidden="true"
                />
                <span class="mine-title">{{ activity.title }}</span>
              </div>
              <span class="mine-meta">
                {{ formatDateTime(activity.startTime) }} · {{ getChannelLabel(activity.registrationChannel) }} · {{ getActivityStatus(activity).label }}
              </span>
            </div>
          </button>
        </div>
      </section>

      <!-- 6. 大会概览 -->
      <div v-reveal class="home-section home-reveal">
        <ConferenceMetrics
          :now="now"
          :next="upcoming[0] || ongoing[0]"
          :date-range="featuredConferences[0].dateRange"
          :location="featuredConferences[0].location"
          :activity-total="eventsTotal"
          :activity-loading="eventsLoading"
          :activity-error="eventsError"
          @retry="reloadEvents"
        />
      </div>

      <!-- 7. 宣传海报生成 -->
      <section v-reveal class="home-section home-reveal" aria-labelledby="home-poster-title">
        <HomeSectionHead title="宣传海报生成" title-id="home-poster-title" />
        <div class="poster-panel">
          <p class="poster-desc">输入活动主副标题，快速生成统一样式的大会宣传海报。</p>
          <el-form class="poster-form" @submit.prevent>
            <el-form-item label="海报标题">
              <el-input v-model="title" placeholder="请输入海报主标题，如“西湖论剑”" />
            </el-form-item>
            <el-form-item label="副标题">
              <el-input v-model="subtitle" placeholder="请输入副标题，如“数字安全大会”" />
            </el-form-item>
            <el-button type="primary" class="poster-submit" @click="generatePoster" :loading="generating">
              生成海报
            </el-button>
          </el-form>
          <div v-if="posterUrl" class="poster-result">
            <el-input v-model="posterUrl" readonly />
            <div class="poster-result-actions">
              <el-button type="primary" @click="openPoster">打开海报</el-button>
              <el-button @click="copyPosterUrl">复制链接</el-button>
            </div>
          </div>
        </div>
      </section>

      <!-- 8. 参展单位 -->
      <div v-reveal class="home-section home-reveal">
        <ExhibitorStrip />
      </div>
    </div>

    <!-- 9. 结尾行动区 -->
    <section v-reveal class="closing-cta home-reveal" aria-labelledby="closing-cta-title">
      <h2 id="closing-cta-title" class="closing-title">西湖论剑 · 数字安全大会</h2>
      <div class="closing-actions">
        <router-link to="/activities" class="closing-btn closing-btn--outline">探索活动</router-link>
        <router-link to="/payment" class="closing-btn closing-btn--dark">购票</router-link>
      </div>
    </section>

    <section v-reveal class="brand-wordmark home-reveal">
      <div class="page-width">
        <div
          class="wordmark"
          role="img"
          aria-label="SWORD SHIELD"
          :style="{ '--line-w': wordmarkLineWidth, '--max-w': wordmarkMaxWidth }"
        >
          <svg
            v-for="(word, w) in wordmarkWords"
            :key="w"
            class="wordmark-word"
            :viewBox="`0 0 ${word.width} 200`"
            :style="{ '--word-w': word.width }"
            aria-hidden="true"
            focusable="false"
          >
            <g
              v-for="(glyph, i) in word.glyphs"
              :key="i"
              class="wordmark-glyph"
              :transform="`translate(${glyph.x} 0)`"
              :style="{ '--hover': glyph.hover }"
              @pointerenter="lightGlyph"
              @pointerleave="dimGlyph"
            >
              <path
                v-for="(part, j) in glyph.paths"
                :key="j"
                :d="part.d"
                :fill="part.kind === 'fill' ? 'currentColor' : 'none'"
                :stroke="part.kind === 'fill' ? 'none' : 'currentColor'"
                :stroke-width="wordmarkStrokeWidth[part.kind] || 0"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </g>
          </svg>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
/**
 * 首页（用户端）。
 *
 * - 首屏使用 `ConferenceHero`，首屏以下按区块依次展示：
 *   活动回顾、即将举办、最新资讯（含分类）、我报名的活动（登录后）、大会概览、宣传海报生成、参展单位、结尾行动区、品牌字标。
 *
 * 依赖接口（部分）：
 * - GET `/activity/list`：活动封面、活动回顾、活动总数与倒计时
 * - GET `/content/articles`：资讯分类统计与最新资讯
 * - GET `/activity/my-registrations`：我的报名活动
 * - GET `/content/exhibitors`：参展单位
 * - POST `/content/poster`、GET `/content/poster/jobs/:jobId`：海报生成任务
 */
import ConferenceHero from '../components/home/ConferenceHero.vue'
import ConferenceMetrics from '../components/home/ConferenceMetrics.vue'
import ExhibitorStrip from '../components/home/ExhibitorStrip.vue'
import HomeActivities from '../components/home/HomeActivities.vue'
import HomeArticles from '../components/home/HomeArticles.vue'
import HomeSectionHead from '../components/home/HomeSectionHead.vue'
import HomeUpcoming, { type FeaturedAgendaItem } from '../components/home/HomeUpcoming.vue'
import Icon from '../components/ui/Icon.vue'
import { useProgramme } from '../components/home/programme'
import { vReveal } from '../directives/reveal'
import { ref, onMounted, computed } from 'vue'

// 字标悬停：一碰到笔画就立刻上色，离开后保持 1.2s 再淡出（再次碰到会重新计时）
const glyphTimers = new WeakMap<Element, number>()
function lightGlyph(e: PointerEvent) {
  if (e.pointerType !== 'mouse') return
  const el = e.currentTarget as Element
  window.clearTimeout(glyphTimers.get(el))
  el.classList.add('is-lit')
}
function dimGlyph(e: PointerEvent) {
  if (e.pointerType !== 'mouse') return
  const el = e.currentTarget as Element
  window.clearTimeout(glyphTimers.get(el))
  glyphTimers.set(el, window.setTimeout(() => el.classList.remove('is-lit'), 1200))
}
import { useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()

const title = ref('西湖论剑')
const subtitle = ref('数字安全大会')
const posterUrl = ref('')
const generating = ref(false)

const myActivities = ref<any[]>([])
const myActivitiesTotal = ref(0)
const myActivitiesLoading = ref(false)
const myActivitiesError = ref('')

// 页尾品牌字标：每个字母是独立的 SVG 圆润描边图形，悬停判定只落在笔画上（O/R/D 的内孔不触发）；
// hover 为该字母悬停色，从青绿到淡靛逐字渐变，相邻字母只差一小步（纯展示，无业务逻辑）
const wordmarkWords = [
  {
    width: 926,
    glyphs: [
      { ch: 'S', x: 0, hover: '#2DD4BF', paths: [{ d: 'M114 50C102 34 86 28 70 28C46 28 28 42 28 64C28 86 46 94 70 100C96 106 112 116 112 138C112 160 94 172 68 172C48 172 32 164 22 150', kind: 'stroke' }] },
      { ch: 'W', x: 164, hover: '#3CD0CB', paths: [{ d: 'M28 30C30 96 38 172 70 172C96 172 102 118 107 82C112 118 118 172 144 172C176 172 184 96 186 30', kind: 'stroke' }] },
      { ch: 'O', x: 402, hover: '#4ACDD8', paths: [{ d: 'M82 28C118 28 136 60 136 100C136 140 118 172 82 172C46 172 28 140 28 100C28 60 46 28 82 28Z', kind: 'stroke' }] },
      { ch: 'R', x: 590, hover: '#59C9E4', paths: [{ d: 'M28 172C29 120 29 72 28 28H80C106 28 122 44 122 68C122 92 106 108 80 108H30', kind: 'stroke' }, { d: 'M76 110C104 118 120 144 126 172', kind: 'stroke' }] },
      { ch: 'D', x: 768, hover: '#64C5EB', paths: [{ d: 'M28 28C29 76 29 124 28 172H74C110 172 130 140 130 100C130 60 110 28 74 28Z', kind: 'stroke' }] }
    ]
  },
  {
    width: 890,
    glyphs: [
      { ch: 'S', x: 0, hover: '#6EC0EE', paths: [{ d: 'M114 50C102 34 86 28 70 28C46 28 28 42 28 64C28 86 46 94 70 100C96 106 112 116 112 138C112 160 94 172 68 172C48 172 32 164 22 150', kind: 'stroke' }] },
      { ch: 'H', x: 164, hover: '#77BBF2', paths: [{ d: 'M28 28C29 76 29 124 28 172', kind: 'stroke' }, { d: 'M128 28C127 76 127 124 128 172', kind: 'stroke' }, { d: 'M30 100C62 94 94 94 126 100', kind: 'stroke' }] },
      { ch: 'I', x: 344, hover: '#81B8F6', paths: [{ d: 'M28 28C29 76 29 124 28 172', kind: 'stroke' }] },
      { ch: 'E', x: 424, hover: '#8DB6F8', paths: [{ d: 'M104 28H64C42 28 28 42 28 64V136C28 158 42 172 64 172H104', kind: 'stroke' }, { d: 'M30 100H92', kind: 'stroke' }] },
      { ch: 'L', x: 578, hover: '#99B5FA', paths: [{ d: 'M28 28C29 70 28 110 28 136C28 158 42 172 64 172H104', kind: 'stroke' }] },
      { ch: 'D', x: 732, hover: '#A5B4FC', paths: [{ d: 'M28 28C29 76 29 124 28 172H74C110 172 130 140 130 100C130 60 110 28 74 28Z', kind: 'stroke' }] }
    ]
  }
]
// 桌面端整行宽度（两词 + 72 单词间距），移动端以较宽的单词为基准
const wordmarkLineWidth = 1888
const wordmarkStrokeWidth: Record<string, number> = { stroke: 54 }
const wordmarkMaxWidth = 926

const isLoggedIn = computed(() => {
  return !!localStorage.getItem('token')
})

const {
  events,
  total: eventsTotal,
  loading: eventsLoading,
  error: eventsError,
  reload: reloadEvents,
  upcoming,
  ongoing,
  now
} = useProgramme()

// 近期重点大会议程（关联真实活动 ID 与 2026.11 - 2027.01 档期）
const featuredConferences = ref<FeaturedAgendaItem[]>([
  {
    id: 1,
    title: '2026西湖论剑·数字安全大会开幕式暨主论坛',
    description: '围绕大模型安全治理、智算中心防护与关键行业韧性展开年度高端交流。',
    type: '旗舰大会',
    status: 'ongoing',
    dateRange: '正在进行 · 11月26日 - 28日',
    location: '杭州国际博览中心主会场',
    scale: '主论坛 + 多场专题峰会 + 安全展区'
  },
  {
    id: 4,
    title: '智能网联汽车与低空经济安全高峰论坛',
    description: '聚焦车云协同、OTA 固件验签与低空智联通信安全标准及工程实践。',
    type: '高峰论坛',
    status: 'upcoming',
    dateRange: '即将开始 · 12月5日 09:30',
    location: '杭州国际博览中心 C 厅',
    scale: '预计 600+ 行业专家与车企代表'
  },
  {
    id: 10,
    title: '2026-2027西湖论剑·数字安全创新大赛总决赛暨颁奖典礼',
    description: '涵盖 AI 安全挑战赛与高校创新应用赛道，现场揭晓年度特等奖并举行签约仪式。',
    type: '年度收官',
    status: 'upcoming',
    dateRange: '报名中 · 2027年1月15日',
    location: '杭州国际博览中心主会场',
    scale: '总决赛答辩 + 颁奖盛典'
  }
])

function activityDateParts(dateStr?: string) {
  if (!dateStr) return { month: '时间', day: '待定' }
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return { month: '时间', day: '待定' }
  return {
    month: `${date.getMonth() + 1}月`,
    day: String(date.getDate())
  }
}

function formatDateTime(dateStr: string) {
  if (!dateStr) return '时间待定'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '时间待定'
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getActivityStatus(activity: any) {
  const current = new Date()
  const startTime = activity.startTime ? new Date(activity.startTime) : null
  const endTime = activity.endTime ? new Date(activity.endTime) : null

  if (endTime && current > endTime) {
    return { label: '已结束', type: 'info' }
  }
  if (startTime && current >= startTime && (!endTime || current <= endTime)) {
    return { label: '进行中', type: 'success' }
  }
  if (startTime && current < startTime) {
    return { label: '未开始', type: 'warning' }
  }
  return { label: '待定', type: '' }
}

function getChannelLabel(channel: string) {
  const channelMap: Record<string, string> = {
    web: '网页报名',
    app: 'APP报名',
    onsite: '现场报名',
    share_link: '分享链接',
    wechat: '微信报名',
    admin: '后台添加'
  }
  return channelMap[channel] || channel || '网页报名'
}

async function loadMyActivities() {
  if (!isLoggedIn.value) return

  myActivitiesLoading.value = true
  myActivitiesError.value = ''
  try {
    const { data } = await api.get('/activity/my-registrations', {
      params: { page: 0, size: 5 }
    })
    if (data.code === 0) {
      myActivities.value = data.data?.items || []
      myActivitiesTotal.value = data.data?.total || 0
    } else {
      myActivitiesError.value = data.message || '加载已报名活动失败，请稍后重试'
    }
  } catch (error: any) {
    if (error.response?.status !== 401) {
      console.error('Failed to load registered activities:', error)
      myActivitiesError.value = error?.message || '加载已报名活动失败，请稍后重试'
    }
  } finally {
    myActivitiesLoading.value = false
  }
}

async function generatePoster() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录后生成海报')
    router.push('/login')
    return
  }
  if (!title.value.trim()) {
    ElMessage.warning('请输入海报标题')
    return
  }

  generating.value = true
  try {
    const params = new URLSearchParams({ title: title.value, subtitle: subtitle.value })
    posterUrl.value = ''
    ElMessage.info('海报生成中，请稍候...')

    const { data: createRes } = await api.post('/content/poster', params)
    if (createRes.code !== 0) {
      throw new Error(createRes.message || '海报任务提交失败')
    }
    const jobId = createRes?.data?.jobId
    if (!jobId) {
      throw new Error('海报任务返回 jobId 为空')
    }

    const start = Date.now()
    while (Date.now() - start < 30_000) {
      const { data: statusRes } = await api.get(`/content/poster/jobs/${jobId}`)
      if (statusRes.code !== 0) {
        throw new Error(statusRes.message || '查询海报任务失败')
      }
      const status = statusRes?.data?.status
      if (status === 'SUCCESS') {
        posterUrl.value = statusRes?.data?.url || ''
        break
      }
      if (status === 'FAILED') {
        throw new Error(statusRes?.data?.errorMessage || '海报生成失败')
      }
      await new Promise((r) => setTimeout(r, 1000))
    }
    if (!posterUrl.value) {
      throw new Error('海报生成超时，请稍后重试')
    }
    ElMessage.success('海报生成成功')
  } catch (error: any) {
    ElMessage.error(error?.message || '生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

function openPoster() {
  if (!posterUrl.value) return
  window.open(posterUrl.value, '_blank')
}

async function copyPosterUrl() {
  if (!posterUrl.value) return
  try {
    await navigator.clipboard.writeText(posterUrl.value)
    ElMessage.success('已复制链接')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

onMounted(() => {
  if (isLoggedIn.value) {
    loadMyActivities()
  }
})
</script>

<style scoped>
.home-page {
  margin: -24px -16px -40px;
  padding-bottom: 80px;
  background: var(--ss-bg);
  color: #151515;
  overflow-x: clip;
}

.home-page ul {
  list-style: none;
}

/* 首屏以下版心与区块节奏 */
.home-below {
  display: flex;
  flex-direction: column;
  gap: 64px;
  width: 100%;
  max-width: 1040px;
  margin-inline: auto;
  padding: 40px 20px 0;
}

.home-section {
  min-width: 0;
}

/* 克制进入动画：透明度渐入 + 12px 位移，.6s */
.home-reveal {
  transition: opacity 0.6s ease-out, transform 0.6s var(--ss-ease);
}

.home-reveal.awaiting-reveal:not(:focus-within) {
  opacity: 0;
  transform: translateY(12px);
}

@media (prefers-reduced-motion: reduce) {
  .home-reveal.awaiting-reveal:not(:focus-within) {
    opacity: 1;
    transform: none;
  }
}

/* ================= 我报名的活动（与即将举办一致的横条卡片） ================= */
.mine-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.mine-card {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #fff;
  color: #151515;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.mine-card:hover {
  border-color: rgba(21, 21, 21, 0.16);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.mine-date-box {
  width: 48px;
  height: 48px;
  border-color: rgba(21, 21, 21, 0.1);
}

.mine-date-day {
  font-size: 16px;
  font-weight: 600;
  color: #151515;
}

.mine-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.mine-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.mine-live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #14b8a6;
  flex-shrink: 0;
}

.mine-title {
  min-width: 0;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.4;
  color: #151515;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-meta {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.4;
  color: #737373;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 20px 24px;
  border-radius: 14px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #fff;
  font-size: 14px;
  color: #737373;
}

.mine-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 16px;
  border-radius: 10px;
  border: 1px solid rgba(21, 21, 21, 0.14);
  background: #fff;
  color: #151515;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.mine-btn:hover {
  border-color: rgba(21, 21, 21, 0.28);
  background: rgba(21, 21, 21, 0.03);
}

.mine-btn--dark {
  border-color: #151515;
  background: #151515;
  color: #fff;
}

.mine-btn--dark:hover {
  border-color: #2b2b2b;
  background: #2b2b2b;
}

.skeleton-card {
  pointer-events: none;
}

.sk-date-box {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  flex-shrink: 0;
}

.sk-line-1 {
  width: 80%;
  height: 16px;
}

.sk-line-2 {
  width: 60%;
  height: 13px;
  margin-top: 8px;
}

/* ================= 宣传海报生成（白卡片 + 细描边） ================= */
.poster-panel {
  padding: 24px 28px;
  border-radius: 16px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.poster-panel:hover {
  border-color: rgba(21, 21, 21, 0.16);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.poster-desc {
  font-size: 14px;
  line-height: 1.6;
  color: #737373;
}

.poster-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  align-items: end;
  gap: 16px;
  margin-top: 18px;
}

.poster-form :deep(.el-form-item) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  margin: 0;
}

.poster-form :deep(.el-form-item__label) {
  justify-content: flex-start;
  height: auto;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  color: #151515;
}

.poster-form :deep(.el-input__wrapper) {
  min-height: 40px;
  background: #fff;
}

.poster-submit {
  height: 40px;
}

.poster-panel :deep(.el-button--primary) {
  border-radius: 10px;
  --el-button-bg-color: #151515;
  --el-button-border-color: #151515;
  --el-button-text-color: #ffffff;
  --el-button-hover-bg-color: #2a2a2a;
  --el-button-hover-border-color: #2a2a2a;
  --el-button-hover-text-color: #ffffff;
  --el-button-active-bg-color: #151515;
  --el-button-active-border-color: #151515;
  --el-button-active-text-color: #ffffff;
  background-color: #151515;
  border-color: #151515;
  color: #ffffff;
}

.poster-panel :deep(.el-button--primary:hover),
.poster-panel :deep(.el-button--primary:focus-visible) {
  background-color: #2a2a2a;
  border-color: #2a2a2a;
  color: #ffffff;
}

.poster-result {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.poster-result-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.poster-result-actions .el-button {
  margin: 0;
}

/* ================= 结尾行动区 ================= */
.closing-cta {
  width: 100%;
  max-width: 1040px;
  margin-inline: auto;
  padding: 112px 20px 48px;
  text-align: center;
}

.closing-title {
  display: inline-block;
  max-width: 100%;
  margin: 0;
  font-family: 'Noto Sans SC', var(--ss-font);
  font-size: 40px;
  font-weight: 500;
  line-height: 1.3;
  letter-spacing: -0.01em;
  color: #151515;
}

.closing-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.closing-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  padding: 0 22px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  white-space: nowrap;
  transition: border-color 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}

.closing-btn--outline {
  border: 1px solid rgba(21, 21, 21, 0.16);
  background: #fff;
  color: #151515;
}

.closing-btn--outline:hover {
  border-color: rgba(21, 21, 21, 0.32);
  background: rgba(21, 21, 21, 0.03);
}

.closing-btn--dark {
  border: 1px solid #151515;
  background: #151515;
  color: #fff;
}

.closing-btn--dark:hover {
  border-color: #2b2b2b;
  background: #2b2b2b;
}

/* ================= 页尾字标（保留） ================= */
.page-width {
  width: min(calc(100% - 64px), 1192px);
  margin-inline: auto;
}
.brand-wordmark {
  margin: 96px 0 -80px;
  padding-block: 72px 64px;
  background: var(--ss-navy);
  overflow: hidden;
}
.wordmark {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.wordmark-word {
  display: block;
  /* 按整行宽度等比分配，两词之间留出单词间距 */
  width: calc(var(--word-w) / var(--line-w) * 100%);
  height: auto;
  overflow: visible;
}
/* 只有字母的实心部分（填充/描边）响应鼠标，字母之间、O/D/R 的内孔、L 拐角的空白都不触发 */
.wordmark-glyph {
  color: #fff;
  transition: color .9s cubic-bezier(.22, .61, .36, 1);
}
@media (hover: hover) {
  .wordmark-glyph.is-lit {
    color: var(--hover, var(--ss-teal));
    transition-duration: .4s;
  }
}
@media (prefers-reduced-motion: reduce) {
  .wordmark-glyph {
    transition-duration: 0s;
  }
}

/* ================= 响应式 ================= */
@media (max-width: 960px) {
  .mine-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .home-page {
    margin: -16px -12px -32px;
    padding-bottom: 48px;
  }
  .page-width {
    width: calc(100% - 40px);
  }
  .brand-wordmark {
    margin: 56px 0 -48px;
    padding-block: 40px 36px;
  }
  .wordmark {
    flex-direction: column;
    gap: 10px;
  }
  /* 两行使用同一缩放比例，字母大小一致；较宽的单词占容器约 70%，避免手机上过大 */
  .wordmark-word {
    width: calc(var(--word-w) / var(--max-w) * 70%);
  }
}

@media (max-width: 640px) {
  .home-below {
    gap: 48px;
    padding-top: 28px;
  }

  .mine-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .poster-panel {
    padding: 20px;
  }

  .poster-form {
    grid-template-columns: minmax(0, 1fr);
  }

  .poster-result {
    flex-direction: column;
  }

  .closing-title {
    font-size: 28px;
  }
}
</style>
