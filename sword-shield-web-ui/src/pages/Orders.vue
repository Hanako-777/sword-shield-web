<template>
  <div class="orders-container">
    <header class="page-hero">
      <div>
        <h1>我的订单</h1>
        <p>查看大会门票与资料包订单状态，待支付订单可在此继续完成支付</p>
      </div>
      <div class="hero-actions">
        <el-button :disabled="ordersLoading" @click="loadOrders">刷新</el-button>
        <el-button type="primary" @click="router.push('/payment')">前往购票</el-button>
      </div>
    </header>

    <section class="orders-section">
      <ReadState
        v-if="ordersLoading || ordersError || !orders.length"
        :loading="ordersLoading"
        :error="ordersError"
        empty="还没有订单，去选一张大会门票或资料包吧"
        @retry="loadOrders"
      />
      <ul v-else class="order-list">
        <li v-for="row in orders" :key="row.orderNo">
          <div class="order-copy">
            <strong><span class="ss-sr-only">商品</span>{{ row.productName }}</strong>
            <small>订单号 {{ row.orderNo }}</small>
            <small>
              下单时间 {{ formatTime(row.createdAt) }}
              <template v-if="row.paidAt"> · 支付时间 {{ formatTime(row.paidAt) }}</template>
            </small>
          </div>
          <strong class="order-price"><span class="ss-sr-only">金额</span>¥{{ row.amount }}</strong>
          <span class="ss-badge" :class="{ 'ss-badge--live': row.status === 'PAID' }">
            <span class="ss-sr-only">状态</span>{{ statusLabel(row.status) }}
          </span>
          <div class="order-action">
            <span class="ss-sr-only">操作</span>
            <el-button v-if="row.status === 'CREATED'" type="primary" @click="openCashier(row)">去支付</el-button>
            <span v-else>{{ row.status === 'PAID' ? '已完成' : statusLabel(row.status) }}</span>
          </div>
        </li>
      </ul>
    </section>

    <el-dialog v-model="cashierOpen" title="收银台（模拟支付）" width="min(460px, calc(100% - 24px))" destroy-on-close>
      <div v-if="current" class="cashier">
        <CheckoutSteps :step="current.status === 'PAID' ? 3 : 2" />
        <div class="cashier-row"><span>订单号</span><b>{{ current.orderNo }}</b></div>
        <div class="cashier-row"><span>商品</span><b>{{ current.productName }}</b></div>
        <div class="cashier-row"><span>应付金额</span><b class="amount">¥{{ current.amount }}</b></div>
        <div class="cashier-row">
          <span>订单状态</span>
          <el-tag :type="statusType(current.status)" size="small">{{ statusLabel(current.status) }}</el-tag>
        </div>
        <div v-if="current.status === 'PAID'" class="success" role="status">
          <i aria-hidden="true"><Icon name="check" :size="24" /></i>已支付
        </div>
        <div v-else class="qr-box">
          <div class="qr">▣</div>
          <p>扫码支付（演示环境不产生真实扣款）</p>
        </div>
        <el-alert
          v-if="current.status !== 'PAID'"
          type="warning"
          :closable="false"
          show-icon
          title="演示说明"
          description="点击下方按钮模拟支付成功；「模拟回调」用于演示第三方异步通知与幂等处理。"
        />
      </div>
      <template #footer>
        <el-button v-if="current && current.status === 'CREATED'" @click="mockCallback">模拟第三方回调</el-button>
        <el-button v-if="current && current.status === 'CREATED'" type="primary" @click="confirmPay">模拟支付成功</el-button>
        <el-button v-if="current && current.status === 'PAID'" type="primary" @click="cashierOpen = false">已完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 我的订单页（账户栏目）。
 *
 * - 展示当前登录用户的购票与资料包订单列表。
 * - 支持对未支付（CREATED）订单唤起模拟收银台完成支付或触发第三方回调。
 *
 * 依赖接口：
 * - GET `/payment/orders`
 * - POST `/payment/orders/:orderNo/pay`
 * - POST `/payment/orders/:orderNo/confirm`
 * - POST `/payment/callback/mock`
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import ReadState from '../components/detail/ReadState.vue'
import { useReadState } from '../components/detail/useReadState'
import CheckoutSteps from '../components/detail/CheckoutSteps.vue'
import Icon from '../components/ui/Icon.vue'

interface Order {
  orderNo: string
  productId: number
  productName: string
  amount: number
  status: string
  channel: string
  payUrl?: string
  createdAt?: string
  paidAt?: string
}

const router = useRouter()
const orders = ref<Order[]>([])
const { loading: ordersLoading, error: ordersError, run: readOrders } = useReadState()
const cashierOpen = ref(false)
const current = ref<Order | null>(null)

onMounted(() => {
  loadOrders()
})

function loadOrders() {
  return readOrders(async () => {
    const { data } = await api.get('/payment/orders', { params: { page: 0, size: 50 } })
    if (data.code !== 0) throw new Error(data.message || '订单加载失败')
    orders.value = data.data?.items || []
  }, '订单加载失败')
}

async function initiatePay() {
  if (!current.value) return
  try {
    const { data } = await api.post(`/payment/orders/${current.value.orderNo}/pay`)
    if (data.code === 0) current.value = { ...current.value, ...data.data }
  } catch {
    // 收银台仍可展示，支付动作单独报错
  }
}

function openCashier(row: Order) {
  current.value = row
  initiatePay()
  cashierOpen.value = true
}

async function confirmPay() {
  if (!current.value) return
  try {
    const { data } = await api.post(`/payment/orders/${current.value.orderNo}/confirm`)
    if (data.code === 0) {
      current.value = data.data
      ElMessage.success('支付成功，订单已更新为已支付')
      await loadOrders()
    } else {
      ElMessage.error(data.message || '支付失败')
    }
  } catch {
    ElMessage.error('支付失败，请稍后再试')
  }
}

async function mockCallback() {
  if (!current.value) return
  try {
    const { data } = await api.post('/payment/callback/mock', {
      orderNo: current.value.orderNo,
      tradeNo: 'MOCK-TRADE-' + Date.now(),
      amount: current.value.amount
    })
    if (data.code === 0) {
      ElMessage.success('第三方回调已处理（重复回调会自动幂等）')
      const detail = await api.get(`/payment/orders/${current.value.orderNo}`)
      if (detail.data.code === 0) current.value = detail.data.data
      await loadOrders()
    } else {
      ElMessage.error(data.message || '回调处理失败')
    }
  } catch {
    ElMessage.error('回调处理失败')
  }
}

function statusLabel(s: string) {
  return s === 'PAID' ? '已支付' : s === 'CREATED' ? '待支付' : s === 'CLOSED' ? '已关闭' : s
}

function statusType(s: string): 'success' | 'warning' | 'info' {
  return s === 'PAID' ? 'success' : s === 'CREATED' ? 'warning' : 'info'
}

function formatTime(t?: string) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}
</script>

<style scoped src="../components/detail/detail.css"></style>
<style scoped>
.orders-container {
  width: min(100%, 960px);
  margin: 0 auto;
  padding: 24px 0 64px;
  font-family: var(--ss-font);
  color: var(--ss-ink);
}

.page-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--ss-line);
}

.page-hero h1 {
  font-size: clamp(28px, 3.6vw, 38px);
  letter-spacing: -0.02em;
  font-weight: 600;
  color: var(--ss-navy);
}

.page-hero p {
  margin-top: 8px;
  color: var(--ss-muted);
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.orders-section {
  margin-top: 8px;
}

.order-list {
  list-style: none;
}

.order-list li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto 96px;
  align-items: center;
  gap: 22px;
  padding: 20px 0;
  border-bottom: 1px solid var(--ss-line);
}

.order-copy {
  display: grid;
  gap: 4px;
  overflow-wrap: anywhere;
}

.order-copy small {
  color: var(--ss-muted);
  font-size: 13px;
}

.order-price {
  font-variant-numeric: tabular-nums;
  color: var(--ss-navy);
}

.order-action {
  text-align: end;
  font-size: 13px;
  color: var(--ss-muted);
}

.cashier-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 9px 0;
  border-bottom: 1px solid var(--ss-line);
}

.cashier-row > span,
.qr-box p {
  color: var(--ss-muted);
}

.cashier-row b {
  overflow-wrap: anywhere;
  text-align: right;
}

.amount {
  font-size: 26px;
  color: var(--ss-navy);
}

.qr-box,
.success {
  text-align: center;
  padding: 20px 0;
}

.qr {
  width: 112px;
  height: 112px;
  margin: 0 auto 12px;
  font-size: 80px;
  line-height: 112px;
  color: var(--ss-navy);
  background: repeating-conic-gradient(var(--ss-navy) 0 25%, #fff 0 50%) 0 0 / 16px 16px;
  border: 5px solid white;
  outline: 1px solid var(--ss-line-strong);
}

.success {
  display: grid;
  justify-items: center;
  gap: 8px;
  font-weight: 600;
}

.success i {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--ss-teal-soft);
  color: var(--ss-teal-ink);
  font-style: normal;
  font-size: 26px;
}

@media (max-width: 640px) {
  .orders-container {
    padding: 12px 4px 40px;
  }

  .order-list li {
    grid-template-columns: 1fr auto;
    gap: 10px;
  }

  .order-list .ss-badge {
    justify-self: start;
  }
}
</style>
