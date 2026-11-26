<template>
  <div class="ai-assistant">
    <transition name="ai-pop">
      <div v-if="open" class="ai-panel">
        <div class="ai-head">
          <div class="ai-title">
            <span class="ai-avatar">🤖</span>
            <div>
              <div class="ai-name">大会智能助手</div>
              <div class="ai-mode">{{ modeLabel }}</div>
            </div>
          </div>
          <button class="ai-close" @click="open = false">×</button>
        </div>

        <div ref="msgBox" class="ai-messages">
          <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
            <div class="bubble">{{ m.content }}</div>
          </div>
          <div v-if="loading" class="msg assistant">
            <div class="bubble typing">正在思考<span>.</span><span>.</span><span>.</span></div>
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
            placeholder="问我日程、报名、资料、购票…"
            @keyup.enter.exact.prevent="send()"
          />
          <el-button type="primary" :loading="loading" @click="send()">发送</el-button>
        </div>
      </div>
    </transition>

    <button class="ai-fab" :class="{ active: open }" @click="toggle" :title="open ? '收起助手' : '打开大会智能助手'">
      {{ open ? '×' : '🤖' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

interface Msg { role: 'user' | 'assistant'; content: string }

const open = ref(false)
const loading = ref(false)
const input = ref('')
const messages = ref<Msg[]>([
  { role: 'assistant', content: '你好，我是西湖论剑·数字安全大会智能助手。可以帮你查日程、报名、直播回放、资料下载和购票。' }
])
const suggestions = ref<string[]>(['帮我看看日程', '怎么报名参会？', '资料在哪里下载？', '购票支持哪些方式？'])
const modeLabel = ref('离线规则引擎')
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
        ? `大模型 · ${data.data.model}`
        : '离线规则引擎（未配置 API Key）'
    }
  } catch {
    modeLabel.value = '离线规则引擎'
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
      modeLabel.value = data.data.mode === 'llm' ? '大模型在线' : '离线规则引擎'
    } else {
      messages.value.push({ role: 'assistant', content: data.message || '服务暂时不可用，请稍后再试。' })
    }
  } catch {
    ElMessage.error('智能助手暂时不可用')
    messages.value.push({ role: 'assistant', content: '网络开小差了，请稍后再试。' })
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

.ai-fab {
  width: 56px; height: 56px; border-radius: 50%; border: none; cursor: pointer;
  font-size: 24px; color: #fff;
  background: linear-gradient(135deg, #ff0050, #00f2ea);
  box-shadow: 0 8px 24px rgba(255, 0, 80, .35);
  transition: transform .2s;
}
.ai-fab:hover { transform: scale(1.06); }
.ai-fab.active { background: #333; }

.ai-panel {
  position: absolute; right: 0; bottom: 72px;
  width: 380px; max-width: calc(100vw - 32px);
  height: 540px; max-height: calc(100vh - 140px);
  background: #fff; border-radius: 16px; overflow: hidden;
  box-shadow: 0 16px 48px rgba(0, 0, 0, .22);
  display: flex; flex-direction: column;
}

.ai-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; color: #fff;
  background: linear-gradient(135deg, #ff0050, #00f2ea);
}
.ai-title { display: flex; align-items: center; gap: 10px; }
.ai-avatar { font-size: 26px; }
.ai-name { font-weight: 600; font-size: 15px; }
.ai-mode { font-size: 11px; opacity: .85; }
.ai-close { background: transparent; border: none; color: #fff; font-size: 24px; cursor: pointer; line-height: 1; }

.ai-messages { flex: 1; overflow-y: auto; padding: 16px; background: #f7f8fa; }
.msg { display: flex; margin-bottom: 12px; }
.msg.user { justify-content: flex-end; }
.bubble {
  max-width: 82%; padding: 10px 14px; border-radius: 12px;
  font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-word;
}
.msg.assistant .bubble { background: #fff; color: #333; border-top-left-radius: 4px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.msg.user .bubble { background: linear-gradient(135deg, #ff0050, #ff4d7d); color: #fff; border-top-right-radius: 4px; }
.typing span { animation: blink 1.2s infinite; }
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%, 100% { opacity: .2 } 50% { opacity: 1 } }

.ai-suggest { padding: 8px 12px; display: flex; flex-wrap: wrap; gap: 6px; background: #fff; border-top: 1px solid #f0f0f0; }
.suggest-chip {
  font-size: 12px; padding: 4px 10px; border-radius: 12px; cursor: pointer;
  background: #fff0f5; color: #ff0050; border: 1px solid #ffd0e0;
}
.suggest-chip:hover { background: #ffe0eb; }

.ai-input { display: flex; gap: 8px; padding: 10px 12px; border-top: 1px solid #eee; background: #fff; align-items: flex-end; }
.ai-input .el-textarea { flex: 1; }

.ai-pop-enter-active, .ai-pop-leave-active { transition: all .2s ease; }
.ai-pop-enter-from, .ai-pop-leave-to { opacity: 0; transform: translateY(12px) scale(.98); }

@media (max-width: 768px) {
  .ai-assistant { right: 16px; bottom: 16px; }
  .ai-panel { width: calc(100vw - 32px); height: 70vh; }
}
</style>
