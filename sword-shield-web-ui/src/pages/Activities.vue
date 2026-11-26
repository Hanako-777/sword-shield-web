<template>
  <div class="activities-container" :class="{ 'is-minimal': minimal, 'has-timeline': showTimeline }">
    <template v-if="!minimal">
      <div ref="stickySentinel" class="sticky-sentinel" aria-hidden="true" />
      <div ref="dockRef" class="sticky-dock" :class="{ 'is-stuck': isStuck }" :style="{ '--sbw': sbw + 'px' }">
        <header class="dock-head">
          <h1 class="dock-title">活动日程</h1>
          <div class="sort-chips" role="group" aria-label="排序方式">
            <button
              v-for="opt in orderOptions"
              :key="opt.value"
              type="button"
              class="sort-btn"
              :class="{ 'is-active': orderBy === opt.value }"
              :aria-pressed="orderBy === opt.value"
              @click="setOrder(opt.value)"
            >{{ opt.label }}</button>
          </div>
        </header>

        <ActivityTimeline
          v-if="showTimeline"
          :items="sortedItems"
          :active-id="focusedActivityId"
          @select="scrollToActivity"
        />
      </div>

      <div v-if="loading && !sortedItems.length" class="schedule" aria-busy="true">
        <div v-for="n in 4" :key="n" class="event-row skeleton-row">
          <div class="ss-skeleton event-cover" />
          <div class="event-text">
            <div class="ss-skeleton" style="width: 30%; height: 14px" />
            <div class="ss-skeleton" style="width: 70%; height: 20px; margin-top: 10px" />
            <div class="ss-skeleton" style="width: 50%; height: 14px; margin-top: 10px" />
          </div>
        </div>
      </div>

      <el-empty v-else-if="!sortedItems.length" description="暂无活动" />

      <div v-else class="schedule">
        <section v-for="group in groups" :key="group.key" class="day">
          <h2 v-if="group.label" class="day-head">
            <span class="day-date">{{ group.label }}</span>
            <span class="day-week">{{ group.week }}</span>
          </h2>
          <ul class="day-list">
            <li
              v-for="entry in group.items"
              :key="entry.item.id"
              :data-activity-id="entry.item.id"
              :class="{ 'is-focused': showTimeline && focusedActivityId === entry.item.id }"
              :ref="(el) => bindCardEl(entry.item.id, el)"
            >
              <router-link :to="`/activity/${entry.item.id}`" class="event-row ss-row">
                <img class="event-cover" :src="getCoverImage(entry.item, entry.index)" :alt="entry.item.title" loading="lazy" />
                <div class="event-text">
                  <div class="event-time">
                    <span class="ss-dot" :class="dotClass(entry.item)" aria-hidden="true" />
                    <span>{{ orderBy === 'heat' ? formatDate(entry.item.startTime) + ' ' : '' }}{{ formatTime(entry.item.startTime) }}</span>
                    <span v-if="statusOf(entry.item) === 'live'" class="ss-badge ss-badge--live">进行中</span>
                    <span v-else-if="statusOf(entry.item) === 'ended'" class="ss-badge">已结束</span>
                  </div>
                  <h3 class="event-title">{{ entry.item.title }}</h3>
                  <p class="event-summary">{{ entry.item.summary || '暂无描述' }}</p>
                  <div class="event-meta">
                    <span class="meta-item">
                      <Icon name="map-pin" :size="14" class="meta-icon" />
                      <span>{{ entry.item.location || '待定' }}</span>
                    </span>
                    <span v-if="entry.item.heat" class="meta-item">
                      <Icon name="zap" :size="14" class="meta-icon heat-icon" />
                      <span>{{ entry.item.heat }}</span>
                    </span>
                    <span class="meta-more">查看详情 →</span>
                  </div>
                </div>
              </router-link>
            </li>
          </ul>
        </section>

        <div v-if="orderBy === 'time' && allEnded" class="today-tail" role="status">
          <span class="today-tail-dot" aria-hidden="true" />
          <span>今天 · 暂无后续活动</span>
        </div>
      </div>
    </template>

    <ul v-else class="minimal-list">
      <li v-for="(item, index) in shownItems" :key="item.id" class="minimal-item">
        <a :href="`/activity/${item.id}`" @click.prevent="$router.push(`/activity/${item.id}`)" class="minimal-link">
          <img class="minimal-thumb" :src="getCoverImage(item, index)" alt="" loading="lazy" />
          <span class="minimal-text">
            <span class="minimal-meta">
              <span class="ss-dot" :class="dotClass(item)" aria-hidden="true" />
              {{ formatDate(item.startTime) }} {{ formatTime(item.startTime) }}
            </span>
            <span class="title">{{ item.title }}</span>
            <span class="minimal-loc">{{ item.location || '待定' }}</span>
          </span>
          <span v-if="item.heat && item.heat > 50" class="minimal-heat" :aria-label="`热度 ${item.heat}`">
            <Icon name="zap" :size="14" class="heat-icon" />
          </span>
        </a>
      </li>
    </ul>

    <div v-if="!minimal && total > pageSize" class="pagination-wrapper">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 活动列表页面 / 组件。
 *
 * - `minimal` 模式用于精简列表场景。
 * - 默认模式展示活动卡片、按时间升序联动通栏时间轴（含里程表日期滚动），支持按时间/热度排序与分页。
 *
 * 依赖接口：
 * - GET `/activity/list`：分页获取已发布活动列表。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type ComponentPublicInstance } from 'vue'
import api from '../api'
import ActivityTimeline from '../components/activities/ActivityTimeline.vue'
import Icon from '../components/ui/Icon.vue'

const props = defineProps<{ minimal?: boolean; limit?: number }>()

const items = ref<any[]>([])
const orderBy = ref('time')
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const loading = ref(false)
const focusedActivityId = ref<number | null>(null)
const stickySentinel = ref<HTMLElement | null>(null)
const dockRef = ref<HTMLElement | null>(null)
const isStuck = ref(false)
const sbw = ref(0)

const cardEls = new Map<number, HTMLElement>()
let stickyObserver: IntersectionObserver | null = null
let sbwObserver: ResizeObserver | null = null
let scrollRaf = 0
let animScrollRaf = 0
let programmaticLockUntil = 0
let wheelStepLockUntil = 0
let wheelAccum = 0
let wheelResetTimer: number | undefined
let userScrolled = false
let hasAutoScrolled = false

const orderOptions = [
  { label: '按时间', value: 'time' },
  { label: '按热度', value: 'heat' }
]

/** 按时间排序时按开始时间升序；按热度保持接口顺序 */
const sortedItems = computed(() => {
  if (orderBy.value !== 'time') return items.value
  return [...items.value].sort((a, b) => {
    const ta = a.startTime ? Date.parse(a.startTime) : Number.MAX_SAFE_INTEGER
    const tb = b.startTime ? Date.parse(b.startTime) : Number.MAX_SAFE_INTEGER
    return ta - tb
  })
})

const shownItems = computed(() => (props.limit ? sortedItems.value.slice(0, props.limit) : sortedItems.value))
const showTimeline = computed(
  () =>
    !props.minimal &&
    orderBy.value === 'time' &&
    sortedItems.value.some((item) => item.startTime && Number.isFinite(Date.parse(item.startTime)))
)

const allEnded = computed(() => {
  if (!sortedItems.value.length) return false
  return sortedItems.value.every((item) => statusOf(item) === 'ended')
})

function setOrder(value: string) {
  if (orderBy.value === value) return
  orderBy.value = value
  currentPage.value = 1
  fetchList()
}

function bindCardEl(id: number, el: Element | ComponentPublicInstance | null) {
  if (el instanceof HTMLElement) cardEls.set(id, el)
  else cardEls.delete(id)
}

function markUserScrolled() {
  userScrolled = true
}

function onUserKey(e: KeyboardEvent) {
  if (['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', 'Space', 'Home', 'End'].includes(e.code)) {
    userScrolled = true
  }
}

function getDockBottom(): number {
  const dock = dockRef.value
  if (dock) {
    return dock.getBoundingClientRect().bottom
  }
  return showTimeline.value ? 212 : 140
}

function getAnchorEl(id: number): HTMLElement | null {
  const cardEl = cardEls.get(id)
  if (!cardEl) return null
  const groupSection = cardEl.closest('section.day') as HTMLElement | null
  // 若是该日期分组的第一项，滚到分组标题顶部；否则滚到卡片本身
  if (groupSection) {
    const firstLi = groupSection.querySelector('li[data-activity-id]')
    if (firstLi === cardEl) return groupSection
  }
  return cardEl
}

function syncFocusedByScroll() {
  if (props.minimal || orderBy.value !== 'time') return
  if (Date.now() < programmaticLockUntil) return
  if (!sortedItems.value.length) return

  const dockBottom = getDockBottom()
  const anchorY = dockBottom + 88

  // 已滑到底部时直接对齐到最后一项
  const atBottom =
    window.scrollY > 10 &&
    window.scrollY + window.innerHeight >= document.documentElement.scrollHeight - 4
  if (atBottom) {
    focusedActivityId.value = sortedItems.value[sortedItems.value.length - 1].id
    return
  }

  let matchedId: number | null = null
  for (const item of sortedItems.value) {
    const cardEl = cardEls.get(item.id)
    const anchorEl = getAnchorEl(item.id)
    if (!cardEl || !anchorEl) continue
    const cardRect = cardEl.getBoundingClientRect()
    // 已经完全滚出时间轴上方的活动不参与选中
    if (cardRect.bottom <= dockBottom + 24) continue
    const anchorTop = anchorEl.getBoundingClientRect().top
    if (anchorTop <= anchorY) {
      matchedId = item.id
    } else {
      if (matchedId == null) {
        matchedId = item.id
      }
      break
    }
  }

  if (matchedId != null) {
    focusedActivityId.value = matchedId
  }
}

function onWindowScroll() {
  if (scrollRaf) return
  scrollRaf = window.requestAnimationFrame(() => {
    scrollRaf = 0
    syncFocusedByScroll()
  })
}

function cancelScrollAnim() {
  if (animScrollRaf) {
    window.cancelAnimationFrame(animScrollRaf)
    animScrollRaf = 0
  }
}

function animateScrollTo(targetTop: number, duration = 340) {
  cancelScrollAnim()
  const maxScroll = Math.max(0, document.documentElement.scrollHeight - window.innerHeight)
  const dest = Math.min(maxScroll, Math.max(0, targetTop))
  const startY = window.scrollY
  const delta = dest - startY
  if (Math.abs(delta) < 1 || duration <= 0) {
    window.scrollTo(0, dest)
    return
  }
  const startTime = performance.now()
  const easeOutCubic = (t: number) => 1 - Math.pow(1 - t, 3)

  const step = (now: number) => {
    const progress = Math.min(1, (now - startTime) / duration)
    window.scrollTo(0, startY + delta * easeOutCubic(progress))
    if (progress < 1) {
      animScrollRaf = window.requestAnimationFrame(step)
    } else {
      animScrollRaf = 0
    }
  }
  animScrollRaf = window.requestAnimationFrame(step)
}

function getAlignedTopForActivity(id: number): number | null {
  const anchor = getAnchorEl(id)
  if (!anchor) return null
  const dockBottom = getDockBottom()
  return Math.max(0, window.scrollY + anchor.getBoundingClientRect().top - dockBottom - 16)
}

function scrollElementBelowDock(id: number, smooth: boolean) {
  const targetTop = getAlignedTopForActivity(id)
  if (targetTop == null) return
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (smooth && !reduced) {
    programmaticLockUntil = Date.now() + 420
    animateScrollTo(targetTop, 340)
  } else {
    cancelScrollAnim()
    programmaticLockUntil = Date.now() + 120
    window.scrollTo(0, targetTop)
  }
}

function isInsideInteractiveOverlay(target: EventTarget | null): boolean {
  if (!(target instanceof Element)) return false
  return Boolean(
    target.closest('.ai-panel, .ai-assistant, .el-dialog, .el-popper, .nav-panel-shell, .nav-drawer, .nav-account-pop')
  )
}

/** 滚轮按格步进：在「按时间」模式下，滚轮滚一次平滑切换刚好一个活动 */
function onWheelStep(e: WheelEvent) {
  userScrolled = true
  if (!showTimeline.value || sortedItems.value.length < 2) return
  if (e.ctrlKey || e.metaKey || e.altKey) return
  if (isInsideInteractiveOverlay(e.target)) return
  if (Math.abs(e.deltaX) > Math.abs(e.deltaY)) return
  if (e.deltaY === 0) return

  const dir: 1 | -1 = e.deltaY > 0 ? 1 : -1
  const list = sortedItems.value
  let currentIdx = list.findIndex((item) => item.id === focusedActivityId.value)
  if (currentIdx < 0) currentIdx = 0

  // 边界放行：已在最末一项继续下滑可自然看到页脚；已在首项且到顶继续上滑不拦截
  if (dir === 1 && currentIdx === list.length - 1) {
    const lastTop = getAlignedTopForActivity(list[currentIdx].id)
    if (lastTop == null || window.scrollY >= lastTop - 12) {
      return
    }
  }
  if (dir === -1 && currentIdx === 0 && window.scrollY <= 4) {
    return
  }

  e.preventDefault()

  const now = Date.now()
  if (now < wheelStepLockUntil) {
    wheelAccum = 0
    return
  }

  // 兼容离散鼠标滚轮（单格 ~100px 立即触发）与触控板轻滑累积
  window.clearTimeout(wheelResetTimer)
  wheelAccum += e.deltaY
  wheelResetTimer = window.setTimeout(() => {
    wheelAccum = 0
  }, 140)

  if (Math.abs(wheelAccum) < 24) return
  wheelAccum = 0

  // 若用户已滚入页脚，向上滚第一格先回到最后一个活动
  let targetIdx = currentIdx + dir
  if (dir === -1 && currentIdx === list.length - 1) {
    const lastTop = getAlignedTopForActivity(list[currentIdx].id)
    if (lastTop != null && window.scrollY > lastTop + 40) {
      targetIdx = currentIdx
    }
  }
  targetIdx = Math.max(0, Math.min(list.length - 1, targetIdx))

  const targetItem = list[targetIdx]
  if (!targetItem) return

  wheelStepLockUntil = now + 380
  focusedActivityId.value = targetItem.id
  scrollElementBelowDock(targetItem.id, true)
}

function scrollToActivity(id: number) {
  userScrolled = true
  focusedActivityId.value = id
  scrollElementBelowDock(id, true)
}

function pickNearestInitialActivity() {
  if (!sortedItems.value.length) return null
  const now = Date.now()
  // 1. 优先聚焦正在进行的活动
  const live = sortedItems.value.find((item) => statusOf(item) === 'live')
  if (live) return live
  // 2. 其次聚焦最近一场即将开始的活动
  const soon = sortedItems.value.find((item) => statusOf(item) === 'soon')
  if (soon) return soon
  // 3. 若全部已结束，聚焦离当前时间最近的最后一场
  let nearest = sortedItems.value[sortedItems.value.length - 1]
  let bestDiff = Infinity
  for (const item of sortedItems.value) {
    const t = item.startTime ? Date.parse(item.startTime) : NaN
    if (!Number.isFinite(t)) continue
    const diff = Math.abs(t - now)
    if (diff < bestDiff) {
      bestDiff = diff
      nearest = item
    }
  }
  return nearest
}

async function autoLocateInitial() {
  if (props.minimal || orderBy.value !== 'time' || userScrolled || hasAutoScrolled) return
  if (!sortedItems.value.length) return

  const target = pickNearestInitialActivity()
  if (!target) return

  hasAutoScrolled = true
  focusedActivityId.value = target.id
  await nextTick()
  window.requestAnimationFrame(() => {
    if (!userScrolled) {
      scrollElementBelowDock(target.id, false)
      focusedActivityId.value = target.id
    }
  })
}

// 默认活动封面图片库 - 会议/科技/峰会主题
const defaultCoverImages = [
  'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1531482615713-2afd69097998?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1573164713988-8665fc963095?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1504384764586-bb4cdc1707b0?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1563986768609-322da13575f3?w=640&h=360&fit=crop'
]

function getCoverImage(item: any, index: number) {
  return item.coverUrl || defaultCoverImages[index % defaultCoverImages.length]
}

function formatDate(dateStr: string) {
  if (!dateStr) return '时间待定'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
}

/** 活动状态：live 进行中 / soon 未开始 / ended 已结束 */
function statusOf(item: any): 'live' | 'soon' | 'ended' | '' {
  const now = Date.now()
  const start = item.startTime ? new Date(item.startTime).getTime() : NaN
  const end = item.endTime ? new Date(item.endTime).getTime() : NaN
  if (!Number.isNaN(end) && now > end) return 'ended'
  if (!Number.isNaN(start) && now >= start) return 'live'
  if (!Number.isNaN(start)) return 'soon'
  return ''
}

function dotClass(item: any) {
  const s = statusOf(item)
  return s === 'live' ? 'ss-dot--live' : s === 'soon' ? 'ss-dot--soon' : ''
}

/** 按时间排序时按日期分组（日期标题吸顶）；按热度排序时不分组 */
const groups = computed(() => {
  const entries = sortedItems.value.map((item, index) => ({ item, index }))
  if (orderBy.value === 'heat') return [{ key: 'all', label: '', week: '', items: entries }]
  const map = new Map<string, { key: string; label: string; week: string; items: typeof entries }>()
  const thisYear = new Date().getFullYear()
  for (const entry of entries) {
    const d = entry.item.startTime ? new Date(entry.item.startTime) : null
    const key = d ? `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}` : 'tbd'
    if (!map.has(key)) {
      map.set(key, {
        key,
        label: d
          ? d.toLocaleDateString('zh-CN', d.getFullYear() === thisYear ? { month: 'long', day: 'numeric' } : { year: 'numeric', month: 'long', day: 'numeric' })
          : '时间待定',
        week: d ? d.toLocaleDateString('zh-CN', { weekday: 'long' }) : '',
        items: []
      })
    }
    map.get(key)!.items.push(entry)
  }
  return [...map.values()]
})

async function fetchList() {
  loading.value = true
  try {
    const { data } = await api.get('/activity/list', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        orderBy: orderBy.value
      }
    })
    if (data.code === 0) {
      items.value = data.data.items
      total.value = data.data.total
      await nextTick()
      if (orderBy.value === 'time') {
        if (!hasAutoScrolled) {
          await autoLocateInitial()
        } else {
          syncFocusedByScroll()
        }
      }
    }
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  userScrolled = true
  currentPage.value = page
  fetchList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function syncScrollbarWidth() {
  if (typeof window === 'undefined') return
  sbw.value = Math.max(0, window.innerWidth - document.documentElement.clientWidth)
}

function setupStickyObserver() {
  stickyObserver?.disconnect()
  if (props.minimal || !stickySentinel.value || !('IntersectionObserver' in window)) return
  stickyObserver = new IntersectionObserver(
    ([entry]) => {
      if (!entry) return
      isStuck.value = !entry.isIntersecting && entry.boundingClientRect.top <= 0
    },
    { threshold: 0 }
  )
  stickyObserver.observe(stickySentinel.value)
}

function onRouteEntered() {
  syncScrollbarWidth()
  if (!props.minimal && orderBy.value === 'time' && sortedItems.value.length) {
    if (!userScrolled) {
      const target = pickNearestInitialActivity()
      if (target) {
        focusedActivityId.value = target.id
        scrollElementBelowDock(target.id, false)
        return
      }
    }
    syncFocusedByScroll()
  }
}

watch(orderBy, async () => {
  await nextTick()
  syncFocusedByScroll()
})

onMounted(() => {
  syncScrollbarWidth()
  setupStickyObserver()
  window.addEventListener('resize', syncScrollbarWidth, { passive: true })
  window.addEventListener('scroll', onWindowScroll, { passive: true })
  window.addEventListener('ss-route-entered', onRouteEntered)
  if ('ResizeObserver' in window) {
    sbwObserver = new ResizeObserver(syncScrollbarWidth)
    sbwObserver.observe(document.documentElement)
  }
  window.addEventListener('wheel', onWheelStep, { passive: false })
  window.addEventListener('touchmove', markUserScrolled, { passive: true })
  window.addEventListener('keydown', onUserKey)
  fetchList()
})

onBeforeUnmount(() => {
  if (scrollRaf) window.cancelAnimationFrame(scrollRaf)
  cancelScrollAnim()
  window.clearTimeout(wheelResetTimer)
  stickyObserver?.disconnect()
  sbwObserver?.disconnect()
  window.removeEventListener('resize', syncScrollbarWidth)
  window.removeEventListener('scroll', onWindowScroll)
  window.removeEventListener('ss-route-entered', onRouteEntered)
  window.removeEventListener('wheel', onWheelStep)
  window.removeEventListener('touchmove', markUserScrolled)
  window.removeEventListener('keydown', onUserKey)
})
</script>

<style scoped>
.activities-container {
  --nav-mask-h: 84px;
  --head-h: 56px;
  --timeline-h: 72px;
  width: min(100%, var(--ss-w-narrow));
  margin: 0 auto;
}
.activities-container.is-minimal {
  width: auto;
}

/* 哨兵：放在吸顶白底块上方，滚出视口即视为进入吸顶态 */
.sticky-sentinel {
  height: 1px;
  margin-top: -64px;
  pointer-events: none;
}

/* 吸顶白底块：导航占位（84px）→ 标题行 → 时间轴（按时间时显示） */
.sticky-dock {
  position: sticky;
  top: 0;
  z-index: 20;
  width: calc(100vw - var(--sbw, 0px));
  margin-left: calc(50% - (100vw - var(--sbw, 0px)) / 2);
  padding-top: var(--nav-mask-h);
  background: #ffffff;
  border-bottom: 1px solid rgba(21, 21, 21, 0.08);
  margin-bottom: 16px;
}

.dock-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: min(100% - 32px, var(--ss-w-narrow));
  height: var(--head-h);
  margin: 0 auto;
}

.dock-title {
  margin: 0;
  font-size: var(--ss-fs-h2);
  font-weight: 600;
  line-height: 1.25;
  color: var(--ss-ink);
  transition: font-size 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}

.sticky-dock.is-stuck .dock-title {
  font-size: 22px;
}

/* 分段控件：外框 10px，选中块 8px */
.sort-chips {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px;
  border-radius: 10px;
  background: var(--ss-fill);
}
.sort-btn {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--ss-muted);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color var(--ss-dur) var(--ss-ease), color var(--ss-dur) var(--ss-ease);
}
.sort-btn:hover {
  color: #151515;
}
.sort-btn.is-active {
  background: #151515;
  color: #fff;
}

/* ---- 日程：按日期分组（已有吸顶时间轴，日期标题随列表自然滚动，不再吸顶） ---- */
.schedule {
  display: flex;
  flex-direction: column;
  gap: 28px;
}
/* 留出底部余量，使最后几场活动也能平滑滚到时间轴正下方对齐 */
.has-timeline .schedule {
  padding-bottom: max(220px, calc(100vh - 460px));
}
.day-list > li.is-focused .event-row {
  background: rgba(19, 26, 36, 0.028);
}
.day-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 10px 0 8px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--ss-line);
  font-size: 17px;
}
.day-date {
  font-weight: 600;
  color: var(--ss-ink);
}
.day-week {
  font-size: 15px;
  font-weight: 500;
  color: var(--ss-muted);
}
.day-list {
  list-style: none;
}

.today-tail {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  border-radius: 12px;
  border: 1px dashed rgba(21, 21, 21, 0.14);
  background: #fafafa;
  color: #737373;
  font-size: 14px;
  font-weight: 500;
}
.today-tail-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #151515;
  flex-shrink: 0;
}

.event-row {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
  padding: 16px 12px;
  margin: 0 -12px;
  color: inherit;
}
.event-cover {
  width: 112px;
  height: 112px;
  border-radius: var(--ss-r-md);
  object-fit: cover;
  background: var(--ss-bg-soft);
}
.event-text {
  min-width: 0;
  padding-top: 2px;
}
.event-time {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--ss-muted);
}
.event-title {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--ss-ink);
  transition: color var(--ss-dur) var(--ss-ease);
}
.event-row:hover .event-title {
  color: var(--ss-navy);
}
.event-summary {
  margin-top: 4px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--ss-muted);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.event-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 16px;
  margin-top: 10px;
  font-size: 13px;
  color: var(--ss-muted);
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.meta-icon {
  color: currentColor;
}
.heat-icon {
  color: var(--ss-warning);
}
.meta-more {
  margin-left: auto;
  font-weight: 500;
  color: var(--ss-navy);
  opacity: 0;
  transition: opacity var(--ss-dur) var(--ss-ease);
}
.event-row:hover .meta-more,
.event-row:focus-visible .meta-more {
  opacity: 1;
}
.skeleton-row {
  pointer-events: none;
}

/* ---- 精简模式：缩略图 + 时间 + 标题 + 地点 ---- */
.minimal-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.minimal-link {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 10px;
  margin: 0 -10px;
  border-radius: var(--ss-r-md);
  color: var(--ss-ink);
  cursor: pointer;
  transition: background-color var(--ss-dur) var(--ss-ease);
}
.minimal-link:hover {
  background: var(--ss-row-hover);
}
.minimal-thumb {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  object-fit: cover;
  background: var(--ss-bg-soft);
}
.minimal-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.minimal-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ss-muted);
}
.minimal-link .title {
  margin-top: 2px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--ss-dur) var(--ss-ease);
}
.minimal-link:hover .title {
  color: var(--ss-navy);
}
.minimal-loc {
  font-size: 13px;
  color: var(--ss-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.minimal-heat {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--ss-fill);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 8px 0 20px;
}

@media (max-width: 768px) {
  .dock-head {
    width: min(100% - 24px, var(--ss-w-narrow));
  }
}

@media (max-width: 640px) {
  .activities-container {
    --head-h: 48px;
    --timeline-h: 56px;
  }
  .dock-title {
    font-size: 24px;
  }
  .sticky-dock.is-stuck .dock-title {
    font-size: 22px;
  }
  .event-row {
    grid-template-columns: 76px minmax(0, 1fr);
    gap: 14px;
    padding: 14px 8px;
    margin: 0 -8px;
  }
  .event-cover {
    width: 76px;
    height: 76px;
  }
  .event-title {
    font-size: 16px;
  }
  .event-summary {
    display: none;
  }
  .meta-more {
    display: none;
  }
}
</style>
