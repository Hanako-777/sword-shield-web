<template>
  <div class="activity-page" :style="{ backgroundColor: tint }">
    <ReadState v-if="loading || !item" class="initial-state" :loading="loading" :error="error" empty="暂无活动" @retry="load" />
    <div v-else class="layout">
      <aside class="identity">
        <ActivityCover :src="item.coverUrl" :title="item.title" @tint="tint = $event" />
        <div class="identity-info">
          <div v-if="item.speaker" class="speaker"><Icon name="user" :size="18" /><div><small>嘉宾</small><strong>{{ item.speaker }}</strong></div></div>
          <div class="tags"><span class="ss-badge">活动</span><span v-if="item.heat != null" class="ss-badge">热度 {{ item.heat }}</span></div>
        </div>
      </aside>
      <div class="details">
        <header class="details-head">
          <h1>{{ item.title }}</h1>
          <div class="fact">
            <div class="ss-date-block" aria-hidden="true"><small>{{ startDate ? startDate.getMonth() + 1 + '月' : '—' }}</small><b>{{ startDate?.getDate() || '—' }}</b></div>
            <div><strong>{{ startDate ? startDate.toLocaleDateString('zh-CN', {year:'numeric',month:'long',day:'numeric',weekday:'long'}) : '待定' }}</strong>
              <p>开始 {{ timeOf(startMs) }} <span aria-hidden="true">—</span> 结束 {{ endLabel }}</p></div>
          </div>
          <div class="fact"><span class="fact-icon"><Icon name="map-pin" :size="18" /></span><strong>{{ item.location || '地点待定' }}</strong></div>
        </header>
        <section v-reveal class="registration reveal">
          <div class="section-title"><h2>活动报名</h2><span class="ss-badge"><i class="ss-dot" :class="{ 'ss-dot--live': phase === '进行中' }" />{{ phase }}</span></div>
          <div v-if="item.maxRegistrations != null || item.currentRegistrations != null" class="capacity">
            <span v-if="item.maxRegistrations != null">限额 <b>{{ item.maxRegistrations }}</b> 人</span>
            <span v-if="item.currentRegistrations != null">已报名 <b>{{ item.currentRegistrations }}</b> 人</span>
            <span v-if="remaining != null">余量 <b>{{ remaining }}</b></span>
          </div>
          <el-button class="primary-action" type="primary" @click="register" :loading="registering" :disabled="!canRegister || registered">
            {{ registered ? '已报名' : '立即报名' }}
          </el-button>
          <div class="other-actions">
            <el-button @click="activityCheckin" :loading="checkingIn" :disabled="!canCheckin || checkedIn">{{ checkedIn ? '已签到' : '活动签到' }}</el-button>
            <router-link to="/payment" class="touch-link buy-link">购票</router-link>
            <el-button @click="share" :loading="sharing"><Icon name="share" :size="15" style="margin-right: 4px" />分享活动</el-button>
          </div>
          <div v-if="registerHint || checkinHint" class="hints"><p v-if="registerHint">{{ registerHint }}</p><p v-if="checkinHint">{{ checkinHint }}</p></div>
          <div v-if="item.liveUrl || item.replayUrl" class="broadcast">
            <h3>直播与回放</h3>
            <button v-if="item.liveUrl" type="button" :disabled="!liveEnabled" @click="enterLive(item.liveUrl)">
              <Icon name="video" :size="18" /><span><b>{{ liveEnabled ? '直播中' : '进入直播' }}</b><small>点击进入直播间观看精彩内容</small></span>
              <strong>{{ liveEnabled ? 'LIVE · 进入直播' : '进入直播' }}</strong>
            </button>
            <button v-if="item.replayUrl" type="button" :disabled="!replayEnabled" @click="enterReplay(item.replayUrl)">
              <Icon name="play-circle" :size="18" /><span><b>视频回放</b><small>观看往期精彩录播内容</small></span><strong>观看回放</strong>
            </button>
          </div>
        </section>
        <section v-if="item.summary" v-reveal class="prose reveal"><h2>活动详情</h2><p>{{ item.summary }}</p></section>
        <ReadState v-if="error" :error="error" @retry="load" />
      <section v-reveal class="interaction-section reveal">
        <div class="section-title">
          <h3>
            <Icon name="message-circle" :size="18" class="heading-icon" />
            评论区
          </h3>
          <el-button size="small" @click="like" :loading="liking" :type="liked ? 'primary' : ''">
            <span>
              <Icon name="star" :size="14" :filled="liked" class="inline-icon" />
              {{ liked ? '已点赞' : '点赞' }} {{ likeCount }}
            </span>
          </el-button>
        </div>

        <div class="comment-form">
          <el-input 
            ref="commentInputRef"
            v-model="comment" 
            type="textarea" 
            :rows="3"
            placeholder="写下你的想法..."
            maxlength="500"
            show-word-limit
          />
          <el-button 
            type="primary" 
            @click="postComment" 
            :loading="commenting"
            style="margin-top: 12px;"
          >
            发表评论
          </el-button>
        </div>

        <div class="comments-list">
          <ReadState v-if="commentsLoading || commentsError" :loading="commentsLoading" :error="commentsError" @retry="loadComments" />
          <div v-else-if="comments.length === 0" class="empty-comments">
            <el-empty description="还没有评论，快来发表第一条吧！" :image-size="96">
              <el-button type="primary" @click="focusCommentBox">去评论</el-button>
            </el-empty>
          </div>
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-avatar">
              <Icon name="user-circle" :size="20" />
            </div>
            <div class="comment-content">
              <div class="comment-text">{{ c.content }}</div>
              <div class="comment-actions">
                <el-button size="small" @click="likeComment(c)" :loading="c._liking" :type="c.isLiked ? 'primary' : ''">
                  <Icon name="star" :size="14" :filled="!!c.isLiked" class="inline-icon" />
                  {{ c.isLiked ? '已点赞' : '点赞' }} {{ c.likeCount ?? 0 }}
                </el-button>
              </div>
              <div class="comment-time">{{ formatDate(c.createdAt) }}</div>
            </div>
          </div>
        </div>
      </section>
      </div>
      <nav class="mobile-action" aria-label="活动操作">
        <el-button v-if="canRegister && !registered" type="primary" :loading="registering" @click="register">立即报名</el-button>
        <el-button v-else-if="canCheckin && !checkedIn" type="primary" :loading="checkingIn" @click="activityCheckin">活动签到</el-button>
        <el-button v-else-if="liveEnabled" type="primary" @click="enterLive(item.liveUrl)">进入直播</el-button>
        <el-button v-else-if="replayEnabled" type="primary" @click="enterReplay(item.replayUrl)">观看回放</el-button>
        <router-link v-else to="/payment" class="touch-link dock-buy">购票</router-link>
      </nav>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 活动详情页。
 *
 * - 展示活动基本信息（标题、时间、地点、限额、直播/回放链接）。
 * - 提供报名、活动签到、分享、评论与点赞等互动能力。
 *
 * 依赖接口（部分）：
 * - GET `/activity/detail`
 * - GET `/activity/registration/status`、POST `/activity/register`
 * - GET `/user/activity-checkin/status`、POST `/user/activity-checkin`
 * - GET `/interaction/comments`、POST `/interaction/comment`
 * - POST `/interaction/like`、GET `/interaction/likes/count`、POST `/interaction/share`
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { ElMessage } from 'element-plus'

import ReadState from '../components/detail/ReadState.vue'
import { useReadState } from '../components/detail/useReadState'
import { vReveal } from '../directives/reveal'
import ActivityCover from '../components/detail/ActivityCover.vue'
import Icon from '../components/ui/Icon.vue'

const { loading, error, run: readPage } = useReadState()
const { loading: commentsLoading, error: commentsError, run: readComments } = useReadState()
function load() {
  return readPage(fetchDetail, '加载失败，请稍后重试')
}
function loadComments() {
  return readComments(fetchComments, '评论加载失败')
}
const tint = ref('#F4F6F8')
const route = useRoute()
const router = useRouter()
const item = ref<any>(null)
const comments = ref<any[]>([])
const comment = ref('')
const likeCount = ref(0)
const liked = ref(false)
const registering = ref(false)
const commenting = ref(false)
const liking = ref(false)
const sharing = ref(false)
const commentInputRef = ref<any>(null)
const checkingIn = ref(false)
const checkedIn = ref(false)
const canCheckin = ref(false)
const checkinHint = ref('')
const canRegister = ref(true)
const registerHint = ref('')
const registered = ref(false)

const nowMs = ref(Date.now())
let nowTimer: number | null = null

const startMs = computed(() => {
  const t = item.value?.startTime
  const ms = t ? new Date(t).getTime() : NaN
  return Number.isFinite(ms) ? ms : null
})

const endMs = computed(() => {
  const t = item.value?.endTime
  const ms = t ? new Date(t).getTime() : NaN
  return Number.isFinite(ms) ? ms : null
})

const liveEnabled = computed(() => {
  if (!item.value?.liveUrl) return false
  // 严格按时间窗：开始后（含）且结束前（不含）可进入直播
  if (startMs.value != null && nowMs.value < startMs.value) return false
  if (endMs.value != null && nowMs.value >= endMs.value) return false
  return true
})

const replayEnabled = computed(() => {
  if (!item.value?.replayUrl) return false
  // 结束后（含）才可观看回放；若未配置结束时间，则保持可用（向后兼容）
  if (endMs.value == null) return true
  return nowMs.value >= endMs.value
})

const startDate = computed(() => startMs.value == null ? null : new Date(startMs.value))
function timeOf(ms: number | null) {
  return ms == null ? '待定' : new Date(ms).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
}
const endLabel = computed(() => {
  if (endMs.value == null) return '待定'
  const date = new Date(endMs.value)
  return date.toDateString() === startDate.value?.toDateString()
    ? timeOf(endMs.value) : date.toLocaleDateString('zh-CN') + ' ' + timeOf(endMs.value)
})
const remaining = computed(() => {
  const { maxRegistrations, currentRegistrations } = item.value || {}
  return maxRegistrations == null || currentRegistrations == null ? null : Math.max(0, maxRegistrations - currentRegistrations)
})
const phase = computed(() => {
  if (endMs.value != null && nowMs.value >= endMs.value) return '已结束'
  if (startMs.value == null) return '待定'
  return nowMs.value < startMs.value ? '未开始' : '进行中'
})

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

async function focusCommentBox() {
  await nextTick()
  commentInputRef.value?.focus?.()
}

function openLink(url: string) {
  if (url) {
    window.open(url, '_blank')
  }
}

function enterLive(url: string) {
  if (!url) return
  if (!liveEnabled.value) return
  router.push(`/activity/${route.params.id}/live?mode=live`)
}

function enterReplay(url: string) {
  if (!url) return
  if (!replayEnabled.value) return
  router.push(`/activity/${route.params.id}/live?mode=replay`)
}

async function fetchDetail() {
  const { data } = await api.get('/activity/detail', { params: { id: route.params.id } })
  if (data.code !== 0) throw new Error(data.message || '活动加载失败')
  if (!data.data) { item.value = null; return }
  item.value = data.data
  computeRegisterWindow()
  computeCheckinWindow()
  await loadRegistrationStatus()
  await loadCheckinStatus()

  const targetId = Number(route.params.id)
  if (!Number.isFinite(targetId)) return

  const token = localStorage.getItem('token')
  if (token) {
    const stateRes = await api.get('/interaction/likes/state', {
      params: { targetType: 'activity', targetId },
    })
    if (stateRes.data.code === 0) {
      likeCount.value = stateRes.data.data.count
      liked.value = !!stateRes.data.data.liked
      return
    }
    liked.value = false
  }

  const likeCountRes = await api.get('/interaction/likes/count', {
    params: { targetType: 'activity', targetId },
  })
  if (likeCountRes.data.code === 0) {
    likeCount.value = likeCountRes.data.data.count
  }
}

function computeCheckinWindow() {
  checkinHint.value = ''
  canCheckin.value = false
  if (!item.value) return
  const now = Date.now()
  const startMs = item.value.startTime ? new Date(item.value.startTime).getTime() : null
  const endMs = item.value.endTime ? new Date(item.value.endTime).getTime() : null

  if (startMs != null && now < startMs) {
    checkinHint.value = '活动未开始，暂不可签到'
    return
  }
  if (endMs != null && now > endMs) {
    checkinHint.value = '活动已结束，不可签到'
    return
  }
  canCheckin.value = true
}

function computeRegisterWindow() {
  registerHint.value = ''
  canRegister.value = false
  if (!item.value) return
  const now = Date.now()
  const startMs = item.value.startTime ? new Date(item.value.startTime).getTime() : null
  const endMs = item.value.endTime ? new Date(item.value.endTime).getTime() : null

  if (endMs != null && now > endMs) {
    registerHint.value = '活动已结束，报名已截止'
    return
  }
  if (startMs != null && now >= startMs) {
    registerHint.value = '活动已开始，报名已截止'
    return
  }
  canRegister.value = true
}

async function loadCheckinStatus() {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    const { data } = await api.get('/user/activity-checkin/status', {
      params: { activityId: Number(route.params.id) }
    })
    if (data.code === 0) {
      checkedIn.value = !!data.data.checkedIn
    }
  } catch {
    // ignore
  }
}

async function loadRegistrationStatus() {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    const { data } = await api.get('/activity/registration/status', {
      params: { activityId: Number(route.params.id) }
    })
    if (data.code === 0) {
      registered.value = !!data.data.registered
    }
  } catch {
    // ignore
  }
}

async function register() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!canRegister.value) {
    ElMessage.warning(registerHint.value || '当前不可报名')
    return
  }
  if (registered.value) return
  
  registering.value = true
  try {
    const { data } = await api.post('/activity/register', { 
      activityId: Number(route.params.id), 
      channel: 'web' 
    })
    if (data.code === 0) {
      registered.value = true
      if (typeof item.value?.currentRegistrations === 'number') item.value.currentRegistrations++
      ElMessage.success('报名成功！')
    } else {
      if (typeof data.message === 'string' && data.message.includes('已报名')) {
        registered.value = true
      }
      ElMessage.error(data.message || '报名失败')
    }
  } catch (error) {
    ElMessage.error('报名失败，请稍后重试')
  } finally {
    registering.value = false
  }
}

async function activityCheckin() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!canCheckin.value) {
    ElMessage.warning(checkinHint.value || '当前不可签到')
    return
  }
  if (checkedIn.value) return

  checkingIn.value = true
  try {
    const { data } = await api.post('/user/activity-checkin', { activityId: Number(route.params.id) })
    if (data.code === 0) {
      checkedIn.value = true
      ElMessage.success('签到成功！')
    } else {
      ElMessage.error(data.message || '签到失败')
    }
  } catch (error) {
    ElMessage.error('签到失败，请稍后重试')
  } finally {
    checkingIn.value = false
  }
}

async function fetchComments() {
  const { data } = await api.get('/interaction/comments', { 
    params: { 
      targetType: 'activity', 
      targetId: Number(route.params.id), 
      page: 0, 
      size: 10 
    } 
  })
  if (data.code !== 0) throw new Error(data.message || '评论加载失败')
  if (data.code === 0) {
    comments.value = data.data.items
    const token = localStorage.getItem('token')
    await Promise.all(comments.value.map(async (c: any) => {
      c._liking = false
      c.isLiked = false
      if (token) {
        const { data: state } = await api.get('/interaction/likes/state', { params: { targetType: 'comment', targetId: c.id } })
        if (state?.code === 0) {
          c.likeCount = state?.data?.count ?? 0
          c.isLiked = !!state?.data?.liked
          return
        }
      }
      const { data: cnt } = await api.get('/interaction/likes/count', { params: { targetType: 'comment', targetId: c.id } })
      c.likeCount = cnt?.data?.count ?? 0
    }))
  }
}

async function postComment() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (!comment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  commenting.value = true
  try {
    const { data } = await api.post('/interaction/comment', { 
      targetType: 'activity', 
      targetId: Number(route.params.id), 
      content: comment.value 
    })
    if (data.code === 0) {
      comment.value = ''
      ElMessage.success('评论成功')
      await loadComments()
    } else {
      ElMessage.error(data.message || '评论失败')
    }
  } catch (error) {
    ElMessage.error('评论失败，请稍后重试')
  } finally {
    commenting.value = false
  }
}

async function like() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // Prevent multiple simultaneous clicks
  if (liking.value) return
  
  liking.value = true
  try {
    const targetId = Number(route.params.id)
    if (!Number.isFinite(targetId)) {
      ElMessage.error('活动 ID 无效')
      return
    }

    if (liked.value) {
      const { data } = await api.post('/interaction/unlike', { targetType: 'activity', targetId })
      if (data.code === 0) {
        likeCount.value = data.data.count
        liked.value = false
        ElMessage.success('已取消点赞')
      } else {
        ElMessage.error(data.message || '操作失败')
      }
      return
    }

    const { data } = await api.post('/interaction/like', { targetType: 'activity', targetId })
    if (data.code === 0) {
      likeCount.value = data.data.count
      liked.value = true
      ElMessage.success('点赞成功')
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    liking.value = false
  }
}

async function share() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  sharing.value = true
  try {
    const url = window.location.href
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(url)
    }
    // 仅复制当前页面链接到剪贴板
    ElMessage.success('链接已复制')

  } catch (error) {
    ElMessage.error('分享失败，请稍后重试')
  } finally {
    sharing.value = false
  }
}

async function likeComment(c: any) {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // Prevent multiple simultaneous clicks
  if (c._liking) return
  
  c._liking = true
  try {
    if (c.isLiked) {
      const { data } = await api.post('/interaction/unlike', { targetType: 'comment', targetId: c.id })
      if (data.code === 0) {
        c.likeCount = data.data.count
        c.isLiked = false
        ElMessage.success('已取消点赞')
      } else {
        ElMessage.error(data.message || '操作失败')
      }
      return
    }

    const { data } = await api.post('/interaction/like', { targetType: 'comment', targetId: c.id })
    if (data.code === 0) {
      c.likeCount = data.data.count
      c.isLiked = true
      ElMessage.success('点赞成功')
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    c._liking = false
  }
}

onMounted(() => {
  load()
  loadComments()
  nowTimer = window.setInterval(() => {
    nowMs.value = Date.now()
    // 报名/签到按钮也依赖时间窗，顺带刷新
    computeRegisterWindow()
    computeCheckinWindow()
  }, 30_000)
})

onBeforeUnmount(() => {
  if (nowTimer != null) {
    window.clearInterval(nowTimer)
    nowTimer = null
  }
})
</script>

<style scoped src="../components/detail/detail.css"></style>
<style scoped>
.activity-page { margin: -24px -16px -40px; padding: 48px 24px 80px; min-height: 72vh; font-family: var(--ss-font); color: var(--ss-ink); }
.layout { max-width: 1040px; margin: auto; display: grid; grid-template-columns: minmax(0,.8fr) minmax(0,1.3fr); gap: 64px; align-items: start; }
.identity { position: sticky; top: calc(var(--ss-header-h) + 20px); }
.identity-info { display: grid; gap: 16px; padding: 20px 4px; }
.speaker, .tags, .fact, .section-title, .capacity, .other-actions, .comment-item { display: flex; align-items: center; gap: 12px; }
.speaker small, .fact p, .hints, .comment-time, .broadcast small { display: block; color: var(--ss-muted); font-size: 14px; }
.speaker strong { font-weight: 550; overflow-wrap: anywhere; }
.details { min-width: 0; }
h1 { color: var(--ss-navy); font-size: 40px; font-weight: 600; letter-spacing: -.02em; line-height: 1.22; margin-bottom: 24px; }
h2, .section-title h3 { color: var(--ss-navy); font-size: 22px; font-weight: 600; }
.fact { margin-top: 14px; }
.fact strong { font-size: 16px; font-weight: 600; }
.fact-icon, .ss-date-block { flex: 0 0 46px; }
.fact-icon { height: 46px; border: 1px solid var(--ss-line-strong); border-radius: 12px; display: grid; place-items: center; background: #fff; }
.ss-date-block small { width: 100%; background: var(--ss-fill); text-align: center; font-size: 11px; }
.ss-date-block b { font-size: 20px; }
.registration { margin: 28px 0; padding: 24px; border: 1px solid var(--ss-line-strong); background: #fff; border-radius: 24px; }
.section-title { justify-content: space-between; }
.capacity { flex-wrap: wrap; gap: 6px 22px; margin-top: 14px; color: var(--ss-muted); font-size: 14px; }
.capacity b { color: var(--ss-navy); }
.primary-action { width: 100%; min-height: 46px; margin-top: 20px; }
.other-actions { flex-wrap: wrap; gap: 8px; margin: 10px 0; }
.other-actions .el-button { margin: 0; }
.buy-link, .dock-buy { display: inline-flex; align-items: center; justify-content: center; min-height: 36px; padding: 0 18px; background: var(--ss-fill); border-radius: 10px; color: var(--ss-navy); font-weight: 600; }
.hints { font-size: 13px; line-height: 1.7; }
.broadcast { border-top: 1px solid var(--ss-line); margin-top: 18px; padding-top: 14px; }
.broadcast h3 { font-size: 16px; }
.broadcast button { display: flex; align-items: center; gap: 10px; text-align: left; width: 100%; border: 0; background: none; padding: 12px 0; color: var(--ss-navy); cursor: pointer; }
.broadcast button:disabled { color: var(--ss-muted); cursor: default; }
.broadcast strong { margin-left: auto; font-size: 13px; }
.broadcast small { font-size: 12px; }
.prose, .interaction-section { margin-top: 32px; }
.prose p, .comment-text { white-space: pre-wrap; overflow-wrap: anywhere; }
.prose p { margin-top: 12px; font-size: 17px; line-height: 1.8; }
.interaction-section .section-title { margin-bottom: 16px; }
.comment-item { align-items: start; border-top: 1px solid var(--ss-line-strong); padding: 18px 0; }
.comment-avatar { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 50%; background: #fff; }
.comment-content { flex: 1; min-width: 0; }
.comment-actions { margin: 6px 0; }
.comments-list { margin-top: 20px; }
.initial-state { max-width: 960px; margin: 0 auto; }
.mobile-action { display: none; }
@media (max-width: 1200px) { .layout { gap: 42px; } }
@media (max-width: 960px) { .layout { gap: 26px; grid-template-columns: minmax(0,.7fr) minmax(0,1.3fr); } }
@media (max-width: 768px) { .activity-page { margin: -16px -12px -32px; } }
@media (max-width: 640px) {
  .activity-page { padding: 20px 16px calc(95px + env(safe-area-inset-bottom)); }
  .layout { display: block; }
  .identity { position: static; }
  .identity-info { padding: 16px 0 22px; }
  h1 { font-size: 32px; }
  .registration { padding: 18px; }
  .mobile-action { position: fixed; bottom: 0; inset-inline: 0; z-index: 20; display: flex; background: #fff;
    border-top: 1px solid var(--ss-line-strong); padding: 10px 150px max(12px, env(safe-area-inset-bottom)) 16px; }
  .mobile-action > * { flex: 1; min-height: 46px; }
  .dock-buy { background: var(--ss-navy); color: white; }
  .buy-link { min-height: 44px; }
}
@media (max-width: 480px) { .mobile-action { padding-right: 80px; } }
</style>
