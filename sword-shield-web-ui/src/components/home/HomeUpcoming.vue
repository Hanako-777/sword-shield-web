<template>
  <section class="home-upcoming" aria-labelledby="home-upcoming-title">
    <HomeSectionHead title="即将举办" title-id="home-upcoming-title" link-text="全部活动" to="/activities" />

    <div v-if="!items.length" class="section-state">
      <p>暂无近期重点议程</p>
      <router-link to="/activities" class="state-btn">查看活动日程</router-link>
    </div>

    <div v-else class="upcoming-grid" :style="{ '--upcoming-cols': cols }">
      <router-link
        v-for="(item, index) in items"
        :key="item.id || index"
        :to="item.id ? `/activity/${item.id}` : '/activities'"
        class="upcoming-card"
      >
        <div class="ss-date-block date-box" aria-hidden="true">
          <span class="ss-date-block__m">{{ dateBadge(item).top }}</span>
          <span class="ss-date-block__d date-day">{{ dateBadge(item).bottom }}</span>
        </div>
        <div class="card-info">
          <div class="title-row">
            <span v-if="item.status === 'ongoing'" class="live-dot" aria-hidden="true" />
            <h3 class="card-title">{{ item.title }}</h3>
          </div>
          <p class="card-meta">{{ item.dateRange }} · {{ item.location }}</p>
        </div>
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import HomeSectionHead from './HomeSectionHead.vue'

export interface FeaturedAgendaItem {
  id?: number
  title: string
  description: string
  type: string
  status: string
  dateRange: string
  location: string
  scale: string
}

const props = defineProps<{
  items: FeaturedAgendaItem[]
}>()

const cols = computed(() => Math.max(1, Math.min(props.items.length, 4)))

function dateBadge(item: FeaturedAgendaItem): { top: string; bottom: string } {
  if (item.dateRange.includes('11月26日') || item.dateRange.includes('今日')) return { top: '11月', bottom: '27日' }
  if (item.dateRange.includes('12月5日')) return { top: '12月', bottom: '5日' }
  if (item.dateRange.includes('1月15日') || item.dateRange.includes('1月9日')) return { top: '1月', bottom: '15日' }
  return { top: '近期', bottom: '议程' }
}
</script>

<style scoped>
.upcoming-grid {
  display: grid;
  grid-template-columns: repeat(var(--upcoming-cols, 4), minmax(0, 1fr));
  gap: 12px;
}

.upcoming-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #fff;
  color: #151515;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.upcoming-card:hover {
  border-color: rgba(21, 21, 21, 0.16);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.date-box {
  width: 48px;
  height: 48px;
  border-color: rgba(21, 21, 21, 0.1);
}

.date-day {
  font-size: 13px;
  font-weight: 600;
  color: #151515;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #14b8a6;
  flex-shrink: 0;
}

.card-title {
  margin: 0;
  min-width: 0;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.4;
  color: #151515;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.4;
  color: #737373;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
}

@media (max-width: 960px) {
  .upcoming-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .upcoming-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
