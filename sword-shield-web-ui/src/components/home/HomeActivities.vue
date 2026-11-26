<template>
  <section class="home-activities" aria-labelledby="home-activities-title">
    <HomeSectionHead title="活动回顾" title-id="home-activities-title" link-text="全部活动" to="/activities" />

    <div v-if="loading && !displayed.length" class="popular-grid" aria-busy="true">
      <div v-for="n in 4" :key="n" class="popular-row skeleton-row">
        <div class="cover-wrap ss-skeleton" />
        <div class="row-body">
          <div class="ss-skeleton sk-time" />
          <div class="ss-skeleton sk-title" />
          <div class="ss-skeleton sk-loc" />
        </div>
      </div>
    </div>

    <div v-else-if="error && !displayed.length" class="section-state">
      <p>{{ error }}</p>
      <button type="button" class="state-btn" @click="$emit('retry')">重新加载</button>
    </div>

    <div v-else-if="!displayed.length" class="section-state">
      <p>暂无已结束的活动回顾</p>
      <router-link to="/activities" class="state-btn">前往活动日程</router-link>
    </div>

    <!-- 仅展示已结束的 4 场活动：Luma 热门活动无描边双列图文横排 -->
    <div v-else class="popular-grid">
      <router-link
        v-for="(item, i) in displayed"
        :key="item.id"
        :to="`/activity/${item.id}`"
        class="popular-row"
      >
        <div class="cover-wrap">
          <EventCover :src="item.image" :index="i" loading="lazy" />
        </div>
        <div class="row-body">
          <p class="row-time">{{ formatDateTime(item.startTime) }}</p>
          <h3 class="row-title">{{ item.title }}</h3>
          <p v-if="item.location" class="row-meta">
            <Icon name="map-pin" :size="13" class="meta-icon" />
            <span>{{ formatVenue(item.location) }}</span>
          </p>
        </div>
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import EventCover from './EventCover.vue'
import HomeSectionHead from './HomeSectionHead.vue'
import Icon from '../ui/Icon.vue'
import type { ConferenceEvent } from './programme'

const props = defineProps<{
  events: ConferenceEvent[]
  now: number
  loading?: boolean
  error?: string
}>()

defineEmits<{ (e: 'retry'): void }>()

/** 仅筛选已结束的活动（按结束时间倒序，最多展示 4 场） */
const displayed = computed(() =>
  props.events
    .filter((item) => Number.isFinite(item.end) && item.end < props.now)
    .sort((a, b) => b.end - a.end)
    .slice(0, 4)
)

function formatDateTime(dateStr?: string) {
  if (!dateStr) return '往期活动'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return '往期活动'
  const thisYear = new Date().getFullYear()
  const sameYear = date.getFullYear() === thisYear
  const datePart = date.toLocaleDateString('zh-CN', sameYear
    ? { month: 'short', day: 'numeric', weekday: 'short' }
    : { year: 'numeric', month: 'short', day: 'numeric' })
  const timePart = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
  return `${datePart} · ${timePart}`
}

function formatVenue(location: string) {
  return location.replace(/^浙江省杭州市/, '').trim() || location
}
</script>

<style scoped>
.popular-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 28px;
  margin-inline: -10px;
}

.popular-row {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  padding: 10px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: #131a24;
  transition: background-color 0.2s ease;
}

.popular-row:hover {
  background: rgba(19, 26, 36, 0.035);
}

.cover-wrap {
  position: relative;
  width: 96px;
  height: 96px;
  flex-shrink: 0;
  border-radius: 14px;
  overflow: hidden;
  background: #f4f4f5;
}

.cover-wrap::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  box-shadow: inset 0 0 0 1px rgba(19, 26, 36, 0.06);
  pointer-events: none;
}

.cover-wrap :deep(img) {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.popular-row:hover .cover-wrap :deep(img) {
  transform: scale(1.04);
}

.row-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
}

.row-time {
  margin: 0;
  font-size: 13px;
  line-height: 1.35;
  color: #5b6675;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.row-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.38;
  letter-spacing: -0.01em;
  color: #131a24;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.18s ease;
}

.popular-row:hover .row-title {
  color: var(--ss-navy, #0b2545);
}

.row-meta {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  font-size: 13px;
  line-height: 1.4;
  color: #737373;
}

.row-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-icon {
  flex-shrink: 0;
  color: #8a94a3;
}

.skeleton-row {
  pointer-events: none;
}

.sk-time {
  height: 13px;
  width: 44%;
}

.sk-title {
  height: 18px;
  width: 84%;
}

.sk-loc {
  height: 13px;
  width: 60%;
}

.section-state {
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

.state-btn {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid rgba(21, 21, 21, 0.14);
  background: #fff;
  color: #151515;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

@media (max-width: 768px) {
  .popular-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 6px;
    margin-inline: -8px;
  }

  .popular-row {
    gap: 14px;
    padding: 8px;
  }

  .cover-wrap {
    width: 80px;
    height: 80px;
    border-radius: 12px;
  }

  .row-title {
    font-size: 15px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .popular-row,
  .cover-wrap :deep(img),
  .row-title {
    transition: none;
  }
}
</style>
