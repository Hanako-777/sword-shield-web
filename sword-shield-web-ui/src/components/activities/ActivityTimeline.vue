<template>
  <div
    v-if="datedEvents.length"
    class="tl-bar"
    role="navigation"
    aria-label="活动时间轴"
  >
    <div class="tl-inner">
      <div class="tl-track" :class="{ 'has-today-tag': todayOutside }">
        <!-- 基线 -->
        <div class="tl-rail" aria-hidden="true" />

        <!-- 每周浅色刻度 -->
        <span
          v-for="tick in weekTicks"
          :key="tick.key"
          class="tl-week-tick"
          :style="{ left: tick.pct + '%' }"
          aria-hidden="true"
        />

        <!-- 月份起点与年份变化标注 -->
        <div
          v-for="m in monthMarks"
          :key="m.key"
          class="tl-month"
          :style="{ left: m.pct + '%' }"
          aria-hidden="true"
        >
          <span v-if="m.yearLabel" class="tl-year">{{ m.yearLabel }}</span>
          <span class="tl-month-label">{{ m.label }}</span>
        </div>

        <!-- 活动点按钮 -->
        <button
          v-for="ev in datedEvents"
          :key="ev.id"
          type="button"
          class="tl-dot-btn"
          :class="{ 'is-active': ev.id === activeId }"
          :style="{ left: ev.pct + '%' }"
          :aria-label="ev.ariaLabel"
          :title="ev.title"
          @click="$emit('select', ev.id)"
        >
          <span class="tl-dot" aria-hidden="true" />
        </button>

        <!-- 游标 + 里程表日期 -->
        <div
          v-if="activeEvent"
          class="tl-cursor"
          :class="{ 'is-left-label': cursorPct > 72 }"
          :style="{ left: cursorPct + '%' }"
        >
          <span class="tl-cursor-line" aria-hidden="true" />
          <span class="tl-cursor-dot" aria-hidden="true" />
          <div class="tl-cursor-date" :aria-label="`${displayMonth}月${displayDay}日`">
            <OdometerDigit
              v-if="displayMonth >= 10"
              :value="Math.floor(displayMonth / 10)"
              :direction="direction"
            />
            <OdometerDigit :value="displayMonth % 10" :direction="direction" />
            <span class="tl-unit">月</span>
            <OdometerDigit
              :value="Math.floor(displayDay / 10)"
              :direction="direction"
              :faded="displayDay < 10"
            />
            <OdometerDigit :value="displayDay % 10" :direction="direction" />
            <span class="tl-unit">日</span>
          </div>
        </div>
      </div>

      <!-- 今天若在时间轴范围外，在最右端显示 -->
      <div v-if="todayOutside" class="tl-today-tag" aria-label="今天日期">
        今天 {{ todayLabel }} →
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import OdometerDigit from './OdometerDigit.vue'

export interface TimelineActivity {
  id: number
  title: string
  startTime?: string
}

const props = defineProps<{
  items: TimelineActivity[]
  activeId: number | null
}>()

defineEmits<{ (e: 'select', id: number): void }>()

const DAY_MS = 86_400_000

function startOfDay(ms: number) {
  const d = new Date(ms)
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
}

const parsed = computed(() =>
  props.items
    .map((item) => {
      const ms = item.startTime ? Date.parse(item.startTime) : NaN
      return { item, ms }
    })
    .filter((entry) => Number.isFinite(entry.ms))
    .sort((a, b) => a.ms - b.ms)
)

const range = computed(() => {
  if (!parsed.value.length) return { minMs: 0, maxMs: DAY_MS, spanMs: DAY_MS }
  const firstDay = startOfDay(parsed.value[0].ms)
  const lastDay = startOfDay(parsed.value[parsed.value.length - 1].ms)
  const minMs = firstDay - 3 * DAY_MS
  const maxMs = lastDay + 3 * DAY_MS
  return { minMs, maxMs, spanMs: Math.max(DAY_MS, maxMs - minMs) }
})

function pctOf(ms: number) {
  const { minMs, spanMs } = range.value
  return Math.min(100, Math.max(0, ((ms - minMs) / spanMs) * 100))
}

const datedEvents = computed(() =>
  parsed.value.map(({ item, ms }) => {
    const d = new Date(ms)
    const dateText = `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
    return {
      id: item.id,
      title: item.title,
      ms,
      month: d.getMonth() + 1,
      day: d.getDate(),
      pct: pctOf(ms),
      ariaLabel: `${dateText} ${item.title}`
    }
  })
)

const weekTicks = computed(() => {
  if (!parsed.value.length) return []
  const { minMs, maxMs } = range.value
  const ticks: { key: number; pct: number }[] = []
  // 从 minMs 后的第一个周一开始每周画一条刻度
  const start = new Date(minMs)
  const offsetToMon = (8 - start.getDay()) % 7
  let cur = minMs + offsetToMon * DAY_MS
  // 若时间跨度极长（> 2 年），步进仍按周但在超过 120 条时按 2 周采样避免过密
  const totalWeeks = (maxMs - minMs) / (7 * DAY_MS)
  const stepDays = totalWeeks > 120 ? 14 : 7
  while (cur <= maxMs) {
    ticks.push({ key: cur, pct: pctOf(cur) })
    cur += stepDays * DAY_MS
  }
  return ticks
})

const monthMarks = computed(() => {
  if (!parsed.value.length) return []
  const { minMs, maxMs } = range.value
  const marks: { key: string; pct: number; label: string; yearLabel: string }[] = []
  const startD = new Date(minMs)
  let year = startD.getFullYear()
  let month = startD.getMonth()
  let prevYear: number | null = null

  while (true) {
    const mStart = new Date(year, month, 1).getTime()
    if (mStart > maxMs) break
    if (mStart >= minMs) {
      const yearChanged = prevYear !== null && year !== prevYear
      marks.push({
        key: `${year}-${month + 1}`,
        pct: pctOf(mStart),
        label: `${month + 1}月`,
        yearLabel: yearChanged ? String(year) : ''
      })
      prevYear = year
    } else {
      prevYear = year
    }
    month++
    if (month > 11) {
      month = 0
      year++
    }
  }

  // 如果范围内没有跨过月初（例如所有活动都在同一月内），在左侧补一个当前月标注
  if (!marks.length) {
    const d = new Date(parsed.value[0].ms)
    marks.push({
      key: `${d.getFullYear()}-${d.getMonth() + 1}`,
      pct: 2,
      label: `${d.getMonth() + 1}月`,
      yearLabel: ''
    })
  }
  return marks
})

const todayDate = new Date()
const todayMs = todayDate.getTime()
const todayLabel = `${todayDate.getMonth() + 1}月${todayDate.getDate()}日`
const todayOutside = computed(() => {
  if (!parsed.value.length) return false
  return todayMs < range.value.minMs || todayMs > range.value.maxMs
})

const activeEvent = computed(() => {
  if (!datedEvents.value.length) return null
  return datedEvents.value.find((e) => e.id === props.activeId) || datedEvents.value[0]
})

const cursorPct = computed(() => activeEvent.value?.pct ?? 0)
const displayMonth = computed(() => activeEvent.value?.month ?? 1)
const displayDay = computed(() => activeEvent.value?.day ?? 1)

const direction = ref<1 | -1>(1)
watch(
  () => activeEvent.value?.ms,
  (nextMs, prevMs) => {
    if (nextMs == null || prevMs == null || nextMs === prevMs) return
    direction.value = nextMs > prevMs ? 1 : -1
  }
)
</script>

<style scoped>
.tl-bar {
  width: 100%;
  background: #ffffff;
}

.tl-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  height: 72px;
  padding: 0 28px;
  border-top: 1px solid rgba(21, 21, 21, 0.06);
}

.tl-track {
  position: relative;
  flex: 1;
  min-width: 0;
  height: 100%;
}

.tl-rail {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 1px;
  background: rgba(21, 21, 21, 0.12);
}

.tl-week-tick {
  position: absolute;
  top: calc(50% - 5px);
  width: 1px;
  height: 10px;
  background: rgba(21, 21, 21, 0.1);
  pointer-events: none;
}

.tl-month {
  position: absolute;
  bottom: 7px;
  transform: translateX(-50%);
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  white-space: nowrap;
  pointer-events: none;
}

.tl-year {
  font-family: 'Inter Variable', 'Inter', sans-serif;
  font-size: 10px;
  font-weight: 600;
  color: #a3a3a3;
  font-variant-numeric: tabular-nums;
}

.tl-month-label {
  font-size: 11px;
  font-weight: 500;
  color: #737373;
}

.tl-dot-btn {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: 0;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  z-index: 2;
}

.tl-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #737373;
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.tl-dot-btn:hover .tl-dot,
.tl-dot-btn.is-active .tl-dot {
  background: #151515;
  transform: scale(1.35);
}

.tl-cursor {
  position: absolute;
  top: 0;
  bottom: 0;
  z-index: 3;
  pointer-events: none;
  transition: left 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.tl-cursor-line {
  position: absolute;
  left: -1px;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: #151515;
  border-radius: 1px;
}

.tl-cursor-dot {
  position: absolute;
  left: -5px;
  top: calc(50% - 5px);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #151515;
}

.tl-cursor-date {
  position: absolute;
  top: 8px;
  left: 10px;
  display: inline-flex;
  align-items: baseline;
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.94);
  font-family: 'Inter Variable', 'Inter', var(--ss-font);
  font-size: 20px;
  font-weight: 600;
  line-height: 1;
  font-variant-numeric: tabular-nums;
  color: #151515;
  white-space: nowrap;
}

.tl-cursor.is-left-label .tl-cursor-date {
  left: auto;
  right: 10px;
}

.tl-unit {
  font-size: 13px;
  font-weight: 500;
  color: #737373;
  margin-inline: 2px 4px;
}

.tl-unit:last-child {
  margin-right: 0;
}

.tl-today-tag {
  flex-shrink: 0;
  padding: 5px 10px;
  border-radius: 6px;
  background: #f4f4f5;
  color: #525252;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .tl-inner {
    height: 56px;
    padding: 0 14px;
    gap: 10px;
  }

  .tl-cursor-date {
    top: 4px;
    font-size: 16px;
  }

  .tl-unit {
    font-size: 11px;
  }

  .tl-today-tag {
    font-size: 11px;
    padding: 4px 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .tl-cursor {
    transition: none;
  }
}
</style>
