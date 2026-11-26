<template>
  <div class="media-page">
    <div class="page-header">
      <h1 class="page-title">媒体中心</h1>
      <p class="page-subtitle">大会现场图片与视频花絮</p>
    </div>

    <!-- Filter Tabs -->
    <el-card class="filter-card">
      <el-radio-group v-model="filterType" @change="loadMedia(1)" size="large">
        <el-radio-button label="">
          <Icon name="grid" :size="15" class="tab-icon" />
          全部
        </el-radio-button>
        <el-radio-button label="image">
          <Icon name="image" :size="15" class="tab-icon" />
          图片
        </el-radio-button>
        <el-radio-button label="video">
          <Icon name="video" :size="15" class="tab-icon" />
          视频
        </el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- Media Grid -->
    <div class="media-grid" v-loading="loading">
      <div v-if="items.length === 0" class="empty-state">
        <Icon name="camera" :size="48" class="empty-icon" />
        <p>暂无媒体内容</p>
      </div>
      
      <el-card 
        v-for="item in items" 
        :key="item.id" 
        class="media-card"
        shadow="hover"
        @click="openPreview(item)"
      >
        <div class="media-thumbnail">
          <div v-if="item.type === 'image'" class="thumbnail-image">
            <img v-if="getThumbSrc(item)" :src="getThumbSrc(item)" class="thumbnail-cover" alt="缩略图" />
            <Icon v-else name="image" :size="40" class="media-icon" />
            <div class="media-type-badge image-badge">
              <Icon name="image" :size="13" class="badge-icon" />
              图片
            </div>
          </div>
          <div v-else class="thumbnail-video">
            <Icon name="play-circle" :size="40" class="media-icon" />
            <div class="media-type-badge video-badge">
              <Icon name="video" :size="13" class="badge-icon" />
              视频
            </div>
          </div>
        </div>
        <div class="media-info">
          <h3 class="media-title">{{ item.title }}</h3>
          <p class="media-summary" v-if="item.summary">{{ item.summary }}</p>
          <div class="media-footer">
            <span class="media-date">
              <Icon name="calendar" :size="14" class="date-icon" />
              {{ formatDate(item.createdAt) }}
            </span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        @current-change="loadMedia"
      />
    </div>

    <!-- Preview Dialog -->
    <el-dialog 
      v-model="previewVisible" 
      :title="currentMedia?.title"
      width="80%"
      center
      destroy-on-close
    >
      <div class="preview-container">
        <img 
          v-if="currentMedia?.type === 'image'" 
          :src="previewUrl" 
          class="preview-image"
          alt="预览图片"
        />
        <video 
          v-else 
          :src="previewUrl" 
          controls 
          autoplay
          class="preview-video"
        ></video>
        <p v-if="currentMedia?.summary" class="preview-description">
          {{ currentMedia.summary }}
        </p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 媒体中心页。
 *
 * - 展示图片/视频等媒体资产，支持按类型筛选与预览。
 * - 通过文件服务的预签名 URL 获取可访问的临时链接。
 *
 * 依赖接口：
 * - GET `/content/media`：媒体列表（可按 type 过滤）
 * - GET `/file/presigned`：获取预览链接
 *
 * 数据字段主要对应后端 `MediaAssetDto`：`id/title/summary/type/objectName/coverUrl/articleId/createdAt`。
 */
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import Icon from '../components/ui/Icon.vue'
import api from '../api'

const items = ref<any[]>([])
const total = ref(0)
const pageSize = ref(12)
const filterType = ref('')
const loading = ref(false)

const previewVisible = ref(false)
const previewUrl = ref('')
const currentMedia = ref<any>(null)
const thumbUrlMap = ref<Record<string, string>>({})

function isProbablyUrl(value: any) {
  if (!value || typeof value !== 'string') return false
  return /^https?:\/\//i.test(value) || value.startsWith('/') || value.startsWith('data:') || value.startsWith('blob:')
}

function resolveObjectName(item: any) {
  return item?.objectName || item?.coverUrl || ''
}

function getThumbSrc(item: any) {
  if (!item) return ''
  const cached = thumbUrlMap.value[String(item.id)]
  if (cached) return cached
  if (isProbablyUrl(item.coverUrl)) return item.coverUrl
  return ''
}

async function ensureThumbUrls(list: any[]) {
  const imageItems = (list || []).filter((x) => x?.type === 'image')
  const targets = imageItems.filter((x) => {
    const id = String(x.id)
    if (thumbUrlMap.value[id]) return false
    if (isProbablyUrl(x.coverUrl)) return false
    return !!resolveObjectName(x)
  })

  await Promise.allSettled(
    targets.map(async (item) => {
      const objectName = resolveObjectName(item)
      const { data } = await api.get('/file/presigned', { params: { objectName, expireSeconds: 600 } })
      if (data.code === 0 && data.data) {
        thumbUrlMap.value[String(item.id)] = data.data
      }
    })
  )
}

async function loadMedia(page = 1) {
  loading.value = true
  try {
    const params: any = {
      page: page - 1,
      size: pageSize.value
    }
    
    if (filterType.value) {
      params.type = filterType.value
    }
    
    const { data } = await api.get('/content/media', { params })
    
    if (data.code === 0) {
      items.value = data.data.items
      total.value = data.data.total
      ensureThumbUrls(items.value)
    }
  } catch (error) {
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function openPreview(item: any) {
  previewUrl.value = ''
  currentMedia.value = item
  try {
    if (item?.type === 'image' && isProbablyUrl(item.coverUrl)) {
      previewUrl.value = item.coverUrl
      previewVisible.value = true
      return
    }

    const objectName = resolveObjectName(item)
    if (!objectName) {
      ElMessage.error('缺少文件标识，无法预览')
      return
    }

    const { data } = await api.get('/file/presigned', { params: { objectName, expireSeconds: 600 } })
    if (data.code === 0 && data.data) {
      previewUrl.value = data.data
      previewVisible.value = true
    } else {
      ElMessage.error(data.message || '获取预览链接失败')
    }
  } catch (error) {
    ElMessage.error('获取预览链接失败')
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadMedia(1)
})
</script>

<style scoped>
.media-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  padding: 8px 0 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #d8e0e5;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #0b2545;
  margin-bottom: 6px;
}

.tab-icon {
  margin-right: 6px;
}

.page-subtitle {
  font-size: 14px;
  color: #506273;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 8px;
  border: 1px solid #d8e0e5;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
  min-height: 300px;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.media-card {
  border-radius: 8px;
  border: 1px solid #d8e0e5;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.media-card:hover {
  border-color: #94a3b8;
  box-shadow: 0 6px 18px rgba(11, 37, 69, 0.08);
}

.media-thumbnail {
  height: 200px;
  position: relative;
  overflow: hidden;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  background: #eef2f6;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumbnail-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-video {
  width: 100%;
  height: 100%;
  background: #0b2545;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-icon {
  font-size: 52px;
  color: rgba(255, 255, 255, 0.72);
}

.thumbnail-image .media-icon {
  color: #94a3b8;
}

.media-type-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 9px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: white;
  display: inline-flex;
  align-items: center;
}

.badge-icon {
  margin-right: 4px;
}

.image-badge {
  background: rgba(11, 37, 69, 0.78);
}

.video-badge {
  background: rgba(15, 118, 110, 0.88);
}

.media-info {
  padding: 16px;
}

.media-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-summary {
  font-size: 13px;
  color: #666;
  margin: 0 0 12px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}

.media-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.media-date {
  font-size: 12px;
  color: #999;
  display: inline-flex;
  align-items: center;
}

.date-icon {
  margin-right: 4px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

.preview-container {
  text-align: center;
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 8px;
}

.preview-video {
  width: 100%;
  max-height: 70vh;
  border-radius: 8px;
}

.preview-description {
  margin-top: 20px;
  color: #666;
  font-size: 14px;
  line-height: 1.8;
  text-align: left;
}
</style>
