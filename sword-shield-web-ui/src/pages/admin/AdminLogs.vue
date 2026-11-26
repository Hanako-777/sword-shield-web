<template>
  <div class="admin-logs">
    <!-- 顶部统计概览卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card stat-card-1">
          <div class="stat-icon">
            <Icon name="monitor" :size="28" />
          </div>
          <div class="stat-content">
            <div class="stat-label">活跃日志服务</div>
            <div class="stat-value">
              {{ summary.activeLogServices }} <span class="stat-unit">/ {{ summary.totalServices }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-2">
          <div class="stat-icon">
            <Icon name="message-circle" :size="28" />
          </div>
          <div class="stat-content">
            <div class="stat-label">AI 问答审计（平均 {{ summary.aiAvgLatencyMs }}ms）</div>
            <div class="stat-value">{{ summary.aiChatTotal }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-3">
          <div class="stat-icon">
            <Icon name="credit-card" :size="28" />
          </div>
          <div class="stat-content">
            <div class="stat-label">支付回调流水（成功 {{ summary.paymentCallbackSuccess }}）</div>
            <div class="stat-value">{{ summary.paymentCallbackTotal }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-4">
          <div class="stat-icon">
            <Icon name="alert-triangle" :size="28" />
          </div>
          <div class="stat-content">
            <div class="stat-label">最近 ERROR 日志</div>
            <div class="stat-value">{{ summary.errorLogCount }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 三合一日志查看面板 -->
    <el-card class="logs-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- Tab 1: 微服务运行日志与全链路 TraceId 追踪 -->
        <el-tab-pane label="微服务运行日志（全链路追踪）" name="runtime">
          <div class="toolbar">
            <el-select
              v-model="runtimeQuery.service"
              placeholder="选择微服务"
              style="width: 210px"
              @change="fetchRuntimeLogs"
            >
              <el-option label="全部 9 个微服务" value="ALL" />
              <el-option
                v-for="svc in summary.services"
                :key="svc.name"
                :label="`${svc.label} (${svc.fileSizeKb}KB)`"
                :value="svc.name"
              />
            </el-select>

            <el-select
              v-model="runtimeQuery.level"
              placeholder="日志级别"
              style="width: 115px"
              @change="fetchRuntimeLogs"
            >
              <el-option label="全部级别" value="ALL" />
              <el-option label="INFO" value="INFO" />
              <el-option label="WARN" value="WARN" />
              <el-option label="ERROR" value="ERROR" />
              <el-option label="DEBUG" value="DEBUG" />
            </el-select>

            <el-input
              v-model="runtimeQuery.traceId"
              placeholder="按 TraceId 跨服务追踪..."
              clearable
              style="width: 230px"
              @keyup.enter="fetchRuntimeLogs"
              @clear="fetchRuntimeLogs"
            />

            <el-input
              v-model="runtimeQuery.keyword"
              placeholder="关键词（路径 / 订单号 / 异常）"
              clearable
              style="width: 220px"
              @keyup.enter="fetchRuntimeLogs"
              @clear="fetchRuntimeLogs"
            />

            <el-select
              v-model="runtimeQuery.limit"
              style="width: 105px"
              @change="fetchRuntimeLogs"
            >
              <el-option label="最近 50 条" :value="50" />
              <el-option label="最近 100 条" :value="100" />
              <el-option label="最近 200 条" :value="200" />
              <el-option label="最近 500 条" :value="500" />
            </el-select>

            <el-checkbox v-model="runtimeQuery.errorOnly" @change="fetchRuntimeLogs">
              仅错误日志
            </el-checkbox>

            <el-button type="primary" :loading="runtimeLoading" @click="refreshAllRuntime">
              <Icon name="refresh-cw" :size="15" style="margin-right: 4px" />
              <span>刷新</span>
            </el-button>

            <el-switch
              v-model="autoRefresh"
              active-text="5s 自动刷新"
              style="margin-left: auto"
            />
          </div>

          <el-alert
            v-if="runtimeQuery.traceId"
            type="info"
            :closable="false"
            show-icon
            class="trace-banner"
          >
            <template #title>
              正在跨微服务追踪 TraceId：<code>{{ runtimeQuery.traceId }}</code>（共命中 {{ runtimeTotalMatched }} 条记录）
              <el-button link type="primary" size="small" style="margin-left: 12px" @click="clearTraceFilter">
                清除追踪过滤
              </el-button>
            </template>
          </el-alert>

          <el-table
            :data="runtimeLogs"
            v-loading="runtimeLoading"
            size="small"
            stripe
            style="width: 100%"
            max-height="540"
            empty-text="暂无匹配的运行日志"
          >
            <el-table-column prop="timestamp" label="时间" width="190">
              <template #default="{ row }">
                <span class="mono-text">{{ row.timestamp }}</span>
              </template>
            </el-table-column>
            <el-table-column label="微服务" width="115">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :type="serviceTagType(row.service)">
                  {{ row.serviceLabel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="级别" width="76">
              <template #default="{ row }">
                <el-tag size="small" :type="levelTagType(row.level)" effect="dark">
                  {{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="TraceId（点击跨服务追踪）" width="180">
              <template #default="{ row }">
                <el-tooltip
                  v-if="isValidTraceId(row.traceId)"
                  :content="`点击追踪全链路：${row.traceId}`"
                  placement="top"
                >
                  <el-tag
                    size="small"
                    type="info"
                    class="trace-chip"
                    @click="trackByTraceId(row.traceId)"
                  >
                    {{ row.traceId.slice(0, 16) }}...
                  </el-tag>
                </el-tooltip>
                <span v-else class="muted-text">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="shortLogger" label="类/过滤器" width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="mono-text muted-logger">{{ row.shortLogger }}</span>
              </template>
            </el-table-column>
            <el-table-column label="日志内容" min-width="320">
              <template #default="{ row }">
                <div class="log-message-cell" @click="openRuntimeDetail(row)">
                  {{ row.message }}
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 2: AI 问答审计日志 -->
        <el-tab-pane label="AI 问答审计日志" name="ai">
          <div class="toolbar">
            <el-input
              v-model="aiQuery.keyword"
              placeholder="搜索提问 / 回复 / 用户名 / 会话 ID"
              clearable
              style="width: 280px"
              @keyup.enter="reloadAiLogs"
              @clear="reloadAiLogs"
            />
            <el-select
              v-model="aiQuery.model"
              placeholder="模型筛选"
              style="width: 170px"
              @change="reloadAiLogs"
            >
              <el-option label="全部模型" value="ALL" />
              <el-option v-for="m in aiModels" :key="m" :label="m" :value="m" />
            </el-select>
            <el-button type="primary" :loading="aiLoading" @click="fetchAiLogs">
              <Icon name="refresh-cw" :size="15" style="margin-right: 4px" />
              <span>刷新</span>
            </el-button>
          </div>

          <el-table
            :data="aiLogs"
            v-loading="aiLoading"
            stripe
            style="width: 100%"
            empty-text="暂无 AI 问答日志"
          >
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="用户" width="120">
              <template #default="{ row }">
                <span>{{ row.username }}</span>
                <span v-if="row.userId" class="muted-text"> (#{{ row.userId }})</span>
              </template>
            </el-table-column>
            <el-table-column prop="question" label="用户提问" min-width="200" show-overflow-tooltip />
            <el-table-column prop="answer" label="AI 回复摘要" min-width="260" show-overflow-tooltip />
            <el-table-column label="模型" width="135">
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="row.model === 'mock-rules' ? 'info' : 'success'"
                  effect="plain"
                >
                  {{ row.model || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="90">
              <template #default="{ row }">
                <span class="mono-text">{{ row.latencyMs }} ms</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="170">
              <template #default="{ row }">
                <span class="mono-text">{{ row.createdAt }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAiDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="aiTotal"
              :page-size="aiQuery.size"
              :page-sizes="[10, 15, 30, 50]"
              :current-page="aiQuery.page"
              @current-change="(p: number) => { aiQuery.page = p; fetchAiLogs() }"
              @size-change="(s: number) => { aiQuery.size = s; aiQuery.page = 1; fetchAiLogs() }"
            />
          </div>
        </el-tab-pane>

        <!-- Tab 3: 支付回调审计日志 -->
        <el-tab-pane label="支付回调审计日志" name="payment">
          <div class="toolbar">
            <el-input
              v-model="payQuery.orderNo"
              placeholder="搜索订单号 / 交易流水号 / 商品名"
              clearable
              style="width: 280px"
              @keyup.enter="reloadPayLogs"
              @clear="reloadPayLogs"
            />
            <el-select
              v-model="payQuery.channel"
              placeholder="回调来源/渠道"
              style="width: 155px"
              @change="reloadPayLogs"
            >
              <el-option label="全部来源" value="ALL" />
              <el-option label="CALLBACK 异步回调" value="CALLBACK" />
              <el-option label="USER_CONFIRM 确认支付" value="USER_CONFIRM" />
              <el-option label="MOCK 模拟支付" value="MOCK" />
              <el-option label="ALIPAY 支付宝" value="ALIPAY" />
              <el-option label="WECHAT 微信" value="WECHAT" />
            </el-select>
            <el-select
              v-model="payQuery.success"
              placeholder="回调结果"
              style="width: 130px"
              @change="reloadPayLogs"
            >
              <el-option label="全部结果" :value="-1" />
              <el-option label="验签成功" :value="1" />
              <el-option label="失败/异常" :value="0" />
            </el-select>
            <el-button type="primary" :loading="payLoading" @click="fetchPayLogs">
              <Icon name="refresh-cw" :size="15" style="margin-right: 4px" />
              <span>刷新</span>
            </el-button>
          </div>

          <el-table
            :data="payLogs"
            v-loading="payLoading"
            stripe
            style="width: 100%"
            empty-text="暂无支付回调流水"
          >
            <el-table-column prop="id" label="ID" width="65" />
            <el-table-column prop="orderNo" label="订单号" min-width="195">
              <template #default="{ row }">
                <span class="mono-text">{{ row.orderNo }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="金额" width="95">
              <template #default="{ row }">
                <span class="amount-text" v-if="row.amount != null">¥{{ Number(row.amount).toFixed(2) }}</span>
                <span v-else class="muted-text">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="username" label="用户" width="105" />
            <el-table-column label="渠道" width="90">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.channel || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="tradeNo" label="外部交易流水号" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="mono-text">{{ row.tradeNo || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="回调结果" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.success ? 'success' : 'danger'">
                  {{ row.success ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="165">
              <template #default="{ row }">
                <span class="mono-text">{{ row.createdAt }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="85" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openPayDetail(row)">报文</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="payTotal"
              :page-size="payQuery.size"
              :page-sizes="[10, 15, 30, 50]"
              :current-page="payQuery.page"
              @current-change="(p: number) => { payQuery.page = p; fetchPayLogs() }"
              @size-change="(s: number) => { payQuery.size = s; payQuery.page = 1; fetchPayLogs() }"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 运行日志详情弹窗 -->
    <el-dialog v-model="runtimeDetailVisible" title="运行日志详情" width="760px" destroy-on-close>
      <div v-if="currentRuntime" class="detail-box">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="微服务">{{ currentRuntime.serviceLabel }} ({{ currentRuntime.service }})</el-descriptions-item>
          <el-descriptions-item label="级别">
            <el-tag size="small" :type="levelTagType(currentRuntime.level)" effect="dark">
              {{ currentRuntime.level }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="时间戳">{{ currentRuntime.timestamp }}</el-descriptions-item>
          <el-descriptions-item label="线程">{{ currentRuntime.thread }}</el-descriptions-item>
          <el-descriptions-item label="TraceId" :span="2">
            <span class="mono-text">{{ isValidTraceId(currentRuntime.traceId) ? currentRuntime.traceId : '无' }}</span>
            <el-button
              v-if="isValidTraceId(currentRuntime.traceId)"
              link
              type="primary"
              size="small"
              style="margin-left: 10px"
              @click="trackByTraceId(currentRuntime.traceId); runtimeDetailVisible = false"
            >
              按此 TraceId 跨服务追踪
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="Logger" :span="2">
            <span class="mono-text">{{ currentRuntime.logger }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="detail-pre-title">完整日志内容</div>
        <pre class="detail-pre">{{ currentRuntime.message }}</pre>
      </div>
    </el-dialog>

    <!-- AI 问答详情弹窗 -->
    <el-dialog v-model="aiDetailVisible" title="AI 问答审计详情" width="740px" destroy-on-close>
      <div v-if="currentAi" class="detail-box">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="记录 ID">#{{ currentAi.id }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentAi.username }} {{ currentAi.userId ? `(#${currentAi.userId})` : '' }}</el-descriptions-item>
          <el-descriptions-item label="模型">{{ currentAi.model }}</el-descriptions-item>
          <el-descriptions-item label="响应耗时">{{ currentAi.latencyMs }} ms</el-descriptions-item>
          <el-descriptions-item label="会话 ID">{{ currentAi.sessionId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ currentAi.createdAt }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-pre-title">用户提问</div>
        <pre class="detail-pre">{{ currentAi.question }}</pre>
        <div class="detail-pre-title">AI 回复</div>
        <pre class="detail-pre">{{ currentAi.answer }}</pre>
      </div>
    </el-dialog>

    <!-- 支付回调报文详情弹窗 -->
    <el-dialog v-model="payDetailVisible" title="支付回调审计报文" width="700px" destroy-on-close>
      <div v-if="currentPay" class="detail-box">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ currentPay.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="外部交易号">{{ currentPay.tradeNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商品">{{ currentPay.productName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ currentPay.amount != null ? `¥${Number(currentPay.amount).toFixed(2)}` : '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付渠道">{{ currentPay.channel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="回调结果">
            <el-tag size="small" :type="currentPay.success ? 'success' : 'danger'">
              {{ currentPay.success ? '验签通过并更新订单' : '回调失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单用户">{{ currentPay.username }}</el-descriptions-item>
          <el-descriptions-item label="记录时间">{{ currentPay.createdAt }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-pre-title">原始回调 Payload</div>
        <pre class="detail-pre">{{ formatPayload(currentPay.payload) }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 管理后台 - 系统日志与审计页。
 *
 * 聚合展示：
 * 1. 微服务全链路运行日志（支持 TraceId 跨 9 个微服务串联追踪）；
 * 2. AI 智能助手问答审计日志（ai_chat_log）；
 * 3. 支付回调审计流水（payment_callback_log）。
 */
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import Icon from '../../components/ui/Icon.vue'
import adminApi from '../../adminApi'

type ServiceMeta = {
  name: string
  label: string
  fileExists: boolean
  fileSizeKb: number
  errorFileSizeKb: number
  errorCount: number
  lastModified?: string | null
}

type RuntimeLogItem = {
  service: string
  serviceLabel: string
  timestamp: string
  thread: string
  level: string
  logger: string
  shortLogger: string
  traceId: string
  message: string
}

type AiLogItem = {
  id: number
  userId?: number | null
  username: string
  sessionId?: string | null
  question: string
  answer: string
  model: string
  latencyMs: number
  createdAt: string
}

type PayLogItem = {
  id: number
  orderNo: string
  channel: string
  tradeNo: string
  payload: string
  success: boolean
  createdAt: string
  productName?: string | null
  amount?: number | null
  orderStatus?: string | null
  userId?: number | null
  username: string
}

const activeTab = ref<'runtime' | 'ai' | 'payment'>('runtime')

const summary = ref<{
  activeLogServices: number
  totalServices: number
  aiChatTotal: number
  aiAvgLatencyMs: number
  paymentCallbackTotal: number
  paymentCallbackSuccess: number
  errorLogCount: number
  services: ServiceMeta[]
}>({
  activeLogServices: 0,
  totalServices: 9,
  aiChatTotal: 0,
  aiAvgLatencyMs: 0,
  paymentCallbackTotal: 0,
  paymentCallbackSuccess: 0,
  errorLogCount: 0,
  services: []
})

// Runtime logs state
const runtimeLoading = ref(false)
const runtimeLogs = ref<RuntimeLogItem[]>([])
const runtimeTotalMatched = ref(0)
const autoRefresh = ref(false)
let refreshTimer: number | null = null

const runtimeQuery = reactive({
  service: 'ALL',
  level: 'ALL',
  traceId: '',
  keyword: '',
  errorOnly: false,
  limit: 100
})

// AI logs state
const aiLoading = ref(false)
const aiLogs = ref<AiLogItem[]>([])
const aiTotal = ref(0)
const aiModels = ref<string[]>([])
const aiQuery = reactive({
  keyword: '',
  model: 'ALL',
  page: 1,
  size: 15
})

// Payment callback logs state
const payLoading = ref(false)
const payLogs = ref<PayLogItem[]>([])
const payTotal = ref(0)
const payQuery = reactive({
  orderNo: '',
  channel: 'ALL',
  success: -1,
  page: 1,
  size: 15
})

// Detail dialogs
const runtimeDetailVisible = ref(false)
const currentRuntime = ref<RuntimeLogItem | null>(null)
const aiDetailVisible = ref(false)
const currentAi = ref<AiLogItem | null>(null)
const payDetailVisible = ref(false)
const currentPay = ref<PayLogItem | null>(null)

function isValidTraceId(traceId: string | null | undefined) {
  return !!traceId && !traceId.includes('%X')
}

function levelTagType(level: string) {
  if (level === 'ERROR') return 'danger'
  if (level === 'WARN') return 'warning'
  if (level === 'DEBUG') return 'info'
  return 'primary'
}

function serviceTagType(service: string) {
  if (service.includes('gateway')) return 'primary'
  if (service.includes('ai')) return 'success'
  if (service.includes('payment')) return 'warning'
  if (service.includes('admin')) return 'danger'
  return 'info'
}

function formatPayload(raw: string | null | undefined) {
  if (!raw) return '(空)'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

async function fetchSummary() {
  try {
    const { data } = await adminApi.get('/admin/logs/summary')
    if (data.code === 0 && data.data) {
      summary.value = data.data
    }
  } catch (err) {
    console.error('Failed to load log summary', err)
  }
}

async function fetchRuntimeLogs() {
  runtimeLoading.value = true
  try {
    const params: Record<string, any> = {
      service: runtimeQuery.service,
      level: runtimeQuery.level,
      errorOnly: runtimeQuery.errorOnly,
      limit: runtimeQuery.limit
    }
    if (runtimeQuery.traceId.trim()) params.traceId = runtimeQuery.traceId.trim()
    if (runtimeQuery.keyword.trim()) params.keyword = runtimeQuery.keyword.trim()

    const { data } = await adminApi.get('/admin/logs/runtime', { params })
    if (data.code === 0 && data.data) {
      runtimeLogs.value = data.data.items || []
      runtimeTotalMatched.value = data.data.totalMatched || 0
    } else {
      ElMessage.error(data.message || '加载微服务运行日志失败')
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '加载微服务运行日志失败')
  } finally {
    runtimeLoading.value = false
  }
}

async function refreshAllRuntime() {
  await Promise.all([fetchSummary(), fetchRuntimeLogs()])
}

function trackByTraceId(traceId: string) {
  runtimeQuery.service = 'ALL'
  runtimeQuery.level = 'ALL'
  runtimeQuery.errorOnly = false
  runtimeQuery.traceId = traceId
  fetchRuntimeLogs()
}

function clearTraceFilter() {
  runtimeQuery.traceId = ''
  fetchRuntimeLogs()
}

async function fetchAiLogs() {
  aiLoading.value = true
  try {
    const params: Record<string, any> = {
      page: aiQuery.page - 1,
      size: aiQuery.size,
      model: aiQuery.model
    }
    if (aiQuery.keyword.trim()) params.keyword = aiQuery.keyword.trim()

    const { data } = await adminApi.get('/admin/logs/ai', { params })
    if (data.code === 0 && data.data) {
      aiLogs.value = data.data.items || []
      aiTotal.value = data.data.total || 0
      aiModels.value = data.data.models || []
    } else {
      ElMessage.error(data.message || '加载 AI 问答日志失败')
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '加载 AI 问答日志失败')
  } finally {
    aiLoading.value = false
  }
}

function reloadAiLogs() {
  aiQuery.page = 1
  fetchAiLogs()
}

async function fetchPayLogs() {
  payLoading.value = true
  try {
    const params: Record<string, any> = {
      page: payQuery.page - 1,
      size: payQuery.size,
      channel: payQuery.channel
    }
    if (payQuery.orderNo.trim()) params.orderNo = payQuery.orderNo.trim()
    if (payQuery.success !== -1) params.success = payQuery.success

    const { data } = await adminApi.get('/admin/logs/payment-callbacks', { params })
    if (data.code === 0 && data.data) {
      payLogs.value = data.data.items || []
      payTotal.value = data.data.total || 0
    } else {
      ElMessage.error(data.message || '加载支付回调日志失败')
    }
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '加载支付回调日志失败')
  } finally {
    payLoading.value = false
  }
}

function reloadPayLogs() {
  payQuery.page = 1
  fetchPayLogs()
}

function handleTabChange(name: string | number) {
  if (name === 'runtime') {
    fetchRuntimeLogs()
  } else if (name === 'ai') {
    fetchAiLogs()
  } else if (name === 'payment') {
    fetchPayLogs()
  }
}

function openRuntimeDetail(row: RuntimeLogItem) {
  currentRuntime.value = row
  runtimeDetailVisible.value = true
}

function openAiDetail(row: AiLogItem) {
  currentAi.value = row
  aiDetailVisible.value = true
}

function openPayDetail(row: PayLogItem) {
  currentPay.value = row
  payDetailVisible.value = true
}

watch(autoRefresh, (enabled) => {
  if (refreshTimer !== null) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (enabled) {
    refreshTimer = window.setInterval(() => {
      if (activeTab.value === 'runtime') {
        refreshAllRuntime()
      }
    }, 5000)
  }
})

onMounted(() => {
  fetchSummary()
  fetchRuntimeLogs()
})

onUnmounted(() => {
  if (refreshTimer !== null) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped>
.admin-logs {
  animation: fadeIn 0.25s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stats-row {
  margin-bottom: 18px;
}

.stat-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 14px 16px;
  box-shadow: 0 4px 12px rgba(148, 163, 184, 0.25);
  border: 1px solid rgba(209, 213, 219, 0.9);
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 82px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card-1 .stat-icon {
  color: #1d4ed8;
  background: #e0ecff;
}

.stat-card-2 .stat-icon {
  color: #15803d;
  background: #dcfce7;
}

.stat-card-3 .stat-icon {
  color: #0369a1;
  background: #e0f2fe;
}

.stat-card-4 .stat-icon {
  color: #b91c1c;
  background: #fee2e2;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
}

.stat-unit {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
}

.logs-card {
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(148, 163, 184, 0.3);
  border: 1px solid rgba(209, 213, 219, 0.9);
  background: #ffffff;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.trace-banner {
  margin-bottom: 12px;
  border-radius: 10px;
}

.trace-chip {
  cursor: pointer;
  font-family: Consolas, 'Courier New', monospace;
  transition: all 0.15s;
}

.trace-chip:hover {
  background-color: #dbeafe;
  color: #1d4ed8;
  border-color: #93c5fd;
}

.mono-text {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
}

.muted-logger {
  color: #64748b;
}

.muted-text {
  color: #94a3b8;
  font-size: 12px;
}

.amount-text {
  font-weight: 600;
  color: #b45309;
  font-family: Consolas, 'Courier New', monospace;
}

.log-message-cell {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.log-message-cell:hover {
  color: #2563eb;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.detail-pre-title {
  margin: 14px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.detail-pre {
  margin: 0;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #0f172a;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;
}
</style>
