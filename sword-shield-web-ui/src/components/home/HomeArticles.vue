<template>
  <section class="home-articles" aria-labelledby="home-articles-title">
    <header class="articles-head">
      <h2 id="home-articles-title" class="articles-title">最新资讯</h2>
      <CategoryFilter
        v-model="selectedCategory"
        class="articles-filter"
        :options="filterOptions"
        aria-label="按分类筛选最新资讯"
      />
      <router-link :to="allArticlesTo" class="articles-more">
        <span>全部资讯</span>
        <Icon name="arrow-right" :size="16" />
      </router-link>
    </header>

    <div v-if="loading && !items.length" class="articles-grid" aria-busy="true">
      <div v-for="n in 4" :key="n" class="article-card skeleton-card">
        <div class="cover-box ss-skeleton" />
        <div class="card-body">
          <div class="ss-skeleton sk-pill" />
          <div class="ss-skeleton sk-title" />
          <div class="ss-skeleton sk-summary" />
        </div>
      </div>
    </div>

    <div v-else-if="error" class="section-state">
      <p>{{ error }}</p>
      <button type="button" class="state-btn" @click="fetchArticles">重新加载</button>
    </div>

    <div v-else-if="!items.length" class="section-state">
      <p>暂无资讯内容</p>
      <router-link :to="allArticlesTo" class="state-btn">前往新闻资讯</router-link>
    </div>

    <div v-else class="articles-grid" :class="{ 'is-loading': loading }">
      <router-link
        v-for="item in items"
        :key="item.id"
        :to="`/article/${item.id}`"
        class="article-card"
      >
        <div class="cover-box">
          <img
            v-if="item.coverUrl && !failedCovers.has(item.id)"
            :src="item.coverUrl"
            alt=""
            loading="lazy"
            @error="failedCovers.add(item.id)"
          />
          <div v-else class="cover-placeholder" aria-hidden="true">
            <Icon name="file-text" :size="24" />
          </div>
        </div>
        <div class="card-body">
          <span class="cat-pill">{{ item.category || '资讯' }}</span>
          <h3 class="card-title">{{ item.title }}</h3>
          <p class="card-summary">{{ item.summary || '点击查看资讯全文。' }}</p>
        </div>
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import api from '../../api'
import { useArticleCategories } from '../../composables/useArticleCategories'
import CategoryFilter from '../ui/CategoryFilter.vue'
import Icon from '../ui/Icon.vue'

interface ArticleItem {
  id: number
  title: string
  summary?: string
  category?: string
  coverUrl?: string
  publishedAt?: string
}

const selectedCategory = ref('')
const items = ref<ArticleItem[]>([])
const loading = ref(true)
const error = ref('')
const failedCovers = reactive(new Set<number>())
let controller = new AbortController()

const { filterOptions } = useArticleCategories({ activeCategory: selectedCategory })

const allArticlesTo = computed(() =>
  selectedCategory.value
    ? { path: '/articles', query: { category: selectedCategory.value } }
    : '/articles'
)

async function fetchArticles() {
  controller.abort()
  controller = new AbortController()
  const signal = controller.signal
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get('/content/articles', {
      params: {
        page: 0,
        size: 4,
        excludeCategory: 'UGC',
        category: selectedCategory.value || undefined
      },
      signal
    })
    if (signal.aborted) return
    if (data.code === 0) {
      items.value = data.data?.items || []
    } else {
      error.value = data.message || '资讯加载失败，请稍后重试'
    }
  } catch (err: any) {
    if (!signal.aborted) {
      console.error('加载首页资讯失败', err)
      error.value = err?.message || '资讯加载失败，请稍后重试'
    }
  } finally {
    if (!signal.aborted) loading.value = false
  }
}

watch(selectedCategory, fetchArticles)
onMounted(fetchArticles)
onBeforeUnmount(() => controller.abort())
</script>

<style scoped>
.articles-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px 16px;
  margin-bottom: 20px;
}

.articles-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: -0.015em;
  color: #151515;
  white-space: nowrap;
}

.articles-filter {
  justify-self: end;
  max-width: 100%;
}

.articles-more {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.4;
  color: #737373;
  white-space: nowrap;
  transition: color 0.2s ease;
}

.articles-more:hover {
  color: #151515;
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  transition: opacity 0.2s ease;
}

.articles-grid.is-loading {
  opacity: 0.6;
}

.article-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-radius: 14px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #fff;
  color: #151515;
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.article-card:hover {
  border-color: rgba(21, 21, 21, 0.16);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.cover-box {
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #f4f4f5;
}

.cover-box img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  background: #f4f4f5;
  color: #151515;
}

.card-body {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 14px 16px 16px;
  min-width: 0;
}

.cat-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  border-radius: 6px;
  background: rgba(21, 21, 21, 0.05);
  color: #525252;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
}

.card-title {
  margin: 10px 0 0;
  font-size: 16px;
  font-weight: 500;
  line-height: 1.45;
  color: #151515;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-summary {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #737373;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.skeleton-card {
  pointer-events: none;
}

.sk-pill {
  width: 44px;
  height: 20px;
  border-radius: 6px;
}

.sk-title {
  width: 90%;
  height: 18px;
  margin-top: 10px;
}

.sk-summary {
  width: 100%;
  height: 32px;
  margin-top: 8px;
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

.state-btn:hover {
  border-color: rgba(21, 21, 21, 0.28);
  background: rgba(21, 21, 21, 0.03);
}

@media (max-width: 960px) {
  .articles-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .articles-head {
    grid-template-columns: 1fr auto;
    row-gap: 14px;
  }

  .articles-title {
    grid-column: 1;
    grid-row: 1;
  }

  .articles-more {
    grid-column: 2;
    grid-row: 1;
  }

  .articles-filter {
    grid-column: 1 / -1;
    grid-row: 2;
    justify-self: stretch;
    width: 100%;
  }

  .articles-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 12px;
  }
}
</style>
