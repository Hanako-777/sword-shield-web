import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import api from '../../api'

interface Activity {
  id: number
  title: string
  summary?: string
  coverUrl?: string
  startTime?: string
  endTime?: string
  location?: string
  heat?: number
}

export interface ConferenceEvent extends Activity {
  start: number
  end: number
  image: string
  tag: string
}

export const fallbackImages = [
  'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&h=450&fit=crop',
  'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800&h=450&fit=crop',
  'https://images.unsplash.com/photo-1563986768609-322da13575f3?w=800&h=450&fit=crop',
  'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=450&fit=crop',
  'https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=800&h=450&fit=crop',
  'https://images.unsplash.com/photo-1531482615713-2afd69097998?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1573164713988-8665fc963095?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1504384764586-bb4cdc1707b0?w=640&h=360&fit=crop',
  'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?w=640&h=360&fit=crop'
]

export async function readPages<T extends { id: number }>(
  path: string, params: Record<string, string>, signal: AbortSignal
) {
  const records = new Map<number, T>()
  let received = 0
  for (let page = 0; ; page++) {
    const { data } = await api.get(path, { params: { ...params, page, size: 50 }, signal })
    if (data.code !== 0) throw new Error(data.message)
    const batch: T[] = data.data.items
    const previousSize = records.size
    batch.forEach(item => records.set(item.id, item))
    received += batch.length
    if (received >= data.data.total || records.size === previousSize) break
  }
  return [...records.values()]
}

export function useProgramme() {
  const events = ref<ConferenceEvent[]>([])
  const total = ref(0)
  const loading = ref(true)
  const error = ref('')
  const now = ref(Date.now())
  const sorted = computed(() => events.value.filter(item => Number.isFinite(item.start)).sort((a, b) => a.start - b.start))
  const upcoming = computed(() => sorted.value.filter(item => item.start > now.value))
  const ongoing = computed(() => sorted.value.filter(item => item.start <= now.value && !(item.end < now.value)))
  const archived = computed(() => sorted.value.filter(item => item.end < now.value).reverse())
  let controller = new AbortController()
  let timer: number | undefined

  function syncClock() {
    window.clearInterval(timer)
    now.value = Date.now()
    if (!document.hidden && (upcoming.value.length || ongoing.value.some(item => Number.isFinite(item.end)))) {
      timer = window.setInterval(() => {
        now.value = Date.now()
        if (!upcoming.value.length && !ongoing.value.some(item => Number.isFinite(item.end))) window.clearInterval(timer)
      }, 1000)
    }
  }

  async function load() {
    controller.abort()
    controller = new AbortController()
    const signal = controller.signal
    loading.value = true
    error.value = ''
    try {
      const items = await readPages<Activity>('/activity/list', { orderBy: 'heat' }, signal)
      if (signal.aborted) return
      events.value = items.map((item, i) => ({
        ...item,
        start: Date.parse(item.startTime || ''),
        end: Date.parse(item.endTime || ''),
        image: item.coverUrl || fallbackImages[i % fallbackImages.length],
        tag: (item.heat ?? 0) > 80 ? '热门活动' : (item.heat ?? 0) > 50 ? '精选活动' : '近期活动'
      }))
      total.value = items.length
      syncClock()
    } catch (err: any) {
      if (!signal.aborted) {
        console.error('加载首页活动失败', err)
        error.value = err?.message || '活动数据加载失败，请稍后重试'
      }
    } finally {
      if (!signal.aborted) loading.value = false
    }
  }

  onMounted(() => {
    document.addEventListener('visibilitychange', syncClock)
    load()
  })
  onBeforeUnmount(() => {
    controller.abort()
    window.clearInterval(timer)
    document.removeEventListener('visibilitychange', syncClock)
  })
  return { events, total, loading, error, reload: load, upcoming, ongoing, archived, now }
}

