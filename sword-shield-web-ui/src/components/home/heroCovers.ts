import { fallbackImages, type ConferenceEvent } from './programme'

export interface HeroCover {
  id: string
  src: string
  /** 光圈层封面地址（使用非实拍 AI App 图标填充） */
  altSrc: string
}

/** 圆圈光圈层展示的 7 个 AI App 图标（Gemini / GPT / Claude / Grok / DeepSeek / GLM / Qwen） */
export const AI_LENS_ICONS = [
  '/hero/ai-icons/gemini.svg',
  '/hero/ai-icons/gpt.svg',
  '/hero/ai-icons/claude.svg',
  '/hero/ai-icons/grok.svg',
  '/hero/ai-icons/deepseek.svg',
  '/hero/ai-icons/glm.svg',
  '/hero/ai-icons/qwen.svg'
] as const

/** 基于种子做稳定 Fisher-Yates 洗牌，确保同一次访问缩放不跳动、每次刷新随机分布 */
export function shuffleWithSeed<T>(items: readonly T[], seed: number): T[] {
  const copy = [...items]
  let state = (seed >>> 0) || 1
  const nextRand = () => {
    state = (state + 0x6d2b79f5) >>> 0
    let t = Math.imul(state ^ (state >>> 15), 1 | state)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(nextRand() * (i + 1))
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
  }
  return copy
}

/** 汇总去重后的底层卡片图片池（活动封面 + 本地实景素材 + 兜底图，按无查询参数的路径去重防止同图重复） */
const localHeroMaterials = [
  '/hero/materials/robot-barista.jpg',
  '/hero/materials/robot-coffee-machine.jpg',
  '/hero/materials/robot-lab-arms.jpg',
  '/hero/materials/robot-lab-gripper.jpg',
  '/hero/materials/robot-lab-left.jpg',
  '/hero/materials/touch-screen-hand.jpg',
  '/hero/materials/touch-screen-wall.jpg'
] as const

export function getHeroBasePool(events: readonly ConferenceEvent[]): string[] {
  const seen = new Set<string>()
  const pool: string[] = []
  const normalizeKey = (src: string) => src.split('?')[0].trim()
  const push = (src?: string) => {
    if (!src) return
    const key = normalizeKey(src)
    if (!seen.has(key)) {
      seen.add(key)
      pool.push(src)
    }
  }
  for (const event of events) push(event.image)
  for (const img of localHeroMaterials) push(img)
  for (const img of fallbackImages) push(img)
  return pool
}

export function getHeroCovers(events: readonly ConferenceEvent[], seed = Date.now()): HeroCover[] {
  const basePool = shuffleWithSeed(getHeroBasePool(events), seed)
  const aiPool = shuffleWithSeed(AI_LENS_ICONS, seed ^ 0x9e3779b9)
  return Array.from({ length: 16 }, (_, index) => ({
    id: `poster-${String(index + 1).padStart(2, '0')}`,
    src: basePool[index % basePool.length],
    altSrc: aiPool[index % aiPool.length]
  }))
}

type Axis = 'x' | 'y'
type Slot = [fx: number, fy: number, size: number, z: number, axis?: Axis, pad?: number]

const slots: readonly Slot[] = [
  [-.09, .95, 220, 1.2, 'y'],
  [.55, .72, 165, .4, 'x', 8],
  [-.5, .6, 150, -.6, 'x', 30],
  [-.82, .28, 115, -1.4, 'x'],
  [-.66, -.16, 185, .9, 'x', 14],
  [.83, .26, 145, -1, 'x', 34],
  [.47, -.12, 135, -1.6, 'x', 16],
  [.68, -.5, 160, .2, 'x', 34],
  [-.38, -.75, 165, -.4, 'y'],
  [.28, -.95, 230, 1.4, 'y'],
  [-.97, -.75, 200, .6, 'x'],
  [.23, .88, 170, .8, 'y'],
  [-.78, .72, 140, -1.2, 'x'],
  [.9, -.15, 130, .5, 'x'],
  [-.05, -.98, 180, -.2, 'y'],
  [-.62, -.88, 140, -.9, 'y']
]

type Variant = [fx: number, fy: number, size: number, axis?: Axis, pad?: number]
const small: Partial<Record<number, Variant>> = {
  3: [-.7, .28, 130], 5: [.83, .3, 145, 'x', 44],
  7: [.65, -.45, 160], 9: [.3, -.98, 200], 10: [-1.05, -.75, 140]
}
const mobile: readonly Variant[] = [
  [-.12, .95, 135], [.55, 1.15, 150], [1.05, .28, 110, 'x'],
  [-1.05, .68, 125, 'x'], [-1.05, -.34, 130, 'x', 12], [1.05, -.42, 125, 'x', 10],
  [1.08, .6, 90, 'x'], [.55, -.85, 145], [-.3, -.92, 130],
  [-1.08, -.75, 95, 'x'], [-1.12, .1, 100, 'x']
]

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

export interface HeroRect {
  left: number
  top: number
  right: number
  bottom: number
}

export interface HeroLayoutOptions {
  containerWidth?: number
  titleGlyphBox?: HeroRect
  contentBoxes?: HeroRect[]
}

const DRIFT_X = 3.6
const DRIFT_Y = 6
const TOP_REST_BOUND = 24 // 静止态上沿下限 24px（计入 6px 漂浮后最高点不低于 18px）
const TITLE_TOP_GAP = 16
const TITLE_SIDE_PAD = 24
const TITLE_MIN_SCALE = 0.45
const MIN_CARD_GAP = 20
const MAX_OUTWARD_SHIFT = 60
const GAP_SHRINK_SCALE = 0.85

function rectsOverlap(a: HeroRect, b: HeroRect) {
  return a.right > b.left && a.left < b.right && a.bottom > b.top && a.top < b.bottom
}

/** 计入双方漂浮振幅（±3.6px, ±6px）后，两张卡片外框之间仍保持至少 20px 间距 */
function hasMinCardGap(a: HeroRect, b: HeroRect) {
  const dx = Math.max(b.left - a.right, a.left - b.right) - 2 * DRIFT_X
  const dy = Math.max(b.top - a.bottom, a.top - b.bottom) - 2 * DRIFT_Y
  return dx >= MIN_CARD_GAP || dy >= MIN_CARD_GAP
}

// x/y are the slot centers relative to the hero center, in CSS pixels (positive y points up).
export function getHeroLayout(width: number, height: number, options?: HeroLayoutOptions) {
  const scale = clamp((width - 650) / 790 * .2 + .8, .8, 1) * clamp(1 + (width - 1440) / 1120 * .3, 1, 1.3)
  const d = (width <= 450 ? 54 : width <= 650 ? 60 : width <= 820 ? 72 : width <= 1000 ? 80 : width > 1920 ? 118 : 92) / 92
  const isSmall = width > 650 && width <= 820
  const cw = options?.containerWidth && options.containerWidth > 0 ? options.containerWidth : width

  const raw = slots.flatMap(([fx, fy, size, z, axis = 'y', pad = 0], index) => {
    if ((index >= 11 && width < 1600) || (isSmall && index === 6)) return []
    if (width <= 450) {
      ;[fx, fy, size, axis = 'y', pad = 0] = mobile[index]
    } else if (isSmall && small[index]) {
      const variant = small[index]!
      fx = variant[0]
      fy = variant[1]
      size = variant[2]
      axis = variant[3] ?? axis
      pad = variant[4] ?? pad
    }
    const side = size * scale
    let x = fx * width / 2
    let y = fy * height / 2
    if (axis === 'x') {
      x = Math.sign(fx) * Math.max(Math.abs(x), Math.max(260 * d, width > 650 ? 245 : 0) + pad + side / 2)
    } else {
      y = Math.sign(fy) * Math.max(Math.abs(y), Math.max(250 * d, 225) + side / 2)
    }
    return [{ index, x, y, side, origSide: side, z }]
  })

  const titleSize = width <= 450 ? 36 : width <= 650 ? 43 : width <= 820 ? 54 : width <= 1000 ? 63 : 72
  const fallbackGlyphTop = Math.max(96, height * 0.25) - titleSize * 0.14
  const fallbackGlyphHalfW = titleSize * 3.12
  const glyphBox: HeroRect = options?.titleGlyphBox ?? {
    left: cw / 2 - fallbackGlyphHalfW,
    right: cw / 2 + fallbackGlyphHalfW,
    top: fallbackGlyphTop,
    bottom: fallbackGlyphTop + titleSize * 2.68
  }
  const forbidden: HeroRect = {
    left: glyphBox.left - TITLE_SIDE_PAD,
    right: glyphBox.right + TITLE_SIDE_PAD,
    top: glyphBox.top - TITLE_TOP_GAP,
    bottom: glyphBox.bottom
  }
  const contentBoxes: HeroRect[] = [glyphBox, ...(options?.contentBoxes ?? [])]
  const availH = Math.floor(forbidden.top - DRIFT_Y - TOP_REST_BOUND)

  const rectOf = (item: { x: number; y: number; side: number }): HeroRect => ({
    left: cw / 2 + item.x - item.side / 2,
    right: cw / 2 + item.x + item.side / 2,
    top: height / 2 - item.y - item.side / 2,
    bottom: height / 2 - item.y + item.side / 2
  })
  const driftRect = (r: HeroRect): HeroRect => ({
    left: r.left - DRIFT_X,
    right: r.right + DRIFT_X,
    top: r.top - DRIFT_Y,
    bottom: r.bottom + DRIFT_Y
  })
  const hitsContent = (r: HeroRect) => {
    const dr = driftRect(r)
    return contentBoxes.some(box => rectsOverlap(dr, box))
  }

  // 第一遍：上边界（24px）与标题禁区避让（垂直平移 → 原地缩小至 0.45 → 隐藏，不再横向推开）
  const kept: typeof raw = []
  for (const item of raw) {
    const r = rectOf(item)
    const overlapsForbX = r.right + DRIFT_X > forbidden.left && r.left - DRIFT_X < forbidden.right
    const hitsTop = width > 450 && r.top < TOP_REST_BOUND
    const hitsForbY = r.bottom + DRIFT_Y > forbidden.top && r.top - DRIFT_Y < forbidden.bottom

    if (width > 450 && overlapsForbX && (hitsTop || hitsForbY) && r.top < forbidden.bottom) {
      if (availH >= item.side) {
        const newTop = clamp(r.top, TOP_REST_BOUND, forbidden.top - DRIFT_Y - item.side)
        item.y = height / 2 - newTop - item.side / 2
      } else if (availH >= item.origSide * TITLE_MIN_SCALE) {
        item.side = availH
        item.y = height / 2 - TOP_REST_BOUND - item.side / 2
      } else {
        continue
      }
    } else if (hitsTop) {
      item.y = height / 2 - TOP_REST_BOUND - item.side / 2
    }
    kept.push(item)
  }

  // 第二遍：任意两张卡片计入漂浮后保持 ≥ 20px 间距（z 较小者先横向外移 ≤ 60px → 缩到 0.85 → 隐藏）
  kept.sort((a, b) => b.z - a.z)
  const placed: typeof raw = []
  for (const item of kept) {
    const fitsAll = (cand: HeroRect) =>
      !hitsContent(cand) && placed.every(other => hasMinCardGap(cand, rectOf(other)))

    if (fitsAll(rectOf(item))) {
      placed.push(item)
      continue
    }

    const dir = item.x < 0 ? -1 : 1
    let resolved = false

    for (let dx = 4; dx <= MAX_OUTWARD_SHIFT; dx += 4) {
      const candidate = { ...item, x: item.x + dir * dx }
      if (fitsAll(rectOf(candidate))) {
        item.x = candidate.x
        resolved = true
        break
      }
    }

    if (!resolved) {
      const shrunk = item.side * GAP_SHRINK_SCALE
      for (let dx = 0; dx <= MAX_OUTWARD_SHIFT; dx += 4) {
        const candidate = { ...item, x: item.x + dir * dx, side: shrunk }
        if (fitsAll(rectOf(candidate))) {
          item.x = candidate.x
          item.side = shrunk
          resolved = true
          break
        }
      }
    }

    if (resolved) placed.push(item)
  }

  placed.sort((a, b) => a.index - b.index)
  return placed.map(({ index, x, y, side, z }) => ({ index, x, y, side, z }))
}
