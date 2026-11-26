<template>
  <section ref="hero" class="hero" :class="{ revealed, paused }" aria-labelledby="conference-title">
    <div v-for="layer in layers" :key="layer" class="scene" :class="{ lens: layer === 'lens' }"
      :aria-hidden="layer === 'lens' || undefined" :inert="layer === 'lens'">
      <div v-if="layer === 'lens'" class="watermark" aria-hidden="true">AI</div>
      <div class="field" aria-hidden="true" inert>
        <div v-for="slot in cards" :key="slot.index" class="position" :style="slot.style">
          <div class="arrival">
            <div class="drift" :ref="el => bindDrift(layer, slot.index, el)">
              <div class="print" v-if="layer === 'base' || lensEnabled">
                <template v-if="layer === 'base' || slot.altSrc">
                  <EventCover class="tint" :src="layer === 'base' ? slot.src : slot.altSrc" :index="slot.index" />
                  <div class="picture"><EventCover :src="layer === 'base' ? slot.src : slot.altSrc" :index="slot.index" /></div>
                </template>
                <div v-else class="picture picture--placeholder" />
              </div>
            </div>
          </div>
        </div>
      </div>
      <div ref="copies" class="copy">
        <div class="intro">
          <component :is="layer === 'base' ? 'h1' : 'div'" :id="layer === 'base' ? 'conference-title' : undefined" class="title">
            <span class="mask"><span class="line">西湖论剑</span></span>
            <span class="mask"><span class="line"><span class="gradient">数字安全大会</span></span></span>
          </component>
          <p class="subtitle">汇集大会议程、活动报名、资料下载与现场回顾。</p>
        </div>
        <div class="actions">
          <component :is="layer === 'base' ? 'a' : 'span'" :href="layer === 'base' ? '#home-explore' : undefined"
            class="learn" @pointerenter="enterLens" @pointerleave="closeLens" @pointercancel="closeLens" @click="explore">
            <span class="pill">了解更多</span>
          </component>
          <component :is="layer === 'base' ? RouterLink : 'span'" :to="layer === 'base' ? '/activities' : undefined" class="explore">
            探索活动<Icon name="arrow-right" :size="16" />
          </component>
        </div>
      </div>
    </div>
    <div class="fade" aria-hidden="true" />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, type ComponentPublicInstance } from 'vue'
import { RouterLink } from 'vue-router'
import Icon from '../ui/Icon.vue'
import EventCover from './EventCover.vue'
import { AI_LENS_ICONS, getHeroBasePool, getHeroLayout, shuffleWithSeed, type HeroRect } from './heroCovers'
import type { ConferenceEvent } from './programme'

const props = defineProps<{ events: ConferenceEvent[] }>()
// 每次进入页面生成随机种子：所有卡片随机落位，窗口缩放时保持稳定不闪烁
const baseSeed = Math.floor(Math.random() * 0xffffffff)
const aiSeed = Math.floor(Math.random() * 0xffffffff)
const basePool = computed(() => shuffleWithSeed(getHeroBasePool(props.events), baseSeed))
const aiIcons = shuffleWithSeed(AI_LENS_ICONS, aiSeed)
const hero = ref<HTMLElement>()
const copies = ref<HTMLElement[]>([])
const width = ref(window.innerWidth)
const containerWidth = ref(window.innerWidth)
const height = ref(window.innerHeight)
const titleGlyphBox = ref<HeroRect | undefined>(undefined)
const contentBoxes = ref<HeroRect[]>([])
const slots = computed(() =>
  getHeroLayout(width.value, height.value, {
    containerWidth: containerWidth.value,
    titleGlyphBox: titleGlyphBox.value,
    contentBoxes: contentBoxes.value
  })
)
const layers = ['base', 'lens'] as const
const cards = computed(() => {
  const pool = basePool.value
  // 按卡片距离光圈中心由近及远排序，保证圆圈内最核心的 7 张卡片不重复展示全部 7 个 AI App 图标
  const rankBySlotIndex = new Map<number, number>()
  ;[...slots.value]
    .sort((a, b) => Math.hypot(a.x, a.y) - Math.hypot(b.x, b.y))
    .forEach((slot, rank) => rankBySlotIndex.set(slot.index, rank))

  return slots.value.map((slot, listIdx) => {
    const centerRank = rankBySlotIndex.get(slot.index) ?? listIdx
    const s = (1 + (slot.z + 1.6) / 3).toFixed(3)
    return {
      index: slot.index,
      src: pool[slot.index % pool.length],
      altSrc: aiIcons[centerRank % aiIcons.length],
      style: {
        left: 'calc(50% + ' + slot.x + 'px)',
        top: 'calc(50% - ' + slot.y + 'px)',
        '--side': slot.side + 'px',
        '--s': s,
        '--delay': (1350 + slot.index * 70) + 'ms',
        zIndex: Math.round(slot.z * 10)
      }
    }
  })
})
const desktop = matchMedia('(hover: hover) and (pointer: fine) and (prefers-reduced-motion: no-preference)')
const reduced = matchMedia('(prefers-reduced-motion: reduce)')
const lensEnabled = ref(desktop.matches)
const revealed = ref(false)
const paused = ref(document.hidden)
const phases = Array.from({ length: 16 }, () => Math.random() * Math.PI * 2)
const drifts = { base: new Map<number, HTMLElement>(), lens: new Map<number, HTMLElement>() }
let frame = 0
let previous = 0
let time = 0
let inView = true
let observer: IntersectionObserver
let resize: ResizeObserver

function bindDrift(layer: typeof layers[number], index: number, el: Element | ComponentPublicInstance | null) {
  if (el instanceof HTMLElement) drifts[layer].set(index, el)
  else drifts[layer].delete(index)
}
function drift(stamp: number) {
  if (previous) time += (stamp - previous) / 1000
  previous = stamp
  for (const [index, el] of drifts.base) {
    const phase = phases[index]
    const x = Math.cos(.4 * time + 1.3 + phase) * 3.6
    const y = Math.sin(.5 * time + phase) * 6
    const transform = 'translate3d(' + x + 'px,' + y + 'px,0)'
    el.style.transform = transform
    const twin = drifts.lens.get(index)
    if (twin && lensEnabled.value) twin.style.transform = transform
  }
  frame = requestAnimationFrame(drift)
}
function getTranslateY(el: Element) {
  const tf = getComputedStyle(el).transform
  if (!tf || tf === 'none') return 0
  return new DOMMatrixReadOnly(tf).m42
}
function measureTitleGlyphBox(heroEl: HTMLElement, bounds: DOMRect): HeroRect | undefined {
  const lines = heroEl.querySelectorAll<HTMLElement>('.scene:not(.lens) .title .line')
  if (!lines.length) return undefined
  let left = Infinity
  let top = Infinity
  let right = -Infinity
  let bottom = -Infinity
  const range = document.createRange()
  for (const line of lines) {
    const ty = getTranslateY(line)
    range.selectNodeContents(line)
    const rect = range.getBoundingClientRect()
    if (!rect.width && !rect.height) continue
    left = Math.min(left, rect.left - bounds.left)
    right = Math.max(right, rect.right - bounds.left)
    top = Math.min(top, rect.top - ty - bounds.top)
    bottom = Math.max(bottom, rect.bottom - ty - bounds.top)
  }
  if (!Number.isFinite(top)) return undefined
  return { left, top, right, bottom }
}
function measureContentBoxes(heroEl: HTMLElement, bounds: DOMRect): HeroRect[] {
  const nodes = heroEl.querySelectorAll<HTMLElement>(
    '.scene:not(.lens) .subtitle, .scene:not(.lens) .pill, .scene:not(.lens) .explore'
  )
  const boxes: HeroRect[] = []
  for (const el of nodes) {
    const ty = getTranslateY(el)
    const rect = el.getBoundingClientRect()
    if (!rect.width && !rect.height) continue
    boxes.push({
      left: rect.left - bounds.left,
      right: rect.right - bounds.left,
      top: rect.top - ty - bounds.top,
      bottom: rect.bottom - ty - bounds.top
    })
  }
  return boxes
}
function measure() {
  if (!hero.value) return
  width.value = window.innerWidth
  containerWidth.value = hero.value.clientWidth || window.innerWidth
  height.value = hero.value.clientHeight
  const bounds = hero.value.getBoundingClientRect()
  titleGlyphBox.value = measureTitleGlyphBox(hero.value, bounds)
  contentBoxes.value = measureContentBoxes(hero.value, bounds)
  const r = Math.round(Math.min(0.28 * width.value, 0.55 * height.value))
  hero.value.style.setProperty('--lens-radius', r + 'px')
}
function enterLens(event: PointerEvent) {
  if (event.pointerType !== 'mouse' || !desktop.matches || paused.value || revealed.value) return
  revealed.value = true
}
function closeLens() { revealed.value = false }
function explore(event: MouseEvent) {
  const target = document.getElementById('home-explore')
  if (!target || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return
  event.preventDefault()
  closeLens()
  target.focus({ preventScroll: true })
  target.scrollIntoView({ behavior: reduced.matches ? 'instant' : 'smooth', block: 'start' })
}
function syncMotion() {
  cancelAnimationFrame(frame)
  previous = 0
  lensEnabled.value = desktop.matches
  paused.value = document.hidden || !inView
  if (!lensEnabled.value || paused.value) closeLens()
  if (!paused.value && !reduced.matches) frame = requestAnimationFrame(drift)
}
let mounted = false
onMounted(() => {
  mounted = true
  measure()
  document.fonts?.ready.then(() => { if (mounted) measure() })
  observer = new IntersectionObserver(([entry]) => { inView = entry.isIntersecting; syncMotion() })
  observer.observe(hero.value!)
  resize = new ResizeObserver(measure)
  resize.observe(hero.value!)
  copies.value.forEach(el => resize.observe(el))
  window.addEventListener('resize', measure)
  desktop.addEventListener('change', syncMotion)
  reduced.addEventListener('change', syncMotion)
  document.addEventListener('visibilitychange', syncMotion)
  document.addEventListener('keydown', closeLens)
  window.addEventListener('blur', closeLens)
  syncMotion()
})
onBeforeUnmount(() => {
  mounted = false
  cancelAnimationFrame(frame)
  resize.disconnect()
  observer.disconnect()
  window.removeEventListener('resize', measure)
  desktop.removeEventListener('change', syncMotion)
  reduced.removeEventListener('change', syncMotion)
  document.removeEventListener('visibilitychange', syncMotion)
  document.removeEventListener('keydown', closeLens)
  window.removeEventListener('blur', closeLens)
})
</script>

<style scoped>
.hero {
  --title-size: 72px; --radius: 0px; --lens-radius: 400px; --lens-bg: #fef9e2; --cx: 50%; --cy: 50%;
  --hero-inset: var(--ss-header-h);
  display: grid; position: relative; isolation: isolate; overflow: clip; overflow-clip-margin: 0;
  min-height: 100svh; color: #151515; background: #fff;
}
.scene { position: relative; padding: max(calc(var(--hero-inset) + 32px), 31svh) 20px max(120px, env(safe-area-inset-bottom)); }
.field, .fade, .watermark { position: absolute; pointer-events: none; }
.field { inset: 0; isolation: isolate; z-index: 1; user-select: none; }
.watermark {
  left: var(--cx); top: var(--cy); z-index: 0;
  transform: translate(-50%, -50%);
  font-family: 'Inter Variable', 'Inter', sans-serif;
  font-weight: 700;
  font-size: calc(var(--lens-radius) * 1.1);
  line-height: 1;
  letter-spacing: -0.04em;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, .04);
  user-select: none;
  white-space: nowrap;
}
.position { position: absolute; width: var(--side); aspect-ratio: 1; transform: translate(-50%,-50%); }
.arrival, .drift, .print { width: 100%; height: 100%; }
.arrival { animation: card-in .9s cubic-bezier(.16,1,.3,1) var(--delay) both; }
@keyframes card-in { from { opacity: 0; transform: translateY(24px) scale(.94); } to { opacity: 1; transform: none; } }
.print {
  position: relative; overflow: hidden; padding: calc(var(--side) * .056); border-radius: calc(var(--side) * .0875);
  border: 1px solid rgba(0,0,0,.06); background: linear-gradient(180deg,#fff,#f4f4f6);
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,.95),
    inset 0 0 0 1px rgba(255,255,255,.6),
    0 1px 2px rgba(0,0,0,.06),
    0 calc(10px * var(--s, 1)) calc(22px * var(--s, 1)) calc(-6px * var(--s, 1)) rgba(0,0,0,.14),
    0 calc(28px * var(--s, 1)) calc(56px * var(--s, 1)) calc(-18px * var(--s, 1)) rgba(0,0,0,.20),
    0 calc(56px * var(--s, 1)) calc(96px * var(--s, 1)) calc(-36px * var(--s, 1)) rgba(15,23,42,.18);
}
.tint { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; filter: blur(14px); opacity: .22; }
.picture { height: 100%; width: 100%; position: relative; border-radius: calc(var(--side) * .05); overflow: hidden; }
.picture--placeholder { background: #ececee; }
.picture img { width: 100%; height: 100%; object-fit: cover; display: block; }
.picture::after { content: ''; position: absolute; inset: 0; border: 1px solid rgba(0,0,0,.08); border-radius: inherit; }
.copy { position: relative; z-index: 2; width: min(100%, 680px); margin-inline: auto; text-align: center; pointer-events: none; }
.intro { min-height: calc(30svh - 25px); padding-bottom: 16px; }
.title { display: flex; flex-direction: column; font-family: 'Noto Sans SC', sans-serif; font-weight: 500; font-size: var(--title-size); line-height: 1.2; letter-spacing: -.01em; color: #151515; }
.mask { display: block; overflow: clip; overflow-clip-margin: 0; padding: 20px; margin: -20px; }
.line { display: block; animation: line-in 1.2s cubic-bezier(.16,1,.3,1) .25s both; }
.mask + .mask .line { animation-delay: .40s; }
@keyframes line-in { from { transform: translateY(170px); } to { transform: translateY(0); } }
.gradient { display: inline-block; padding: .15em; margin: -.15em; color: transparent;
  background: radial-gradient(circle farthest-corner at .15em .15em,#ab46dd 0%,#f31a7c 36%,#f8712b 51%,#eaab26 77%);
  background-clip: text; -webkit-background-clip: text; }
.subtitle { margin: 30px auto 0; max-width: 450px; font-size: 20px; line-height: 30px; color: #6b6b6b; text-wrap: balance;
  animation: rise .7s cubic-bezier(.33,1,.68,1) .95s both; }
@keyframes rise { from { opacity: 0; transform: translateY(14px); } to { opacity: 1; transform: translateY(0); } }
.actions { display: flex; align-items: center; flex-direction: column; gap: 23px; }
.learn, .pill, .explore { display: inline-flex; align-items: center; justify-content: center; }
.scene:not(.lens) .actions a { pointer-events: auto; }
.learn { border-radius: 100px; }
.pill { font-size: 18px; font-weight: 500; line-height: 26px; padding: 10px 16px; border: 2px solid transparent; border-radius: 100px;
  color: white; background: #151515; backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px);
  box-shadow: 0 3px 3px #00000008,0 8px 7px #0000000a,0 17px 14px #0000000d,0 35px 29px #0000000f,0 96px 80px #00000012;
  transition: background-color .3s cubic-bezier(.4,0,.2,1), border-color .3s cubic-bezier(.4,0,.2,1), color .3s cubic-bezier(.4,0,.2,1), box-shadow .3s cubic-bezier(.4,0,.2,1);
  animation: rise .7s cubic-bezier(.33,1,.68,1) 1.1s both; }
.explore { position: relative; gap: 5px; color: #737373; font-size: 16px; font-weight: 500; transition: color .2s;
  animation: rise .7s cubic-bezier(.33,1,.68,1) 1.25s both; }
.explore::after { content: ''; position: absolute; inset: -9px -4px; }
.explore:hover { color: #151515; }
.learn:focus-visible .pill, .explore:focus-visible { animation: none; }
.lens {
  position: absolute; inset: 0; z-index: 2; pointer-events: none;
  background: var(--lens-bg);
  clip-path: circle(var(--radius) at var(--cx) var(--cy));
  transition: clip-path .5s cubic-bezier(.2,.8,.2,1);
}
.revealed .lens { transition-duration: .7s; }
@media (hover:hover) and (pointer:fine) and (prefers-reduced-motion:no-preference) {
  .revealed { --radius: var(--lens-radius); }
}
.hero:has(a:focus-visible) .lens { visibility: hidden; }
.lens .title { color: #151515; }
.lens .gradient { background-image: linear-gradient(100deg, #059669 0%, #0284c7 48%, #2563eb 100%); }
.lens .subtitle { color: #665f52; }
.lens .pill { box-shadow: none; color: #151515; background: rgba(21,21,21,.08); border-color: transparent; }
.lens::after {
  content: ''; position: absolute; top: var(--cy); left: var(--cx); transform: translate(-50%,-50%);
  width: calc(2 * var(--radius)); height: calc(2 * var(--radius)); border: 1px solid rgba(21,21,21,.06); border-radius: 50%;
  transition: width .5s cubic-bezier(.2,.8,.2,1), height .5s cubic-bezier(.2,.8,.2,1);
}
.revealed .lens::after { transition-duration: .7s; }
.fade { z-index: 3; inset: auto 0 0; height: 180px; backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
  mask-image: linear-gradient(transparent,#000 95%); background: linear-gradient(transparent,#fff 99%); }
.paused :is(.arrival,.line,.subtitle,.pill,.explore) { animation-play-state: paused; }
@media(max-width:1000px) { .hero { --title-size: 63px; } }
@media(max-width:820px) { .hero { --title-size: 54px; } }
@media(max-width:650px) { .hero { --title-size: 43px; } .scene { padding-bottom: max(126px,calc(88px + env(safe-area-inset-bottom))); } }
@media(max-width:450px) { .hero { --title-size: 36px; } }
@media(prefers-reduced-motion:reduce) {
  .hero *, .hero *::after { animation: none !important; transition: none !important; }
  .drift { transform: none !important; }
  .lens { display: none; }
}
</style>
