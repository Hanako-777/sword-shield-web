<template>
  <div class="payment-container">
    <header class="page-hero">
      <h1>大会购票与资料包</h1><p>选择门票或会议资料包，在线完成下单与支付</p>
    </header>
    <CheckoutSteps :step="cashierOpen ? (current?.status === 'PAID' ? 3 : 2) : selected ? 1 : 0" />
    <div class="checkout-layout">
      <section v-reveal class="reveal" aria-labelledby="ticket-heading">
        <h2 id="ticket-heading">选择票种</h2>
        <ReadState v-if="productsLoading || productsError || !products.length" :loading="productsLoading"
          :error="productsError" empty="暂无商品" @retry="loadProducts" />
        <fieldset v-else class="choices"><legend class="ss-sr-only">选择票种</legend>
          <label v-for="p in products" :key="p.id" class="ticket" :class="{ selected: selectedId === p.id, sold: p.stock != null && p.stock <= 0 }">
            <input type="radio" name="product" v-model="selectedId" :value="p.id" :disabled="(p.stock != null && p.stock <= 0) || placing" />
            <span class="ticket-info"><span class="ss-badge">{{ typeLabel(p.type) }}</span>
              <b>{{ p.name }}</b><span class="description">{{ p.description }}</span>
            </span>
            <span class="ticket-price"><b>¥{{ p.price }}</b><small>余量 {{ p.stock ?? '不限' }}</small></span>
            <span class="mark" aria-hidden="true"><Icon v-if="selectedId === p.id" name="check" :size="13" /></span>
          </label>
        </fieldset>
      </section>
      <aside class="summary">
        <h2>订单小结</h2>
        <dl><div><dt>所选票种</dt><dd>{{ selected?.name || '待选择' }}</dd></div>
          <div><dt>数量</dt><dd>1</dd></div>
          <div class="total"><dt>合计</dt><dd>{{ selected ? '¥' + selected.price : '—' }}</dd></div></dl>
        <el-button type="primary" :disabled="!selected" :loading="placing" @click="buySelected">立即购买</el-button>
        <router-link v-if="isLoggedIn" to="/orders" class="summary-orders-link">
          查看我的订单 →
        </router-link>
      </aside>
    </div>
    <el-alert v-if="!isLoggedIn" type="info" :closable="false" class="login-tip">
      登录后即可下单并在「账户 → 我的订单」中查看记录。演示账号：demo / 123456
    </el-alert>
    <el-dialog v-model="cashierOpen" title="收银台（模拟支付）" width="min(460px, calc(100% - 24px))" destroy-on-close>
      <div v-if="current" class="cashier">
        <CheckoutSteps :step="current.status === 'PAID' ? 3 : 2" />
        <div class="cashier-row"><span>订单号</span><b>{{ current.orderNo }}</b></div>
        <div class="cashier-row"><span>商品</span><b>{{ current.productName }}</b></div>
        <div class="cashier-row"><span>应付金额</span><b class="amount">¥{{ current.amount }}</b></div>
        <div class="cashier-row"><span>订单状态</span>
          <el-tag :type="statusType(current.status)" size="small">{{ statusLabel(current.status) }}</el-tag>
        </div>
        <div v-if="current.status === 'PAID'" class="success" role="status"><i aria-hidden="true"><Icon name="check" :size="24" /></i>已支付</div>
        <div v-else class="qr-box">
          <div class="qr">▣</div>
          <p>扫码支付（演示环境不产生真实扣款）</p>
        </div>
        <el-alert v-if="current.status !== 'PAID'" type="warning" :closable="false" show-icon
                  title="演示说明"
                  description="点击下方按钮模拟支付成功；「模拟回调」用于演示第三方异步通知与幂等处理。" />
      </div>
      <template #footer>
        <el-button v-if="current && current.status === 'CREATED'" @click="mockCallback">模拟第三方回调</el-button>
        <el-button v-if="current && current.status === 'CREATED'" type="primary" @click="confirmPay">模拟支付成功</el-button>
        <el-button v-if="current && current.status === 'PAID'" @click="goToOrders">查看我的订单</el-button>
        <el-button v-if="current && current.status === 'PAID'" type="primary" @click="cashierOpen = false">已完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

import ReadState from '../components/detail/ReadState.vue'
import { useReadState } from '../components/detail/useReadState'
import { vReveal } from '../directives/reveal'
import CheckoutSteps from '../components/detail/CheckoutSteps.vue'
import Icon from '../components/ui/Icon.vue'

interface Product { id: number; name: string; type: string; price: number; stock: number | null; description: string }
interface Order {
  orderNo: string; productId: number; productName: string; amount: number;
  status: string; channel: string; payUrl?: string; createdAt?: string; paidAt?: string
}

const router = useRouter()
const products = ref<Product[]>([])
const selectedId = ref<number>()
const selected = computed(() => products.value.find(product => product.id === selectedId.value))
const placing = ref(false)
async function buySelected() {
  if (!selected.value || placing.value) return
  placing.value = true
  try { await buy(selected.value) } finally { placing.value = false }
}
const { loading: productsLoading, error: productsError, run: readProducts } = useReadState()
const cashierOpen = ref(false)
const current = ref<Order | null>(null)
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

onMounted(() => {
  loadProducts()
})

function loadProducts() {
  return readProducts(async () => {
    const { data } = await api.get('/payment/products')
    if (data.code !== 0) throw new Error(data.message || '商品加载失败')
    products.value = data.data
  }, '商品加载失败')
}

async function buy(p: Product) {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录后再购买')
    router.push('/login')
    return
  }
  try {
    const { data } = await api.post('/payment/orders', { productId: p.id, channel: 'MOCK' })
    if (data.code === 0) {
      current.value = data.data
      await initiatePay()
      cashierOpen.value = true
    } else {
      ElMessage.error(data.message || '下单失败')
    }
  } catch {
    ElMessage.error('下单失败，请稍后再试')
  }
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

function goToOrders() {
  cashierOpen.value = false
  router.push('/orders')
}

async function confirmPay() {
  if (!current.value) return
  try {
    const { data } = await api.post(`/payment/orders/${current.value.orderNo}/confirm`)
    if (data.code === 0) {
      current.value = data.data
      ElMessage.success('支付成功，订单已更新为已支付')
      await loadProducts()
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
      await loadProducts()
    } else {
      ElMessage.error(data.message || '回调处理失败')
    }
  } catch {
    ElMessage.error('回调处理失败')
  }
}

function typeLabel(t: string) {
  return t === 'TICKET' ? '门票' : t === 'MATERIAL' ? '资料包' : '周边'
}
function statusLabel(s: string) {
  return s === 'PAID' ? '已支付' : s === 'CREATED' ? '待支付' : s === 'CLOSED' ? '已关闭' : s
}
function statusType(s: string): 'success' | 'warning' | 'info' {
  return s === 'PAID' ? 'success' : s === 'CREATED' ? 'warning' : 'info'
}
</script>

<style scoped src="../components/detail/detail.css"></style>
<style scoped>
.payment-container { width: min(100%, 1040px); margin: 0 auto; padding: 24px 0 64px; font-family: var(--ss-font); color: var(--ss-ink); }
.page-hero h1 { font-size: clamp(30px,4vw,44px); letter-spacing: -.02em; font-weight: 600; color: var(--ss-navy); }
.page-hero p, .description, .ticket-price small, dt, .order-copy small, .order-action, .cashier-row > span, .qr-box p { color: var(--ss-muted); }
.page-hero p { margin: 8px 0 26px; }
.checkout-layout { margin-top: 32px; display: grid; grid-template-columns: minmax(0,1.55fr) minmax(0,1fr); align-items: start; gap: 48px; }
h2 { color: var(--ss-navy); font-size: 22px; margin-bottom: 16px; }
.choices { min-width: 0; border: 0; padding: 0; display: grid; gap: 12px; }
.ticket { display: grid; position: relative; grid-template-columns: minmax(0,1fr) auto 22px; align-items: center; gap: 16px;
  padding: 22px; cursor: pointer; border: 1px solid var(--ss-line-strong); border-radius: 20px; background: #fff; }
.ticket.selected { border-color: var(--ss-navy); box-shadow: inset 0 0 0 1px var(--ss-navy); }
.ticket:has(input:focus-visible) { outline: 2px solid rgba(21, 21, 21, .55); outline-offset: 2px; }
.ticket.sold { background: var(--ss-bg-soft); cursor: default; }
.ticket input { position: absolute; width: 1px; height: 1px; clip-path: inset(50%); }
.ticket-info, .ticket-price { display: grid; justify-items: start; gap: 6px; }
.ticket-info > b { font-size: 18px; color: var(--ss-navy); }
.description { font-size: 14px; line-height: 1.65; overflow-wrap: anywhere; }
.ticket-price { justify-items: end; font-variant-numeric: tabular-nums; }
.ticket-price b { font-size: 22px; }
.mark { display: grid; place-items: center; border-radius: 50%; width: 22px; height: 22px; border: 1px solid var(--ss-line-strong); }
.selected .mark { background: var(--ss-teal-ink); color: #fff; }
.summary { position: sticky; top: 96px; border-radius: 24px; padding: 26px; background: var(--ss-bg-soft); border: 1px solid var(--ss-line); }
.summary dl { margin: 16px 0; }
.summary dl > div, .cashier-row { display: flex; align-items: baseline; justify-content: space-between; gap: 16px; padding: 9px 0; }
dd { margin: 0; text-align: right; font-weight: 600; }
.summary .total { margin-top: 12px; border-top: 1px solid var(--ss-line-strong); align-items: center; }
.total dd, .amount { font-size: 26px; color: var(--ss-navy); }
.summary .el-button { width: 100%; min-height: 46px; }
.summary-orders-link { display: block; margin-top: 14px; text-align: center; font-size: 13px; color: var(--ss-muted); transition: color var(--ss-dur) var(--ss-ease); }
.summary-orders-link:hover { color: var(--ss-navy); }
.login-tip { margin-top: 48px; }
.cashier-row { border-bottom: 1px solid var(--ss-line); }
.cashier-row b { overflow-wrap: anywhere; text-align: right; }
.qr-box, .success { text-align: center; padding: 20px 0; }
.qr { width: 112px; height: 112px; margin: 0 auto 12px; font-size: 80px; line-height: 112px; color: var(--ss-navy); background: repeating-conic-gradient(var(--ss-navy) 0 25%, #fff 0 50%) 0 0/16px 16px; border: 5px solid white; outline: 1px solid var(--ss-line-strong); }
.success { display: grid; justify-items: center; gap: 8px; font-weight: 600; }
.success i { display: grid; place-items: center; width: 48px; height: 48px; border-radius: 50%; background: var(--ss-teal-soft); color: var(--ss-teal-ink); font-style: normal; font-size: 26px; }
@media(prefers-reduced-motion: no-preference) { .success i { animation: checked .32s var(--ss-ease) both; } }
@keyframes checked { from { opacity: 0; transform: scale(.75); } to { opacity: 1; transform: scale(1); } }
@media(max-width:1200px) { .checkout-layout { gap: 32px; } }
@media(max-width:960px) { .checkout-layout { gap: 20px; grid-template-columns: minmax(0,1.3fr) minmax(260px,1fr); } }
@media(max-width:640px) {
  .payment-container { padding: 12px 4px 40px; }
  .checkout-layout { grid-template-columns: minmax(0,1fr); }
  .summary { position: static; padding: 20px; }
  .ticket { gap: 10px; padding: 16px; }
}
</style>
