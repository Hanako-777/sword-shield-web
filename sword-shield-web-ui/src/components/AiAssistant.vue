<template>
  <div class="ai-assistant">
    <transition name="ai-pop">
      <div v-if="open" class="ai-panel">
        <div class="ai-head">
          <div class="ai-title">
            <div>
              <div class="ai-name">大会咨询</div>
              <div class="ai-mode">{{ modeLabel }}</div>
            </div>
          </div>
          <button type="button" class="ai-close" aria-label="关闭咨询窗口" @click="open = false">
            <Icon name="x" :size="18" />
          </button>
        </div>

        <div ref="msgBox" class="ai-messages">
          <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
            <div class="bubble">{{ m.content }}</div>
          </div>
          <div v-if="loading" class="msg assistant">
            <div class="bubble typing">正在回复<span>.</span><span>.</span><span>.</span></div>
          </div>
        </div>

        <div v-if="messages.length <= 1 && suggestions.length" class="ai-suggest">
          <span v-for="s in suggestions" :key="s" class="suggest-chip" @click="send(s)">{{ s }}</span>
        </div>

        <div class="ai-input">
          <el-input
            v-model="input"
            type="textarea"
            :rows="1"
            resize="none"
            maxlength="500"
            placeholder="询问日程、报名、资料或购票..."
            @keyup.enter.exact.prevent="send()"
          />
          <el-button type="primary" :loading="loading" @click="send()">发送</el-button>
        </div>
      </div>
    </transition>

    <button
      type="button"
      class="ai-fab"
      :class="{ active: open, 'home-fab': $route.path === '/' }"
      @click="toggle"
      :title="open ? '收起咨询' : '大会咨询'"
      :aria-label="open ? '收起咨询' : '打开大会咨询'"
    >
      <Icon v-if="!open" name="message-circle" :size="18" />
      <Icon v-else name="x" :size="18" />
      <span class="fab-label">{{ open ? '收起' : '大会咨询' }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import Icon from './ui/Icon.vue'

interface Msg { role: 'user' | 'assistant'; content: string }

const open = ref(false)
const loading = ref(false)
const input = ref('')
const messages = ref<Msg[]>([
  { role: 'assistant', content: '您好，可以向我咨询西湖论剑大会日程、报名方式、资料下载与购票相关问题。' }
])
const suggestions = ref<string[]>(['帮我看看日程', '怎么报名参会？', '资料在哪里下载？', '购票支持哪些方式？'])
const modeLabel = ref('在线问答')
const msgBox = ref<HTMLElement | null>(null)

let sessionId = localStorage.getItem('aiSessionId')
if (!sessionId) {
  sessionId = 's-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
  localStorage.setItem('aiSessionId', sessionId)
}

async function toggle() {
  open.value = !open.value
  if (open.value) {
    await nextTick()
    scrollBottom()
    loadStatus()
  }
}

async function loadStatus() {
  try {
    const { data } = await api.get('/ai/status')
    if (data.code === 0 && data.data) {
      modeLabel.value = data.data.mode === 'llm'
        ? `在线问答 · ${data.data.model}`
        : '会务知识库'
    }
  } catch {
    modeLabel.value = '会务知识库'
  }
}

async function send(text?: string) {
  const content = (text ?? input.value).trim()
  if (!content || loading.value) return
  messages.value.push({ role: 'user', content })
  input.value = ''
  loading.value = true
  await nextTick()
  scrollBottom()

  try {
    const { data } = await api.post('/ai/chat', { message: content, sessionId })
    if (data.code === 0) {
      messages.value.push({ role: 'assistant', content: data.data.answer })
      if (Array.isArray(data.data.suggestions) && data.data.suggestions.length) {
        suggestions.value = data.data.suggestions
      }
      modeLabel.value = data.data.mode === 'llm' ? '在线问答' : '会务知识库'
    } else {
      messages.value.push({ role: 'assistant', content: data.message || '服务暂时不可用，请稍后再试。' })
    }
  } catch {
    ElMessage.error('咨询服务暂时不可用')
    messages.value.push({ role: 'assistant', content: '网络连接异常，请稍后再试。' })
  } finally {
    loading.value = false
    await nextTick()
    scrollBottom()
  }
}

function scrollBottom() {
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}

onMounted(() => { loadStatus() })
</script>

<style scoped>
.ai-assistant { position: fixed; right: 24px; bottom: 24px; z-index: 2000; }

/* 悬浮入口：圆角矩形“大会咨询” */
.ai-fab {
  display: inline-flex; align-items: center; gap: 8px;
  height: 44px; padding: 0 18px 0 16px;
  border: 0; border-radius: 12px; cursor: pointer;
  color: #fff; background: var(--ss-navy);
  font-size: 14px; font-weight: 500;
  box-shadow: 0 10px 28px -10px rgba(11, 37, 69, .55);
  transition: background-color var(--ss-dur) var(--ss-ease);
}
.ai-fab.home-fab { outline: 2px solid white; outline-offset: 1px; }
.ai-fab.home-fab:focus-visible { outline: 2px solid rgba(21, 21, 21, .55); outline-offset: 2px; }
.ai-fab:hover { background: var(--ss-navy-hover); }
.ai-fab.active { background: var(--ss-ink); }
.fab-close { font-size: 20px; line-height: 1; }

/* 面板：白底、20px 圆角、浮层阴影 */
.ai-panel {
  position: absolute; right: 0; bottom: 56px;
  width: 380px; max-width: calc(100vw - 32px);
  height: 540px; max-height: calc(100vh - 120px);
  background: #fff; border-radius: var(--ss-r-lg); overflow: hidden;
  box-shadow: var(--ss-shadow-pop);
  display: flex; flex-direction: column;
}

.ai-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 16px 12px 20px;
  border-bottom: 1px solid var(--ss-line);
}
.ai-title { display: flex; align-items: center; gap: 10px; }
.ai-name { font-weight: 600; font-size: 16px; color: var(--ss-ink); letter-spacing: var(--ss-tracking-tight); }
.ai-mode { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--ss-muted); margin-top: 2px; }
.ai-mode::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: var(--ss-teal); }
.ai-close {
  display: grid; place-items: center; width: 32px; height: 32px;
  border: 0; border-radius: 50%; background: transparent; color: var(--ss-muted); cursor: pointer;
  transition: background-color var(--ss-dur) var(--ss-ease), color var(--ss-dur) var(--ss-ease);
}
.ai-close:hover { background: var(--ss-fill); color: var(--ss-ink); }

.ai-messages { flex: 1; overflow-y: auto; padding: 16px; background: #fff; }
.msg { display: flex; margin-bottom: 10px; }
.msg.user { justify-content: flex-end; }
.bubble {
  max-width: 84%; padding: 10px 14px; border-radius: 16px;
  font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word;
}
.msg.assistant .bubble { background: var(--ss-bg-soft); color: var(--ss-ink); border-bottom-left-radius: 6px; }
.msg.user .bubble { background: var(--ss-navy); color: #fff; border-bottom-right-radius: 6px; }
.typing { color: var(--ss-muted); }
.typing span { animation: blink 1.2s infinite; }
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%, 100% { opacity: .2 } 50% { opacity: 1 } }

/* 推荐问题：胶囊 */
.ai-suggest { padding: 4px 16px 12px; display: flex; flex-wrap: wrap; gap: 6px; background: #fff; }
.suggest-chip {
  font-size: 13px; padding: 6px 12px; border-radius: 8px; cursor: pointer;
  background: var(--ss-fill); color: var(--ss-ink-2);
  transition: background-color var(--ss-dur) var(--ss-ease), color var(--ss-dur) var(--ss-ease);
}
.suggest-chip:hover { background: var(--ss-fill-hover); color: var(--ss-ink); }

.ai-input { display: flex; gap: 8px; padding: 12px; border-top: 1px solid var(--ss-line); background: #fff; align-items: flex-end; }
.ai-input .el-textarea { flex: 1; }
.ai-input :deep(.el-textarea__inner) { border-radius: 18px; padding: 8px 14px; background: var(--ss-bg-soft); box-shadow: none; }
.ai-input :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 1px var(--ss-navy) inset; background: #fff; }
.ai-input .el-button { height: 36px; }

.ai-pop-enter-active, .ai-pop-leave-active { transition: opacity .18s var(--ss-ease), transform .18s var(--ss-ease); }
.ai-pop-enter-from, .ai-pop-leave-to { opacity: 0; transform: translateY(8px); }

@media (max-width: 768px) {
  .ai-assistant { right: 16px; bottom: 16px; }
  .ai-panel { width: calc(100vw - 32px); height: 72vh; }
}
@media (max-width: 480px) {
  .fab-label { display: none; }
  .ai-fab { width: 48px; height: 48px; padding: 0; justify-content: center; }
}
</style>
