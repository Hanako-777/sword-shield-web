<template>
  <section class="partners" aria-labelledby="partners-title">
    <HomeSectionHead title="参展单位" title-id="partners-title" link-text="展开" to="/exhibitors" />

    <div v-if="loading && !exhibitors.length" class="state-card" aria-busy="true">
      <div class="skeleton-strip">
        <div v-for="n in 6" :key="n" class="ss-skeleton sk-logo" />
      </div>
    </div>

    <div v-else-if="error && !exhibitors.length" class="state-card state-row">
      <p>{{ error }}</p>
      <button type="button" class="state-btn" @click="loadExhibitors">重新加载</button>
    </div>

    <div v-else-if="!exhibitors.length" class="state-card state-row">
      <p>暂无参展单位信息</p>
      <router-link to="/exhibitors" class="state-btn">查看全部</router-link>
    </div>

    <div v-else ref="viewport" class="viewport" :class="{ paused }">
      <div class="track">
        <ul v-for="copy in 2" :key="copy" :aria-hidden="copy === 2 || undefined">
          <li v-for="item in exhibitors" :key="item.id">
            <img v-if="!failed.has(item.id)" :src="item.logoUrl" alt="" loading="lazy" @error="failed.add(item.id)" />
            <span>{{ item.name }}</span>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import api from '../../api'
import HomeSectionHead from './HomeSectionHead.vue'

const exhibitors = ref<{ id: number; name: string; logoUrl: string }[]>([])
const loading = ref(true)
const error = ref('')
const failed = reactive(new Set<number>())
const viewport = ref<HTMLElement>()
const paused = ref(true)
let controller = new AbortController()
let observer: IntersectionObserver | undefined
let visible = false

function sync() {
  paused.value = document.hidden || !visible
}

watch(viewport, el => {
  observer?.disconnect()
  if (!el) return
  observer = new IntersectionObserver(([entry]) => {
    visible = entry.isIntersecting
    sync()
  })
  observer.observe(el)
}, { flush: 'post' })

async function loadExhibitors() {
  controller.abort()
  controller = new AbortController()
  const signal = controller.signal
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get('/content/exhibitors', { params: { page: 0, size: 12 }, signal })
    if (signal.aborted) return
    if (data.code === 0) {
      exhibitors.value = (data.data?.items || []).filter((item: { logoUrl?: string }) => item.logoUrl)
    } else {
      error.value = data.message || '参展单位加载失败，请稍后重试'
    }
  } catch (err: any) {
    if (!signal.aborted) {
      console.error('加载参展单位失败', err)
      error.value = err?.message || '参展单位加载失败，请稍后重试'
    }
  } finally {
    if (!signal.aborted) loading.value = false
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', sync)
  loadExhibitors()
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', sync)
  controller.abort()
  observer?.disconnect()
})
</script>

<style scoped>
.viewport {
  overflow: hidden;
  padding: 20px 0;
  border-radius: 16px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.viewport:hover {
  border-color: rgba(21, 21, 21, 0.16);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.track,
ul {
  display: flex;
  width: max-content;
}

.track {
  animation: travel 56s linear infinite;
}

.paused .track,
.viewport:hover .track {
  animation-play-state: paused;
}

ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

li {
  width: 156px;
  display: grid;
  place-items: center;
  justify-items: center;
  gap: 8px;
  padding: 0 12px;
  border-right: 1px solid rgba(21, 21, 21, 0.08);
}

img {
  width: 105px;
  height: 42px;
  object-fit: contain;
}

li span {
  font-size: 13px;
  font-weight: 500;
  color: #737373;
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.3s ease;
}

li:hover span {
  color: #151515;
}

.state-card {
  padding: 20px 24px;
  border-radius: 16px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #fff;
}

.state-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
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

.state-btn:hover {
  border-color: rgba(21, 21, 21, 0.28);
  background: rgba(21, 21, 21, 0.03);
}

.skeleton-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
}

.sk-logo {
  height: 44px;
}

@keyframes travel {
  to {
    transform: translate3d(-50%, 0, 0);
  }
}

@media (max-width: 640px) {
  .skeleton-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (prefers-reduced-motion: reduce) {
  .viewport {
    overflow-x: auto;
  }

  .track {
    animation: none;
  }

  ul[aria-hidden='true'] {
    display: none;
  }
}
</style>
