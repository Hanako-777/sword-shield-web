<template>
  <div class="articles-container" :class="{ 'is-minimal': minimal }">
    <template v-if="!minimal">
      <header class="ss-page-head articles-head">
        <h1 class="ss-page-title articles-title">新闻资讯</h1>
      </header>

      <CategoryFilter
        class="articles-filter"
        :options="filterOptions"
        :model-value="category"
        aria-label="按分类筛选资讯"
        @update:model-value="selectCategory"
      />

      <div v-if="loading && !items.length" class="article-list" aria-busy="true">
        <div v-for="n in 4" :key="n" class="article-row skeleton-row">
          <div class="article-text">
            <div class="ss-skeleton" style="width: 24%; height: 14px" />
            <div class="ss-skeleton" style="width: 72%; height: 22px; margin-top: 10px" />
            <div class="ss-skeleton" style="width: 90%; height: 14px; margin-top: 10px" />
          </div>
        </div>
      </div>

      <el-empty v-else-if="!items.length" description="暂无资讯" />

      <ul v-else class="article-list">
        <li v-for="item in items" :key="item.id">
          <router-link :to="`/article/${item.id}`" class="article-row ss-row">
            <div class="article-text">
              <div class="article-meta">
                <span class="category-pill" :data-type="getCategoryType(item.category)">{{ item.category || '通用' }}</span>
                <span class="publish-date">{{ formatDate(item.publishedAt) }}</span>
              </div>
              <h2 class="article-title">{{ item.title }}</h2>
              <p class="article-summary">{{ item.summary || '暂无摘要' }}</p>
              <span class="article-more">阅读全文 →</span>
            </div>
            <img v-if="item.coverUrl" class="article-thumb" :src="item.coverUrl" alt="" loading="lazy" />
          </router-link>
        </li>
      </ul>
    </template>

    <ul v-else class="minimal-list">
      <li v-for="item in shownItems" :key="item.id" class="minimal-item">
        <a :href="`/article/${item.id}`" @click.prevent="$router.push(`/article/${item.id}`)" class="minimal-link">
          <span class="title">{{ item.title }}</span>
          <span class="minimal-meta">
            <span class="category-pill" :data-type="getCategoryType(item.category)">{{ item.category }}</span>
            <span class="publish-date">{{ formatDate(item.publishedAt) }}</span>
          </span>
        </a>
      </li>
    </ul>

    <div v-if="!minimal && total > pageSize" class="pagination-wrapper">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 资讯列表页面 / 组件。
 *
 * - `minimal` 模式用于首页：仅展示标题列表。
 * - 默认模式展示资讯卡片，并提供分页。
 *
 * 依赖接口：
 * - GET `/content/articles`：分页获取资讯列表（通过 `excludeCategory=UGC` 排除用户投稿）。
 *
 * 数据字段主要对应后端 `ContentArticleDto`：`id/title/summary/category/publishedAt` 等。
 */
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import CategoryFilter from '../components/ui/CategoryFilter.vue'
import { useArticleCategories } from '../composables/useArticleCategories'

const props = defineProps<{ minimal?: boolean; limit?: number }>()
const route = useRoute()
const router = useRouter()
const category = computed(() => {
  const value = route.query.category
  return !props.minimal && typeof value === 'string' && value.trim().toUpperCase() !== 'UGC' ? value.trim() : ''
})

const { filterOptions } = useArticleCategories({
  immediate: computed(() => !props.minimal),
  activeCategory: category
})

function selectCategory(nextCategory: string) {
  if (nextCategory === category.value) return
  const nextQuery = { ...route.query }
  if (nextCategory) {
    nextQuery.category = nextCategory
  } else {
    delete nextQuery.category
  }
  router.replace({ path: route.path, query: nextQuery })
}

/** 精简模式下最多展示的条数（首页用，未传则全部展示） */
const shownItems = computed(() => (props.limit ? items.value.slice(0, props.limit) : items.value))

const items = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const loading = ref(false)

function formatDate(dateStr: string) {
  if (!dateStr) return '日期未知'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function getCategoryType(category: string) {
  const types: Record<string, any> = {
    '技术': 'primary',
    '安全': 'danger',
    '行业': 'success',
    '通知': 'warning'
  }
  return types[category] || 'info'
}

let latestRequest = 0
async function fetchList() {
  const request = ++latestRequest
  loading.value = true
  try {
    const { data } = await api.get('/content/articles', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        excludeCategory: 'UGC',
        category: category.value || undefined
      }
    })
    if (request === latestRequest && data.code === 0) {
      items.value = data.data.items
      total.value = data.data.total
    }
  } finally {
    if (request === latestRequest) loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(category, () => {
  currentPage.value = 1
  items.value = []
  total.value = 0
  fetchList()
}, { immediate: true })
</script>

<style scoped>
.articles-container {
  width: min(100%, var(--ss-w-narrow));
  margin: 0 auto;
}
.articles-container.is-minimal {
  width: auto;
}

.articles-head {
  padding-bottom: 16px;
}

.articles-filter {
  margin-bottom: 20px;
}

.article-list {
  list-style: none;
  border-top: 1px solid var(--ss-line);
}
.article-list > li {
  border-bottom: 1px solid var(--ss-line);
}
.article-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
  padding: 22px 12px;
  margin: 0 -12px;
  color: inherit;
}
.article-meta,
.minimal-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}
.category-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 6px;
  background: var(--ss-fill);
  color: var(--ss-ink-2);
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}
/* 分类只用中性灰；“通知”用藏青浅底稍作区分，不引入额外点缀色 */
.category-pill[data-type='warning'] {
  background: var(--el-color-primary-light-9);
  color: var(--ss-navy);
}
.publish-date {
  font-size: 13px;
  color: var(--ss-muted);
}
.article-title {
  margin-top: 8px;
  font-size: 19px;
  font-weight: 600;
  line-height: 1.45;
  color: var(--ss-ink);
  transition: color var(--ss-dur) var(--ss-ease);
}
.article-row:hover .article-title {
  color: var(--ss-navy);
}
.article-summary {
  margin-top: 6px;
  font-size: 15px;
  line-height: 1.7;
  color: var(--ss-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.article-more {
  display: inline-block;
  margin-top: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--ss-navy);
}
.article-thumb {
  width: 132px;
  height: 96px;
  border-radius: var(--ss-r-md);
  object-fit: cover;
}
.skeleton-row {
  pointer-events: none;
}

/* ---- 精简模式（首页）：标题 + 分类 + 日期 ---- */
.minimal-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.minimal-link {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 10px;
  margin: 0 -10px;
  border-radius: var(--ss-r-md);
  color: var(--ss-ink);
  cursor: pointer;
  transition: background-color var(--ss-dur) var(--ss-ease);
}
.minimal-link:hover {
  background: var(--ss-row-hover);
}
.minimal-link .title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.45;
  transition: color var(--ss-dur) var(--ss-ease);
}
.minimal-link:hover .title {
  color: var(--ss-navy);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 8px 0 20px;
}

@media (max-width: 640px) {
  .article-row {
    gap: 14px;
    padding: 18px 8px;
    margin: 0 -8px;
  }
  .article-title {
    font-size: 17px;
  }
  .article-thumb {
    width: 84px;
    height: 84px;
  }
}
</style>
