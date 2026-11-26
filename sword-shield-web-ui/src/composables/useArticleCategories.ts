import { computed, onBeforeUnmount, onMounted, ref, type Ref } from 'vue'
import { readPages } from '../components/home/programme'

export interface ArticleCategoryEntry {
  name: string
  count: number
}

export interface CategoryFilterOption {
  label: string
  value: string
  count: number | null
}

export interface UseArticleCategoriesOptions {
  /** 是否在挂载时自动拉取分类统计（默认 true） */
  immediate?: boolean | Ref<boolean>
  /** 当前选中的分类名；若其当前篇数为 0，仍保留在 options 中以便选中并展示空态 */
  activeCategory?: Ref<string>
}

export function useArticleCategories(options: UseArticleCategoriesOptions = {}) {
  const categories = ref<ArticleCategoryEntry[]>([])
  const totalCount = ref<number | null>(null)
  const loading = ref(false)
  const error = ref('')
  let controller = new AbortController()

  const filterOptions = computed<CategoryFilterOption[]>(() => {
    const opts: CategoryFilterOption[] = [
      { label: '全部', value: '', count: totalCount.value }
    ]
    const seen = new Set<string>()
    for (const entry of categories.value) {
      seen.add(entry.name)
      opts.push({ label: entry.name, value: entry.name, count: entry.count })
    }
    const active = options.activeCategory?.value?.trim()
    if (active && active.toUpperCase() !== 'UGC' && !seen.has(active)) {
      opts.push({
        label: active,
        value: active,
        count: totalCount.value !== null ? 0 : null
      })
    }
    return opts
  })

  async function load() {
    controller.abort()
    controller = new AbortController()
    const signal = controller.signal
    loading.value = true
    error.value = ''
    try {
      const articles = await readPages<{ id: number; category?: string }>(
        '/content/articles',
        { excludeCategory: 'UGC' },
        signal
      )
      if (signal.aborted) return
      const counts = new Map<string, number>()
      let total = 0
      for (const { category: rawCat } of articles) {
        const name = rawCat?.trim()
        if (name && name.toUpperCase() === 'UGC') continue
        total++
        if (name) {
          counts.set(name, (counts.get(name) ?? 0) + 1)
        }
      }
      totalCount.value = total
      categories.value = [...counts.entries()]
        .map(([name, count]) => ({ name, count }))
        .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name, 'zh-CN'))
    } catch (err: any) {
      if (!signal.aborted) {
        console.error('加载资讯分类统计失败', err)
        error.value = err?.message || '分类统计加载失败'
      }
    } finally {
      if (!signal.aborted) loading.value = false
    }
  }

  onMounted(() => {
    const shouldRun =
      options.immediate === undefined
        ? true
        : typeof options.immediate === 'boolean'
          ? options.immediate
          : options.immediate.value
    if (shouldRun) load()
  })

  onBeforeUnmount(() => controller.abort())

  return {
    categories,
    totalCount,
    filterOptions,
    loading,
    error,
    reload: load
  }
}
