<template>
  <div class="payment-container">
    <div class="page-hero">
      <h1>大会购票与资料包</h1>
      <p>在线下单 → 模拟收银台 → 支付回调，完整演示订单状态流转与幂等处理</p>
    </div>

    <el-row :gutter="20" class="product-grid">
      <el-col v-for="p in products" :key="p.id" :xs="24" :sm="12" :md="8">
        <el-card class="product-card" shadow="hover">
          <div class="product-type">{{ typeLabel(p.type) }}</div>
          <h3 class="product-name">{{ p.name }}</h3>
          <p class="product-desc">{{ p.description }}</p>
          <div class="product-footer">
            <div class="price">¥<span>{{ p.price }}</span></div>
            <div class="stock">余量 {{ p.stock }}</div>
          </div>
          <el-button type="primary" class="buy-btn" @click="buy(p)">立即购买</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="isLoggedIn" class="orders-card" shadow="never">
      <div class="orders-head">
        <h2>我的订单</h2>
        <el-button text @click="loadOrders">刷新</el-button>
      </div>
      <el-table :data="orders" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="200" />
        <el-table-column prop="productName" label="商品" min-width="160" />
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'CREATED'" size="small" type="primary" @click="openCashier(row)">
              去支付
            </el-button>
            <span v-else class="paid-text">已完成</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!orders.length" class="empty">还没有订单，先选一个商品试试吧</div>
    </el-card>

    <el-alert v-else type="info" :closable="false" class="login-tip">
      登录后即可下单并查看「我的订单」。演示账号：demo / 123456
    </el-alert>

    <el-dialog v-model="cashierOpen" title="收银台（模拟支付）" width="460px" destroy-on-close>
      <div v-if="current" class="cashier">
        <div class="cashier-row"><span>订单号</span><b>{{ current.orderNo }}</b></div>
        <div class="cashier-row"><span>商品</span><b>{{ current.productName }}</b></div>
        <div class="cashier-row"><span>应付金额</span><b class="amount">¥{{ current.amount }}</b></div>
        <div class="cashier-row"><span>订单状态</span>
          <el-tag :type="statusType(current.status)" size="small">{{ statusLabel(current.status) }}</el-tag>
        </div>
        <div class="qr-box">
          <div class="qr">▣</div>
          <p>扫码支付（演示环境不产生真实扣款）</p>
        </div>
        <el-alert type="warning" :closable="false" show-icon
                  title="演示说明"
                  description="点击下方按钮模拟支付成功；「模拟回调」用于演示第三方异步通知与幂等处理。" />
      </div>
      <template #footer>
        <el-button v-if="current && current.status === 'CREATED'" @click="mockCallback">模拟第三方回调</el-button>
        <el-button v-if="current && current.status === 'CREATED'" type="primary" @click="confirmPay">模拟支付成功</el-button>
        <el-button v-if="current && current.status === 'PAID'" type="success" @click="cashierOpen = false">已完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

interface Product { id: number; name: string; type: string; price: number; stock: number; description: string }
interface Order {
  orderNo: string; productId: number; productName: string; amount: number;
  status: string; channel: string; payUrl?: string; createdAt?: string; paidAt?: string
}

const router = useRouter()
const products = ref<Product[]>([])
const orders = ref<Order[]>([])
const cashierOpen = ref(false)
const current = ref<Order | null>(null)
const isLoggedIn = computed(() => !!localStorage.getItem('token'))

onMounted(async () => {
  await loadProducts()
  if (isLoggedIn.value) await loadOrders()
})

async function loadProducts() {
  try {
    const { data } = await api.get('/payment/products')
    if (data.code === 0) products.value = data.data
  } catch {
    ElMessage.error('商品加载失败')
  }
}

async function loadOrders() {
  try {
    const { data } = await api.get('/payment/orders', { params: { page: 0, size: 20 } })
    if (data.code === 0) orders.value = data.data.items
  } catch {
    // 未登录或服务不可用时静默
  }
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
      await loadOrders()
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

function typeLabel(t: string) {
  return t === 'TICKET' ? '门票' : t === 'MATERIAL' ? '资料包' : '周边'
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

<style scoped>
.payment-container { max-width: 1200px; margin: 0 auto; }
.page-hero {
  color: #fff; text-align: center; padding: 32px 16px 40px;
}
.page-hero h1 { font-size: 34px; margin-bottom: 10px; text-shadow: 0 2px 12px rgba(0,0,0,.25); }
.page-hero p { opacity: .92; }
.product-grid { margin-bottom: 24px; }
.product-card { border-radius: 14px; margin-bottom: 20px; height: 100%; }
.product-type {
  display: inline-block; font-size: 12px; color: #ff0050; background: #fff0f5;
  padding: 3px 10px; border-radius: 10px; margin-bottom: 10px;
}
.product-name { font-size: 18px; margin-bottom: 10px; color: #222; }
.product-desc { font-size: 13px; color: #777; line-height: 1.6; min-height: 62px; }
.product-footer { display: flex; align-items: baseline; justify-content: space-between; margin: 12px 0; }
.price { color: #ff0050; font-size: 14px; }
.price span { font-size: 28px; font-weight: 700; }
.stock { font-size: 12px; color: #999; }
.buy-btn { width: 100%; }
.orders-card { border-radius: 14px; background: rgba(255,255,255,.97); }
.orders-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.orders-head h2 { font-size: 20px; color: #222; }
.paid-text { color: #67c23a; font-size: 13px; }
.empty { text-align: center; color: #999; padding: 24px; }
.login-tip { max-width: 560px; margin: 0 auto; border-radius: 12px; }
.cashier-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px dashed #eee; font-size: 14px; }
.cashier-row span { color: #888; }
.cashier-row .amount { color: #ff0050; font-size: 20px; }
.qr-box { text-align: center; padding: 18px 0; }
.qr {
  width: 130px; height: 130px; margin: 0 auto 10px; font-size: 90px; line-height: 130px;
  color: #111; background: repeating-conic-gradient(#111 0 25%, #fff 0 50%) 0 0/16px 16px;
  border: 6px solid #fff; outline: 2px solid #eee;
}
.qr-box p { font-size: 12px; color: #999; }
</style>
