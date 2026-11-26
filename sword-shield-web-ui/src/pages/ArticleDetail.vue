<template>
  <div class="article-page">
    <div v-if="item" ref="progressBar" class="reading-progress" aria-hidden="true"></div>
    <div class="article-container">
      
      <!-- Navigation / Breadcrumb -->
      <div class="nav-header">
        <el-button link @click="goBack" class="back-link">
          <Icon name="arrow-left" :size="16" style="margin-right: 4px" />
          返回列表
        </el-button>
      </div>

      <ReadState v-if="loading || !item" :loading="loading" :error="error" empty="暂无资讯" @retry="load" />
      <article v-else ref="article" class="article-card">
        
        <!-- Article Header -->
        <header class="article-header">
          <div class="article-meta-top">
             <span class="ss-badge">
               {{ item.category || '通用' }}
             </span>
             <span class="publish-date">{{ formatDate(item.publishedAt) }}</span>
          </div>
          <h1 class="article-title">{{ item.title }}</h1>
        </header>

        <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" class="article-cover" />
        <!-- Summary / Lead -->
        <div class="article-summary" v-if="item.summary">
          <p>{{ item.summary }}</p>
        </div>



        <!-- Main Body -->
        <div class="article-body">
          <div class="typo-content" v-html="item.body"></div>
        </div>

        <!-- Action Bar (Like/Share) -->
        <div class="article-actions">
           <div class="pills">
            <el-button 
              :type="liked ? 'primary' : 'default'"
              size="large"
              @click="like" 
              :loading="liking"
            >
              <Icon name="star" :size="16" :filled="liked" style="margin-right: 6px" />
              {{ liked ? `已点赞 ${likeCount}` : (likeCount > 0 ? `点赞 ${likeCount}` : '点赞') }}
            </el-button>
            <el-button 
              size="large"
              @click="share" 
              :loading="sharing"
            >
              <Icon name="share" :size="16" style="margin-right: 6px" />
              分享
            </el-button>
          </div>
        </div>
      </article>

      <!-- Media Gallery Section -->
      <ReadState v-if="error && item && !loading" :error="error" @retry="load" />
      <section v-if="item && relatedMedia.length" v-reveal class="section-container media-section reveal">
        <h3 class="section-heading">
          <span class="icon-wrapper"><Icon name="video" :size="18" /></span>
          精彩花絮
        </h3>
        <div class="gallery-grid">
          <button type="button"
            v-for="media in relatedMedia" 
            :key="media.id" 
            class="media-card"
            @click="previewMedia(media)"
          >
            <div class="media-cover">
              <img 
                v-if="media.type === 'image' && media.thumbnailUrl" 
                :src="media.thumbnailUrl" 
                class="cover-img"
                :alt="media.title"
              />
              <img 
                v-else-if="media.type === 'video' && media.coverUrl" 
                :src="media.coverUrl" 
                class="cover-img"
                :alt="media.title"
              />
              <div class="placeholder-icon" v-else>
                 <Icon v-if="media.type === 'image'" name="image" :size="24" />
                 <Icon v-else name="play-circle" :size="24" />
              </div>
              
              <div class="media-badge" :class="media.type">
                {{ media.type === 'image' ? '图片' : '视频' }}
              </div>
              
              <div class="play-overlay" v-if="media.type === 'video'">
                 <Icon name="play-circle" :size="20" />
              </div>
            </div>
            <div class="media-info">
              <h4 class="media-title" :title="media.title">{{ media.title }}</h4>
            </div>
          </button>
        </div>
      </section>

      <!-- Comments Section -->
      <section v-if="item && !loading" v-reveal class="section-container comments-section reveal">
        <div class="section-title">
          <h3>评论 {{ comments.length > 0 ? `(${comments.length})` : '' }}</h3>
        </div>

        <div class="comment-input-area">
          <el-input 
            ref="commentInputRef"
            v-model="comment"
            type="textarea" 
            :rows="3"
            placeholder="写下你的想法..."
            maxlength="500"
            show-word-limit
            resize="none"
          />
          <div class="input-actions">
            <el-button 
              type="primary" 
              @click="postComment" 
              :loading="commenting"
            >
              发表评论
            </el-button>
          </div>
        </div>

        <div class="comments-list">
          <ReadState v-if="commentsLoading || commentsError" :loading="commentsLoading" :error="commentsError" @retry="loadComments" />
          <div v-else-if="comments.length === 0" class="empty-state">
            <el-empty description="暂无评论，来坐沙发！" :image-size="80" />
          </div>
          
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-avatar">
              {{ 'U' }}
            </div>
            <div class="comment-main">
              <div class="comment-header">
                <span class="user-name">用户_{{ c.userId != null ? String(c.userId).substring(0, 6) : '匿名' }}</span>
                <span class="time-ago">{{ formatDate(c.createdAt) }}</span>
              </div>
              <div class="comment-body">
                {{ c.content }}
              </div>
              <div class="comment-footer">
                <el-button 
                  size="small" 
                  @click="likeComment(c)" 
                  :loading="c._liking"
                  :type="c.isLiked ? 'primary' : 'default'"
                >
                  <Icon name="star" :size="14" :filled="!!c.isLiked" style="margin-right: 4px" />
                   {{ c.isLiked ? '已点赞' : '点赞' }} {{ c.likeCount ?? 0 }}
                </el-button>
                <el-button size="small" @click="focusCommentBox">回复</el-button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Media Preview Dialog -->
    <el-dialog 
      v-model="mediaPreviewVisible" 
      :title="currentMedia?.title"
      width="min(800px, calc(100% - 24px))"
      align-center
      destroy-on-close
      class="media-preview-dialog"
    >
      <div class="preview-container">
        <img 
          v-if="currentMedia?.type === 'image'" 
          :src="mediaPreviewUrl" 
          class="preview-img"
          alt="预览"
        />
        <video 
          v-else 
          :src="mediaPreviewUrl" 
          controls 
          autoplay
          class="preview-video"
        ></video>
        <div v-if="currentMedia?.summary" class="preview-desc">
          {{ currentMedia.summary }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 资讯/文章详情页。
 *
 * - 展示文章正文与元信息（分类、发布时间等）。
 * - 支持评论/点赞/分享互动，并展示关联媒体资源（图片/视频）与下载/预览能力。
 * - 支持生成“分享海报”（后端异步任务：创建任务 → 轮询状态 → 打开结果 URL）。
 *
 * 依赖接口（部分）：
 * - GET `/content/article`
 * - GET `/content/media/by-article`、GET `/content/media`
 * - GET `/file/presigned`（下载/预览）
 * - GET `/interaction/comments`、POST `/interaction/comment`
 * - POST `/interaction/like`、GET `/interaction/likes/count`、POST `/interaction/share`
 * - POST `/content/poster`、GET `/content/poster/jobs/:jobId`
 */
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/ui/Icon.vue'
import api from '../api'
import { ElMessage } from 'element-plus'

import ReadState from '../components/detail/ReadState.vue'
import { useReadState } from '../components/detail/useReadState'
import { vReveal } from '../directives/reveal'

const { loading, error, run: readPage } = useReadState()
const { loading: commentsLoading, error: commentsError, run: readComments } = useReadState()
function load() {
  return readPage(fetchDetail, '加载失败，请稍后重试')
}
function loadComments() {
  return readComments(fetchComments, '评论加载失败')
}
const route = useRoute()
const router = useRouter()
const item = ref<any>(null)
const comments = ref<any[]>([])
const comment = ref('')
const likeCount = ref(0)
const liked = ref(false)
const commenting = ref(false)
const liking = ref(false)
const sharing = ref(false)
const commentInputRef = ref<any>(null)


const relatedMedia = ref<any[]>([])
const mediaPreviewVisible = ref(false)
const mediaPreviewUrl = ref('')
const currentMedia = ref<any>(null)

function getRouteId(): string {
  const paramId = Array.isArray(route.params.id) ? route.params.id[0] : (route.params.id as any)
  const queryId = Array.isArray(route.query.id) ? route.query.id[0] : (route.query.id as any)
  const id = paramId ?? queryId
  return id == null ? '' : String(id)
}

function getTargetId(): string | number {
  const idStr = getRouteId()
  const n = Number(idStr)
  return Number.isNaN(n) ? idStr : n
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  const fallback = item.value?.category === 'UGC' ? '/submissions' : '/articles'
  router.push(fallback)
}

async function focusCommentBox() {
  await nextTick()
  commentInputRef.value?.focus?.()
}


async function fetchDetail() {
  const id = getRouteId()
  if (!id) {
    ElMessage.error('未指定文章 ID')
    return
  }

  const { data } = await api.get('/content/article', { params: { id } })
  if (data.code !== 0) throw new Error(data.message || '资讯加载失败')
  item.value = data.data
  if (!item.value) return

  const token = localStorage.getItem('token')
  if (token) {
    const stateRes = await api.get('/interaction/likes/state', {
      params: { targetType: 'article', targetId: getTargetId() },
    })
    if (stateRes.data.code === 0) {
      likeCount.value = stateRes.data.data.count
      liked.value = !!stateRes.data.data.liked
    } else {
      liked.value = false
    }
  }

  if (!token || !liked.value) {
    const likeCountRes = await api.get('/interaction/likes/count', {
      params: { targetType: 'article', targetId: getTargetId() },
    })
    if (likeCountRes.data.code === 0) {
      likeCount.value = likeCountRes.data.data.count
    }
  }
  
  // Load related media
  await loadRelatedMedia()
}

async function loadRelatedMedia() {
  try {
    const { data } = await api.get('/content/media/by-article', {
      params: { articleId: getTargetId() }
    })
    if (data.code === 0) {
      relatedMedia.value = data.data
      
      // Load thumbnail URLs for all media
      await Promise.all(relatedMedia.value.map(async (media: any) => {
        try {
          const { data: urlData } = await api.get('/file/presigned', {
            params: {
              objectName: media.objectName,
              expireSeconds: 3600
            }
          })
          if (urlData.code === 0) {
            media.thumbnailUrl = urlData.data
          }
        } catch (error) {
          console.error(`Failed to load thumbnail for ${media.id}`, error)
        }
      }))
    }
  } catch (error) {
    console.error('Failed to load related media', error)
  }
}

async function previewMedia(media: any) {
  try {
    const { data } = await api.get('/file/presigned', {
      params: {
        objectName: media.objectName,
        expireSeconds: 600
      }
    })
    
    if (data.code === 0) {
      mediaPreviewUrl.value = data.data
      currentMedia.value = media
      mediaPreviewVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取预览链接失败')
  }
}

async function fetchComments() {
  const { data } = await api.get('/interaction/comments', { 
    params: { 
      targetType: 'article', 
      targetId: getTargetId(), 
      page: 0, 
      size: 10 
    } 
  })
  if (data.code !== 0) throw new Error(data.message || '评论加载失败')
  if (data.code === 0) {
    comments.value = data.data.items
    const token = localStorage.getItem('token')
    await Promise.all(comments.value.map(async (c: any) => {
      c._liking = false
      c.isLiked = false
      if (token) {
        const { data: state } = await api.get('/interaction/likes/state', { params: { targetType: 'comment', targetId: c.id } })
        if (state?.code === 0) {
          c.likeCount = state?.data?.count ?? 0
          c.isLiked = !!state?.data?.liked
          return
        }
      }
      const { data: cnt } = await api.get('/interaction/likes/count', { params: { targetType: 'comment', targetId: c.id } })
      c.likeCount = cnt?.data?.count ?? 0
    }))
  }
}

async function postComment() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (!comment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  
  commenting.value = true
  try {
    const { data } = await api.post('/interaction/comment', { 
      targetType: 'article', 
      targetId: getTargetId(), 
      content: comment.value 
    })
    if (data.code === 0) {
      comment.value = ''
      ElMessage.success('评论成功')
      await loadComments()
    } else {
      ElMessage.error(data.message || '评论失败')
    }
  } catch (error) {
    ElMessage.error('评论失败，请稍后重试')
  } finally {
    commenting.value = false
  }
}

async function like() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // Prevent multiple simultaneous clicks
  if (liking.value) return
  
  liking.value = true
  try {
    if (liked.value) {
      const { data } = await api.post('/interaction/unlike', { targetType: 'article', targetId: getTargetId() })
      if (data.code === 0) {
        likeCount.value = data.data.count
        liked.value = false
        ElMessage.success('已取消点赞')
      } else {
        ElMessage.error(data.message || '操作失败')
      }
      return
    }

    const { data } = await api.post('/interaction/like', { targetType: 'article', targetId: getTargetId() })
    if (data.code === 0) {
      likeCount.value = data.data.count
      liked.value = true
      ElMessage.success('点赞成功')
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    liking.value = false
  }
}

async function share() {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  const previewWindow = window.open('', '_blank')
  sharing.value = true
  try {
    if (previewWindow) {
      previewWindow.document.title = '海报生成中...'
      previewWindow.document.body.innerHTML = '<div style="font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial; padding: 28px; color: #0f172a;"><h2 style="margin: 0 0 10px;">海报生成中...</h2><p style="margin: 0; color: #475569;">请稍候，生成完成后会自动跳转。</p></div>'
    }

    const url = window.location.href
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(url)
    }
    const { data } = await api.post('/interaction/share', {
      targetType: 'article',
      targetId: getTargetId(),
      channel: 'link'
    })
    if (data.code === 0) {
      ElMessage.success('已复制链接，分享成功')
    } else {
      ElMessage.error(data.message || '分享失败')
    }
    // Generate poster
    const nickname = localStorage.getItem('username') || '参会者'
    const params = new URLSearchParams()
    params.set('title', item.value?.title || '西湖论剑·资讯')
    params.set('subtitle', `来自 ${nickname} 的分享`)
    ElMessage.info('海报生成中，请稍候...')
    const posterJob = await api.post('/content/poster', params, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
    if (posterJob.data.code !== 0) {
      throw new Error(posterJob.data.message || '海报任务提交失败')
    }
    const jobId = posterJob.data?.data?.jobId
    if (!jobId) {
      throw new Error('海报任务返回 jobId 为空')
    }

    const start = Date.now()
    let posterUrl = ''
    while (Date.now() - start < 30_000) {
      const { data: statusRes } = await api.get(`/content/poster/jobs/${jobId}`)
      if (statusRes.code !== 0) {
        throw new Error(statusRes.message || '查询海报任务失败')
      }
      const status = statusRes?.data?.status
      if (status === 'SUCCESS') {
        posterUrl = statusRes?.data?.url || ''
        break
      }
      if (status === 'FAILED') {
        throw new Error(statusRes?.data?.errorMessage || '海报生成失败')
      }
      await new Promise((r) => setTimeout(r, 1000))
    }
    if (!posterUrl) {
      throw new Error('海报生成超时，请稍后重试')
    }

    if (previewWindow) {
      previewWindow.location.href = posterUrl
    } else {
      window.open(posterUrl, '_blank')
    }
  } catch (error: any) {
    if (previewWindow) {
      previewWindow.document.title = '海报生成失败'
      previewWindow.document.body.innerHTML = '<div style="font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial; padding: 28px; color: #0f172a;"><h2 style="margin: 0 0 10px;">海报生成失败</h2><p style="margin: 0; color: #475569;">请返回页面重试。</p></div>'
    }
    ElMessage.error(error?.message || '分享失败，请稍后重试')
  } finally {
    sharing.value = false
  }
}

async function likeComment(c: any) {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // Prevent multiple simultaneous clicks
  if (c._liking) return
  
  c._liking = true
  try {
    if (c.isLiked) {
      const { data } = await api.post('/interaction/unlike', { targetType: 'comment', targetId: c.id })
      if (data.code === 0) {
        c.likeCount = data.data.count
        c.isLiked = false
        ElMessage.success('已取消点赞')
      } else {
        ElMessage.error(data.message || '操作失败')
      }
      return
    }

    const { data } = await api.post('/interaction/like', { targetType: 'comment', targetId: c.id })
    if (data.code === 0) {
      c.likeCount = data.data.count
      c.isLiked = true
      ElMessage.success('点赞成功')
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    c._liking = false
  }
}

const article = ref<HTMLElement>()
const progressBar = ref<HTMLElement>()
let frame = 0
let resize: ResizeObserver | undefined
function measureProgress() {
  frame = 0
  if (!article.value || !progressBar.value) return
  const rect = article.value.getBoundingClientRect()
  const header = parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--ss-header-h')) || 0
  const distance = rect.height - (window.innerHeight - header)
  const fraction = distance <= 0 ? Number(rect.bottom <= window.innerHeight) : (header - rect.top) / distance
  progressBar.value.style.transform = 'scaleX(' + Math.min(1, Math.max(0, fraction)) + ')'
}
function queueProgress() {
  if (!frame) frame = requestAnimationFrame(measureProgress)
}
watch(article, el => {
  resize?.disconnect()
  if (!el) return
  resize = new ResizeObserver(queueProgress)
  resize.observe(el)
  queueProgress()
}, { flush: 'post' })
onMounted(() => {
  load()
  loadComments()
  window.addEventListener('scroll', queueProgress, { passive: true })
  window.addEventListener('resize', queueProgress)
})
onBeforeUnmount(() => {
  cancelAnimationFrame(frame)
  resize?.disconnect()
  window.removeEventListener('scroll', queueProgress)
  window.removeEventListener('resize', queueProgress)
})
</script>

<style scoped src="../components/detail/detail.css"></style>
<style scoped>
.article-page { color: var(--ss-ink); font-family: var(--ss-font); padding: 18px 0 56px; }
.article-container { width: min(100%, var(--ss-w-read)); margin-inline: auto; }
.reading-progress { position: fixed; top: var(--ss-header-h); inset-inline: 0; z-index: 10; height: 2px;
  background: var(--ss-navy); transform-origin: left; transform: scaleX(0); pointer-events: none; }
.nav-header { margin-bottom: 20px; }
.back-link { color: var(--ss-muted); }
.article-title { margin: 12px 0 20px; color: var(--ss-navy); font-size: 36px; font-weight: 600; line-height: 1.3; overflow-wrap: anywhere; }
.article-meta-top, .pills, .comment-header, .comment-footer, .section-heading { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.publish-date, .time-ago { color: var(--ss-muted); font-size: 13px; }
.article-cover { display: block; width: 100%; aspect-ratio: 16 / 9; object-fit: cover; border-radius: 22px; background: var(--ss-bg-soft); margin: 0 0 24px; }
.article-summary { margin-bottom: 26px; padding: 4px 0 4px 16px; border-left: 2px solid var(--ss-line-strong); color: var(--ss-muted); font-size: 17px; line-height: 1.8; }
.typo-content { font-size: 17px; line-height: 1.8; overflow-wrap: anywhere; }
.typo-content :deep(p), .typo-content :deep(ul), .typo-content :deep(ol) { margin-block: 0 1.15em; }
.typo-content :deep(ul), .typo-content :deep(ol) { padding-inline-start: 1.65em; }
.typo-content :deep(li + li) { margin-top: .35em; }
.typo-content :deep(h2), .typo-content :deep(h3) { color: var(--ss-navy); margin: 1.45em 0 .5em; line-height: 1.4; }
.typo-content :deep(img) { display: block; max-width: 100%; height: auto; border-radius: 20px; margin: 20px auto; }
.typo-content :deep(pre), .typo-content :deep(table) { display: block; max-width: 100%; overflow-x: auto; }
.typo-content :deep(a) { text-decoration: underline; text-underline-offset: .18em; }
.article-actions { margin-top: 28px; }
.pills .el-button { margin: 0; border-radius: 12px; }
.comment-footer .el-button { margin: 0; border-radius: 8px; }
.section-container { padding-top: 24px; border-top: 1px solid var(--ss-line); margin-top: 38px; }
.section-heading, .section-title h3 { color: var(--ss-navy); font-size: 21px; margin-bottom: 16px; }
.gallery-grid { display: grid; grid-template-columns: repeat(auto-fit,minmax(min(180px,100%),1fr)); gap: 14px; }
.media-card { overflow: hidden; text-align: left; color: inherit; font: inherit; cursor: pointer; padding: 0; border-radius: 20px; border: 1px solid var(--ss-line-strong); background: white; }
.media-cover { position: relative; display: grid; place-items: center; aspect-ratio: 16 / 9; background: var(--ss-bg-soft); }
.cover-img { width: 100%; height: 100%; object-fit: cover; }
.media-badge, .play-overlay { position: absolute; color: white; background: var(--ss-navy); border-radius: 20px; }
.media-badge { top: 10px; left: 10px; font-size: 12px; padding: 2px 8px; border-radius: 6px; }
.play-overlay { display: grid; place-items: center; width: 36px; height: 36px; }
.media-info { padding: 12px 14px; }
.preview-img, .preview-video { display: block; width: 100%; max-height: 65vh; object-fit: contain; }
.preview-desc { margin-top: 12px; }
.input-actions { margin: 12px 0 20px; text-align: right; }
.comment-item { display: flex; gap: 12px; padding: 18px 0; border-top: 1px solid var(--ss-line); }
.comment-avatar { flex: 0 0 36px; height: 36px; display: grid; place-items: center; border-radius: 50%; background: var(--ss-bg-soft); color: var(--ss-navy); }
.comment-main { min-width: 0; flex: 1; }
.comment-header { justify-content: space-between; font-size: 14px; }
.comment-body { white-space: pre-wrap; overflow-wrap: anywhere; margin: 8px 0 10px; }
@media(max-width:1200px) { .article-page { padding-top: 14px; } }
@media(max-width:960px) { .section-container { margin-top: 30px; } }
@media(max-width:640px) { .article-page { padding: 0 4px 36px; } .gallery-grid { grid-template-columns: 1fr; } .article-cover { border-radius: 20px; } }
</style>
