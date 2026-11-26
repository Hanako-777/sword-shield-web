<template>
  <section class="ss-container programme" :aria-label="heading">
    <div class="head">
      <h2>{{ heading }}</h2>
      <div class="controls">
        <button type="button" aria-label="上一张活动卡片" :disabled="atStart" @click="move(-1)"><Icon name="arrow-left" :size="16" /></button>
        <button type="button" aria-label="下一张活动卡片" :disabled="atEnd" @click="move(1)"><Icon name="arrow-right" :size="16" /></button>
      </div>
    </div>
    <div ref="rail" class="rail" tabindex="0" role="region" :aria-label="heading"
      @scroll.passive="measure" @keydown.left.prevent="move(-1)" @keydown.right.prevent="move(1)"
      @pointerdown="beginDrag" @pointermove="drag" @pointerup="endDrag" @pointercancel="endDrag" @pointerleave="leaveRail"
      @click.capture="guardClick" @dragstart.prevent>
      <router-link v-for="(item, i) in items" :key="item.id" :to="'/activity/' + item.id" class="card">
        <div class="cover">
          <EventCover :src="item.image" :index="i" loading="lazy" />
          <time class="date" :datetime="item.startTime">
            <small>{{ new Date(item.start).getMonth() + 1 }}月</small><b>{{ new Date(item.start).getDate() }}</b>
          </time>
        </div>
        <div class="body">
          <span class="meta"><i class="ss-dot" :class="dot" />{{ status }} <span>·</span> {{ item.tag }}</span>
          <h3>{{ item.title }}</h3>
          <p class="summary">{{ item.summary || '点击查看活动详情' }}</p>
          <p class="place"><Icon name="map-pin" :size="14" />{{ item.location || '待定' }}</p>
        </div>
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import Icon from '../ui/Icon.vue'
import type { ConferenceEvent } from './programme'
import EventCover from './EventCover.vue'

const props = defineProps<{ items: ConferenceEvent[]; heading: string; status: string; dot: string }>()
const rail = ref<HTMLElement>()
const atStart = ref(true)
const atEnd = ref(false)
let resize: ResizeObserver
let mouse: { x: number; left: number; dragging: boolean } | undefined
let skipClick = false
function beginDrag(event: PointerEvent) {
  skipClick = false
  if (event.pointerType === 'mouse' && event.button === 0 && rail.value)
    mouse = { x: event.clientX, left: rail.value.scrollLeft, dragging: false }
}
function drag(event: PointerEvent) {
  const el = rail.value
  if (!el || !mouse) return
  const delta = event.clientX - mouse.x
  if (!mouse.dragging && Math.abs(delta) < 6) return
  if (!mouse.dragging) {
    el.setPointerCapture(event.pointerId)
    el.classList.add('dragging')
    mouse.dragging = true
  }
  el.scrollLeft = mouse.left - delta
}
function endDrag() {
  skipClick = !!mouse?.dragging
  mouse = undefined
  rail.value?.classList.remove('dragging')
}
function leaveRail() {
  if (!mouse?.dragging) mouse = undefined
}
function guardClick(event: MouseEvent) {
  if (skipClick && event.detail) { event.preventDefault(); event.stopPropagation() }
  skipClick = false
}
function measure() {
  const el = rail.value
  if (!el) return
  atStart.value = el.scrollLeft < 2
  atEnd.value = el.scrollLeft + el.clientWidth >= el.scrollWidth - 2
}
function move(direction: number) {
  const el = rail.value
  const card = el?.firstElementChild
  if (!el || !card) return
  const step = card.getBoundingClientRect().width + parseFloat(getComputedStyle(el).columnGap)
  el.scrollBy({ left: direction * step, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' })
}
onMounted(() => {
  resize = new ResizeObserver(measure)
  resize.observe(rail.value!)
  measure()
})
watch(() => props.items, measure, { flush: 'post' })
onBeforeUnmount(() => resize.disconnect())
</script>

<style scoped>
.programme { margin-top: clamp(40px, 5vw, 72px); }
.head, .controls, .meta, .place { display: flex; align-items: center; }
.head { justify-content: space-between; margin-bottom: 18px; }
h2 { font-size: clamp(25px, 3vw, 36px); color: var(--ss-navy); }
.controls { gap: 7px; }
button { display: grid; place-items: center; width: 38px; height: 38px; border: 1px solid var(--ss-line-strong);
  border-radius: 50%; background: white; color: var(--ss-navy); cursor: pointer; }
button:disabled { opacity: .35; cursor: default; }
button:enabled:hover { background: var(--ss-bg-soft); }
.rail { display: grid; grid-auto-flow: column; grid-auto-columns: min(82%, 348px); gap: 18px;
  overflow-x: auto; overscroll-behavior-x: contain; scroll-snap-type: x mandatory; scrollbar-width: none;
  padding: 2px 2px 12px; }
.rail::-webkit-scrollbar { display: none; }
.rail.dragging { scroll-snap-type: none; user-select: none; cursor: grabbing; }
.card { overflow: hidden; border: 1px solid var(--ss-line); border-radius: 22px; color: var(--ss-ink); scroll-snap-align: start; }
.card:focus-visible { outline-offset: -3px; }
.cover { position: relative; aspect-ratio: 16 / 9; overflow: hidden; background: var(--ss-bg-soft); }
.cover img { display: block; width: 100%; height: 100%; object-fit: cover; transition: transform .5s var(--ss-ease); }
.date { position: absolute; top: 14px; left: 14px; display: grid; text-align: center; min-width: 46px;
  overflow: hidden; border-radius: 10px; background: #fff; color: var(--ss-navy); }
.date small { background: #e6f4f1; color: var(--ss-teal-ink); font-size: 11px; line-height: 18px; }
.date b { font-size: 21px; line-height: 29px; }
.body { padding: 17px 18px 20px; }
.meta, .place, .summary { font-size: 13px; color: var(--ss-muted); }
.meta, .place { gap: 8px; }
h3 { margin: 8px 0 5px; font-size: 19px; line-height: 1.45; }
.summary, .place { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.place { display: block; margin-top: 15px; }
.place .el-icon { vertical-align: -.1em; margin-right: 5px; }
@media (hover: hover) and (prefers-reduced-motion: no-preference) {
  .card:hover img { transform: scale(1.045); }
}
</style>
