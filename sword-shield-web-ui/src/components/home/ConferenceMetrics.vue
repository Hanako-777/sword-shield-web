<template>
  <section ref="bar" class="metrics-section" aria-labelledby="overview-title">
    <HomeSectionHead title="大会概览" title-id="overview-title" link-text="浏览数据看板" to="/statistics" />

    <div class="metrics-card">
      <dl class="metrics-grid">
        <div v-for="(metric, i) in metrics" :key="metric.label" class="metric-item">
          <dd class="metric-value">
            <span class="ss-sr-only">{{ metric.unavailable ? '暂无数据' : `${metric.value}${metric.unit}` }}</span>
            <span aria-hidden="true">
              <template v-if="metric.unavailable">—</template>
              <template v-else>{{ display(i) }}<small v-if="metric.unit">{{ metric.unit }}</small></template>
            </span>
          </dd>
          <dt class="metric-label">{{ metric.label }}</dt>
        </div>
      </dl>

      <div class="metrics-foot">
        <div class="conference-facts">
          <span><Icon name="map-pin" :size="15" />{{ location }}</span>
          <span><Icon name="calendar" :size="15" />{{ dateRange }}</span>
        </div>

        <router-link v-if="next" class="countdown" :to="'/activity/' + next.id">
          <i class="live-dot" aria-hidden="true" />
          <b class="countdown-label">{{ next.start > now ? '下一场活动' : '进行中' }}</b>
          <span class="next-name">{{ next.title }}</span>
          <span v-if="next.start > now" class="digits">{{ remaining }}</span>
          <span class="countdown-arrow" aria-hidden="true">↗</span>
        </router-link>
        <p v-else-if="activityError" class="foot-note">
          活动总数与倒计时加载失败，请稍后重试。
          <button type="button" class="retry-inline" @click="$emit('retry')">重新加载</button>
        </p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import Icon from '../ui/Icon.vue'
import HomeSectionHead from './HomeSectionHead.vue'
import type { ConferenceEvent } from './programme'

const props = defineProps<{
  next?: ConferenceEvent
  now: number
  dateRange: string
  location: string
  activityTotal?: number
  activityLoading?: boolean
  activityError?: string
}>()

defineEmits<{ (e: 'retry'): void }>()

const remaining = computed(() => {
  const seconds = Math.max(0, Math.ceil(((props.next?.start ?? props.now) - props.now) / 1000))
  const pad = (n: number) => String(n).padStart(2, '0')
  return Math.floor(seconds / 86400) + '天 ' + pad(Math.floor(seconds / 3600) % 24) + '时 ' +
    pad(Math.floor(seconds / 60) % 60) + '分 ' + pad(seconds % 60) + '秒'
})

const bar = ref<HTMLElement>()
const progress = ref(1)

const metrics = computed(() => [
  { value: 8420, unit: '', label: '近 30 天参会人次', precision: 0, unavailable: false },
  { value: 5, unit: '场', label: '筹备中大会', precision: 0, unavailable: false },
  { value: 96.3, unit: '%', label: '平均签到率', precision: 1, unavailable: false },
  {
    value: props.activityTotal ?? 0,
    unit: '场',
    label: '大会活动总数',
    precision: 0,
    unavailable: !!props.activityError && !props.activityTotal
  }
])

function display(i: number) {
  const m = metrics.value[i]
  return (m.value * progress.value).toLocaleString('zh-CN', {
    minimumFractionDigits: m.precision,
    maximumFractionDigits: m.precision
  })
}

const motion = matchMedia('(prefers-reduced-motion: reduce)')
let observer: IntersectionObserver
let frame = 0
let inView = false
let elapsed = 0
let previous = 0
let finished = false

function tick(time: number) {
  if (previous) elapsed += time - previous
  previous = time
  progress.value = 1 - (1 - Math.min(1, elapsed / 1150)) ** 3
  if (elapsed < 1150) frame = requestAnimationFrame(tick)
  else { finished = true; observer.disconnect() }
}

function sync() {
  cancelAnimationFrame(frame)
  previous = 0
  if (motion.matches) {
    finished = true
    progress.value = 1
    observer.disconnect()
  } else if (!finished && inView && !document.hidden) frame = requestAnimationFrame(tick)
}

onMounted(() => {
  progress.value = 0
  observer = new IntersectionObserver(([entry]) => { inView = entry.isIntersecting; sync() }, { threshold: 0.15 })
  observer.observe(bar.value!)
  motion.addEventListener('change', sync)
  document.addEventListener('visibilitychange', sync)
  sync()
})

onBeforeUnmount(() => {
  observer.disconnect()
  cancelAnimationFrame(frame)
  motion.removeEventListener('change', sync)
  document.removeEventListener('visibilitychange', sync)
})
</script>

<style scoped>
.metrics-card {
  padding: 28px;
  border-radius: 16px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.metrics-card:hover {
  border-color: rgba(21, 21, 21, 0.16);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-inline: 24px;
  border-left: 1px solid rgba(21, 21, 21, 0.08);
  min-width: 0;
}

.metric-item:first-child {
  padding-left: 0;
  border-left: 0;
}

.metric-item:last-child {
  padding-right: 0;
}

.metric-value {
  margin: 0;
  font-family: 'Inter Variable', 'Inter', var(--ss-font);
  font-size: 40px;
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: -0.025em;
  font-variant-numeric: tabular-nums;
  color: #151515;
}

.metric-value small {
  margin-left: 3px;
  font-size: 0.45em;
  font-weight: 500;
  color: #737373;
}

.metric-label {
  font-size: 13px;
  line-height: 1.4;
  color: #737373;
}

.metrics-foot {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid rgba(21, 21, 21, 0.08);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.conference-facts {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 20px;
}

.conference-facts span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #737373;
}

.countdown {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 10px;
  width: fit-content;
  max-width: 100%;
  min-height: 40px;
  padding: 8px 14px;
  border-radius: 12px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #fff;
  color: #151515;
  font-size: 13px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.countdown:hover {
  border-color: rgba(21, 21, 21, 0.16);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #14b8a6;
  flex-shrink: 0;
}

.countdown-label {
  font-weight: 600;
  color: #151515;
}

.next-name {
  min-width: 0;
  color: #151515;
  overflow-wrap: anywhere;
}

.digits {
  font-family: 'Inter Variable', 'Inter', var(--ss-font);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  color: #737373;
}

.countdown-arrow {
  color: #737373;
}

.foot-note {
  font-size: 13px;
  color: #737373;
}

.retry-inline {
  margin-left: 6px;
  border: 0;
  background: transparent;
  color: #151515;
  font-weight: 500;
  cursor: pointer;
}

.retry-inline:hover {
  text-decoration: underline;
}

@media (max-width: 960px) {
  .metric-item {
    padding-inline: 16px;
  }

  .metric-value {
    font-size: 34px;
  }
}

@media (max-width: 640px) {
  .metrics-card {
    padding: 20px;
  }

  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-item {
    padding: 14px 12px;
    border-left: 1px solid rgba(21, 21, 21, 0.08);
  }

  .metric-item:nth-child(odd) {
    padding-left: 0;
    border-left: 0;
  }

  .metric-item:nth-child(even) {
    padding-right: 0;
  }

  .metric-item:nth-child(-n + 2) {
    padding-top: 0;
  }

  .metric-item:nth-child(n + 3) {
    padding-bottom: 0;
    border-top: 1px solid rgba(21, 21, 21, 0.08);
  }

  .metric-value {
    font-size: 30px;
  }
}
</style>
