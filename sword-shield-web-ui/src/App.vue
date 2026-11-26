<template>
  <div class="app-shell">
    <template v-if="!isAdminRoute">
      <a class="skip-link" href="#main-content">跳到主要内容</a>

      <header
        v-if="!isAuthRoute"
        ref="headerRef"
        class="app-header"
        :class="{ 'is-hidden': isHomeRoute && !navVisible, 'is-expanded': Boolean(activePanelGroup) }"
        :aria-hidden="(isHomeRoute && !navVisible) || undefined"
        :inert="isHomeRoute && !navVisible"
        @pointerenter="onHeaderPointerEnter"
        @pointerleave="onHeaderPointerLeave"
      >
        <div class="nav-inner">
          <router-link to="/" class="brand" aria-label="西湖论剑 首页">
            <span class="brand-cn">西湖论剑</span>
            <span class="brand-sub">数字安全大会</span>
          </router-link>

          <!-- 桌面端顶层：首页 / 大会 ▾ / 服务 ▾ -->
          <nav class="nav-menu" aria-label="主导航">
            <router-link
              to="/"
              class="nav-link"
              :class="{ 'is-active': route.path === '/' }"
              @pointerenter="scheduleClosePanel"
            >首页</router-link>

            <button
              v-for="group in primaryNavGroups"
              :key="group.id"
              :ref="(el) => setTriggerRef(group.id, el)"
              type="button"
              class="nav-link"
              :class="{
                'is-active': isNavGroupActive(group, route.path),
                'is-open': activePanelId === group.id
              }"
              :aria-expanded="activePanelId === group.id"
              :aria-controls="`nav-panel-${group.id}`"
              @pointerdown="onGroupPointerDown"
              @pointerenter="(e) => onGroupPointerEnter(group.id, e)"
              @click="(e) => onGroupClick(group.id, e)"
              @keydown="(e) => onGroupKeydown(group.id, e)"
            >
              <span>{{ group.label }}</span>
              <Icon name="chevron-down" :size="14" class="chev" :class="{ 'is-open': activePanelId === group.id }" />
            </button>
          </nav>

          <div class="nav-actions">
            <router-link to="/payment" class="nav-cta" :class="{ 'is-active': route.path === '/payment' }">购票</router-link>

            <template v-if="isLoggedIn">
              <div
                ref="accountWrapRef"
                class="nav-account-wrap"
                @pointerenter="onAccountPointerEnter"
                @pointerleave="onAccountPointerLeave"
              >
                <button
                  ref="accountTriggerRef"
                  type="button"
                  class="nav-user"
                  :class="{ 'is-active': isNavGroupActive(accountNavGroup, route.path) }"
                  :aria-label="`账户菜单：${username || '用户'}`"
                  :aria-expanded="accountOpen"
                  aria-controls="nav-account-panel"
                  @pointerdown="onAccountPointerDown"
                  @click="onAccountClick"
                  @keydown="onAccountKeydown"
                >
                  <el-avatar :size="28" :src="userAvatar || undefined" class="nav-avatar">
                    {{ username?.charAt(0) || 'U' }}
                  </el-avatar>
                  <span class="username">{{ username }}</span>
                  <Icon name="chevron-down" :size="14" class="chev" :class="{ 'is-open': accountOpen }" />
                </button>

                <transition name="account-pop">
                  <div
                    v-if="accountOpen"
                    id="nav-account-panel"
                    ref="accountPanelRef"
                    class="nav-account-pop"
                    role="menu"
                    aria-label="账户菜单"
                  >
                    <div class="account-group-label">{{ accountNavGroup.label }}</div>
                    <router-link
                      v-for="item in accountNavGroup.items"
                      :key="item.to"
                      :to="item.to"
                      class="account-link"
                      :class="{ 'is-active': isNavItemActive(item, route.path) }"
                      role="menuitem"
                      @click="closeAllPanels"
                    >
                      <Icon :name="item.icon" :size="18" class="account-link-icon" />
                      <span>{{ item.label }}</span>
                    </router-link>
                    <div class="account-divider" role="separator" />
                    <button type="button" class="account-link account-logout" role="menuitem" @click="logout">
                      <Icon name="log-out" :size="18" class="account-link-icon" />
                      <span>退出</span>
                    </button>
                  </div>
                </transition>
              </div>
            </template>
            <template v-else>
              <router-link to="/login" class="nav-text">登录</router-link>
              <router-link to="/register" class="nav-ghost">注册</router-link>
            </template>

            <button
              type="button"
              class="nav-burger"
              aria-label="打开菜单"
              aria-controls="nav-drawer"
              :aria-expanded="drawerOpen"
              @click="drawerOpen = true"
            >
              <Icon name="menu" :size="20" />
            </button>
          </div>
        </div>

        <!-- Partiful 式向下展开面板（与导航条合为一块 1040px / 20px 圆角面板） -->
        <div
          class="nav-panel-shell"
          :class="{ 'is-open': Boolean(activePanelGroup) }"
          :aria-hidden="!activePanelGroup || undefined"
          :inert="!activePanelGroup"
        >
          <div class="nav-panel-clip">
            <div
              v-if="renderedPanelGroup"
              :id="`nav-panel-${renderedPanelGroup.id}`"
              ref="panelBodyRef"
              class="nav-panel-body"
              :class="{ 'is-visible': Boolean(activePanelGroup) }"
            >
              <div class="panel-links-col">
                <div class="panel-group-label">{{ renderedPanelGroup.label }}</div>
                <ul class="panel-links">
                  <li v-for="item in renderedPanelGroup.items" :key="item.to">
                    <router-link
                      :to="item.to"
                      class="panel-link"
                      :class="{
                        'is-active': isNavItemActive(item, route.path),
                        'has-desc': Boolean(item.description)
                      }"
                      @click="closeAllPanels"
                    >
                      <span class="panel-link-head">
                        <Icon :name="item.icon" :size="20" class="panel-link-icon" />
                        <span class="panel-link-title">{{ item.label }}</span>
                      </span>
                      <span v-if="item.description" class="panel-link-desc">{{ item.description }}</span>
                    </router-link>
                  </li>
                </ul>
              </div>

              <router-link
                :to="renderedPanelGroup.featured.to"
                class="panel-featured"
                @click="closeAllPanels"
              >
                <div class="featured-cover-wrap">
                  <img :src="renderedPanelGroup.featured.image" :alt="renderedPanelGroup.featured.title" class="featured-cover" />
                </div>
                <div class="featured-meta">
                  <span class="featured-tag">{{ renderedPanelGroup.featured.tag }}</span>
                  <strong class="featured-title">{{ renderedPanelGroup.featured.title }}</strong>
                  <p class="featured-desc">{{ renderedPanelGroup.featured.description }}</p>
                </div>
              </router-link>
            </div>
          </div>
        </div>
      </header>

      <!-- 移动端抽屉：首页 + 大会 / 服务 / 账户（统一读 siteNav，无推荐卡） -->
      <transition name="drawer">
        <div v-if="drawerOpen" id="nav-drawer" class="nav-drawer" role="dialog" aria-modal="true" aria-label="网站导航">
          <div class="drawer-head">
            <router-link to="/" class="brand">
              <span class="brand-cn">西湖论剑</span>
              <span class="brand-sub">数字安全大会</span>
            </router-link>
            <button ref="drawerClose" type="button" class="nav-burger is-close" aria-label="关闭菜单" @click="drawerOpen = false">
              <Icon name="x" :size="20" />
            </button>
          </div>
          <div class="drawer-body">
            <router-link to="/payment" class="drawer-cta">购票</router-link>

            <div class="drawer-home">
              <router-link to="/" class="drawer-link" :class="{ 'is-active': route.path === '/' }">
                <Icon name="home" :size="20" class="drawer-link-icon" />
                <span>首页</span>
              </router-link>
            </div>

            <section v-for="group in primaryNavGroups" :key="group.id" class="drawer-group">
              <h2 class="drawer-title">{{ group.label }}</h2>
              <router-link
                v-for="item in group.items"
                :key="item.to"
                :to="item.to"
                class="drawer-link"
                :class="{ 'is-active': isNavItemActive(item, route.path) }"
              >
                <Icon :name="item.icon" :size="20" class="drawer-link-icon" />
                <span>{{ item.label }}</span>
              </router-link>
            </section>

            <section class="drawer-group">
              <h2 class="drawer-title">{{ accountNavGroup.label }}</h2>
              <template v-if="isLoggedIn">
                <router-link
                  v-for="item in accountNavGroup.items"
                  :key="item.to"
                  :to="item.to"
                  class="drawer-link"
                  :class="{ 'is-active': isNavItemActive(item, route.path) }"
                >
                  <Icon :name="item.icon" :size="20" class="drawer-link-icon" />
                  <span>{{ item.label }}</span>
                </router-link>
                <button type="button" class="drawer-link drawer-logout" @click="logout">
                  <Icon name="log-out" :size="20" class="drawer-link-icon" />
                  <span>退出</span>
                </button>
              </template>
              <template v-else>
                <router-link to="/login" class="drawer-link">
                  <Icon name="user" :size="20" class="drawer-link-icon" />
                  <span>登录</span>
                </router-link>
                <router-link to="/register" class="drawer-link">
                  <Icon name="plus" :size="20" class="drawer-link-icon" />
                  <span>注册</span>
                </router-link>
                <router-link to="/admin/login" class="drawer-link">
                  <Icon name="monitor" :size="20" class="drawer-link-icon" />
                  <span>管理后台</span>
                </router-link>
              </template>
            </section>
          </div>
        </div>
      </transition>

      <main id="main-content" class="app-main" :class="{ 'auth-main': isDisplayedAuthRoute, 'home-main': isDisplayedHomeRoute }">
        <router-view v-slot="{ Component, route: viewRoute }">
          <transition
            :name="routeTransitionName"
            mode="out-in"
            @before-enter="onRouteBeforeEnter"
            @enter="onRouteEnter"
            @after-enter="onRouteAfterEnter"
          >
            <div v-if="Component" :key="viewRoute.path" class="ss-route-view">
              <component :is="Component" />
            </div>
          </transition>
        </router-view>
      </main>

      <footer v-if="!isAuthRoute" class="app-footer">
        <div class="footer-inner">
          <div class="footer-brand">
            <div class="footer-name">西湖论剑 · 数字安全大会</div>
            <p class="footer-desc">为会议活动提供一站式数字安全支撑与智能运营能力。</p>
          </div>
          <nav class="footer-cols" aria-label="页脚导航">
            <div v-for="group in siteNavGroups" :key="group.id" class="footer-col">
              <h2 class="footer-title">{{ group.label }}</h2>
              <router-link v-for="item in group.items" :key="item.to" :to="item.to" class="footer-link">{{ item.label }}</router-link>
            </div>
          </nav>
        </div>
        <div class="footer-bottom">
          <span>© 2026 西湖论剑</span>
          <router-link to="/admin/login" class="footer-link footer-admin">管理后台</router-link>
        </div>
      </footer>
      <AiAssistant v-if="!isAuthRoute" />
    </template>
    <template v-else>
      <!-- Admin routes: render only admin layout from admin components -->
      <router-view />
    </template>
  </div>
</template>

<script setup lang="ts">
/**
 * 根组件（App Shell）。
 *
 * - 用户端：悬浮圆角导航条 + Partiful 式一体展开下拉面板（首页首屏上半部分隐藏，滚过首屏 50% 后滑入；其余路由始终固定显示）。
 * - 导航、页尾与移动端抽屉统一从 `src/config/siteNav.ts` 读取单一数据源。
 * - 管理端：当路由以 `/admin` 开头时，交由后台布局组件渲染（本组件仅透传 `<router-view />`）。
 */
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick, type ComponentPublicInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AiAssistant from './components/AiAssistant.vue'
import Icon from './components/ui/Icon.vue'
import {
  siteNavGroups,
  primaryNavGroups,
  accountNavGroup,
  isNavGroupActive,
  isNavItemActive,
  type SiteNavGroupId
} from './config/siteNav'

const OPEN_DELAY = 120
const CLOSE_DELAY = 200

function normalizeRoutePath(path: string): string {
  if (!path || path === '/') return '/'
  return path.replace(/\/+$/, '') || '/'
}

/**
 * 页面切换方向索引表：按 siteNav 展开顺序（首页 → 大会各项 → 服务各项 → 账户各项）。
 * 至少有一方不在表内时仅做淡入淡出。
 */
const routeOrderIndex = new Map<string, number>(
  ['/', ...siteNavGroups.flatMap((group) => group.items.map((item) => item.to))].map((path, index) => [
    normalizeRoutePath(path),
    index
  ])
)

const router = useRouter()
const route = useRoute()
const displayedRoutePath = ref(route.path)
const routeTransitionName = ref<'route-slide-left' | 'route-slide-right' | 'route-fade'>('route-fade')

const removeRouteTransitionGuard = router.beforeResolve((to, from) => {
  if (!from.matched.length) {
    routeTransitionName.value = 'route-fade'
    return
  }
  const fromPath = normalizeRoutePath(from.path)
  const toPath = normalizeRoutePath(to.path)
  if (fromPath === toPath) return
  const fromIdx = routeOrderIndex.get(fromPath)
  const toIdx = routeOrderIndex.get(toPath)
  if (fromIdx === undefined || toIdx === undefined) {
    routeTransitionName.value = 'route-fade'
  } else if (toIdx > fromIdx) {
    routeTransitionName.value = 'route-slide-left'
  } else {
    routeTransitionName.value = 'route-slide-right'
  }
})

let isPopstateNav = false
function onPopstate() {
  isPopstateNav = true
}

function onRouteBeforeEnter() {
  displayedRoutePath.value = route.path
  if (!isPopstateNav && !route.hash) {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  }
}

function onRouteEnter() {
  updateNavVisibility()
  window.requestAnimationFrame(updateNavVisibility)
}

function onRouteAfterEnter() {
  isPopstateNav = false
  displayedRoutePath.value = route.path
  updateNavVisibility()
  window.requestAnimationFrame(updateNavVisibility)
  window.dispatchEvent(new Event('ss-route-entered'))
}

const username = ref(localStorage.getItem('username') || '')
const userAvatar = ref(localStorage.getItem('userAvatar') || '')
const token = ref(localStorage.getItem('token') || '')

const isLoggedIn = computed(() => !!token.value)
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isAuthRoute = computed(() => route.path === '/login' || route.path === '/register')
const isHomeRoute = computed(() => route.path === '/')
const isDisplayedAuthRoute = computed(() => displayedRoutePath.value === '/login' || displayedRoutePath.value === '/register')
const isDisplayedHomeRoute = computed(() => displayedRoutePath.value === '/')

const headerRef = ref<HTMLElement | null>(null)
const panelBodyRef = ref<HTMLElement | null>(null)
const accountWrapRef = ref<HTMLElement | null>(null)
const accountTriggerRef = ref<HTMLButtonElement | null>(null)
const accountPanelRef = ref<HTMLElement | null>(null)
const triggerRefs = new Map<SiteNavGroupId, HTMLButtonElement>()

const drawerOpen = ref(false)
const drawerClose = ref<HTMLButtonElement | null>(null)
const navVisible = ref(route.path !== '/')

const activePanelId = ref<SiteNavGroupId | null>(null)
const lastPanelId = ref<SiteNavGroupId>('conference')
const accountOpen = ref(false)

const activePanelGroup = computed(() =>
  activePanelId.value ? primaryNavGroups.find((g) => g.id === activePanelId.value) ?? null : null
)
const renderedPanelGroup = computed(() =>
  activePanelGroup.value ?? primaryNavGroups.find((g) => g.id === lastPanelId.value) ?? primaryNavGroups[0]
)

let openTimer: number | undefined
let closeTimer: number | undefined
let accountOpenTimer: number | undefined
let accountCloseTimer: number | undefined
let lastGroupPointerType = ''
let lastAccountPointerType = ''
let scrollRaf = 0
let scrollPending = false

function setTriggerRef(id: SiteNavGroupId, el: Element | ComponentPublicInstance | null) {
  if (el instanceof HTMLButtonElement) triggerRefs.set(id, el)
  else triggerRefs.delete(id)
}

function clearPanelTimers() {
  window.clearTimeout(openTimer)
  window.clearTimeout(closeTimer)
}

function clearAccountTimers() {
  window.clearTimeout(accountOpenTimer)
  window.clearTimeout(accountCloseTimer)
}

function openPanelNow(id: SiteNavGroupId) {
  clearPanelTimers()
  clearAccountTimers()
  accountOpen.value = false
  lastPanelId.value = id
  activePanelId.value = id
}

function closePanelNow(restoreFocus = false) {
  clearPanelTimers()
  const prevId = activePanelId.value
  activePanelId.value = null
  if (restoreFocus && prevId) {
    triggerRefs.get(prevId)?.focus()
  }
}

function openAccountNow() {
  clearAccountTimers()
  clearPanelTimers()
  activePanelId.value = null
  accountOpen.value = true
}

function closeAccountNow(restoreFocus = false) {
  clearAccountTimers()
  const wasOpen = accountOpen.value
  accountOpen.value = false
  if (restoreFocus && wasOpen) {
    accountTriggerRef.value?.focus()
  }
}

function closeAllPanels() {
  clearPanelTimers()
  clearAccountTimers()
  activePanelId.value = null
  accountOpen.value = false
}

function onGroupPointerDown(e: PointerEvent) {
  lastGroupPointerType = e.pointerType
}

function onGroupPointerEnter(id: SiteNavGroupId, e: PointerEvent) {
  if (e.pointerType !== 'mouse') return
  clearPanelTimers()
  // 从一个父级滑到另一个父级时立即切换内容，不先收起再展开
  if (activePanelId.value) {
    openPanelNow(id)
    return
  }
  openTimer = window.setTimeout(() => {
    openPanelNow(id)
  }, OPEN_DELAY)
}

function onHeaderPointerEnter(e: PointerEvent) {
  if (e.pointerType !== 'mouse') return
  if (activePanelId.value) {
    window.clearTimeout(closeTimer)
  }
}

function onHeaderPointerLeave(e: PointerEvent) {
  if (e.pointerType !== 'mouse') return
  scheduleClosePanel()
}

function scheduleClosePanel() {
  window.clearTimeout(openTimer)
  if (!activePanelId.value) return
  window.clearTimeout(closeTimer)
  closeTimer = window.setTimeout(() => {
    activePanelId.value = null
  }, CLOSE_DELAY)
}

function onGroupClick(id: SiteNavGroupId, e: MouseEvent) {
  // 鼠标点击只负责保持面板展开并取消收起计时器，不切换关闭；触屏 / 笔 / 键盘（detail === 0）按切换处理
  if (e.detail > 0 && lastGroupPointerType === 'mouse') {
    openPanelNow(id)
    return
  }
  if (activePanelId.value === id) {
    closePanelNow(false)
  } else {
    openPanelNow(id)
  }
}

async function onGroupKeydown(id: SiteNavGroupId, e: KeyboardEvent) {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    if (activePanelId.value === id) closePanelNow(false)
    else openPanelNow(id)
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    openPanelNow(id)
    await nextTick()
    panelBodyRef.value?.querySelector<HTMLElement>('a, button')?.focus()
  } else if (e.key === 'Escape' && activePanelId.value) {
    e.preventDefault()
    closePanelNow(true)
  }
}

function onAccountPointerDown(e: PointerEvent) {
  lastAccountPointerType = e.pointerType
}

function onAccountPointerEnter(e: PointerEvent) {
  if (e.pointerType !== 'mouse') return
  clearAccountTimers()
  if (accountOpen.value) return
  accountOpenTimer = window.setTimeout(() => {
    openAccountNow()
  }, OPEN_DELAY)
}

function onAccountPointerLeave(e: PointerEvent) {
  if (e.pointerType !== 'mouse') return
  window.clearTimeout(accountOpenTimer)
  if (!accountOpen.value) return
  window.clearTimeout(accountCloseTimer)
  accountCloseTimer = window.setTimeout(() => {
    accountOpen.value = false
  }, CLOSE_DELAY)
}

function onAccountClick(e: MouseEvent) {
  if (e.detail > 0 && lastAccountPointerType === 'mouse') {
    openAccountNow()
    return
  }
  if (accountOpen.value) closeAccountNow(false)
  else openAccountNow()
}

async function onAccountKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    if (accountOpen.value) closeAccountNow(false)
    else openAccountNow()
  } else if (e.key === 'ArrowDown') {
    e.preventDefault()
    openAccountNow()
    await nextTick()
    accountPanelRef.value?.querySelector<HTMLElement>('a, button')?.focus()
  } else if (e.key === 'Escape' && accountOpen.value) {
    e.preventDefault()
    closeAccountNow(true)
  }
}

function onDocumentPointerDown(e: PointerEvent) {
  const target = e.target as Node | null
  if (!target) return
  if (activePanelId.value && headerRef.value && !headerRef.value.contains(target)) {
    closePanelNow(false)
  }
  if (accountOpen.value && accountWrapRef.value && !accountWrapRef.value.contains(target)) {
    closeAccountNow(false)
  }
}

function syncAuth() {
  username.value = localStorage.getItem('username') || ''
  userAvatar.value = localStorage.getItem('userAvatar') || ''
  token.value = localStorage.getItem('token') || ''
}

watch(
  () => route.path,
  async (nextPath, prevPath) => {
    if (nextPath.startsWith('/admin') || prevPath?.startsWith('/admin')) {
      displayedRoutePath.value = nextPath
    }
    syncAuth()
    drawerOpen.value = false
    closeAllPanels()
    updateNavVisibility()
    await nextTick()
    updateNavVisibility()
    window.requestAnimationFrame(updateNavVisibility)
  }
)

watch(navVisible, (visible) => {
  if (!visible) closeAllPanels()
})

watch(drawerOpen, async (open) => {
  if (open) closeAllPanels()
  document.body.style.overflow = open ? 'hidden' : ''
  if (open) {
    await nextTick()
    drawerClose.value?.focus()
  }
})

function onStorage(e: StorageEvent) {
  if (e.key === 'username' || e.key === 'userAvatar' || e.key === 'token') {
    syncAuth()
  }
}

function getScrollY() {
  return window.scrollY || window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0
}

function updateNavVisibility() {
  if (!isHomeRoute.value) {
    navVisible.value = true
    return
  }
  const heroEl = document.querySelector<HTMLElement>('.home-page .hero')
  const heroHeight = heroEl ? heroEl.getBoundingClientRect().height : 0
  const baseHeight = heroHeight > 0 ? heroHeight : window.innerHeight
  navVisible.value = getScrollY() > baseHeight * 0.5
}

function onScrollOrResize() {
  if (scrollRaf) {
    scrollPending = true
    return
  }
  updateNavVisibility()
  scrollRaf = window.requestAnimationFrame(() => {
    scrollRaf = 0
    if (scrollPending) {
      scrollPending = false
      updateNavVisibility()
    }
  })
}

function onKeydown(e: KeyboardEvent) {
  if (e.key !== 'Escape') return
  if (drawerOpen.value) {
    drawerOpen.value = false
  } else if (activePanelId.value) {
    closePanelNow(true)
  } else if (accountOpen.value) {
    closeAccountNow(true)
  }
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('userAvatar')
  username.value = ''
  userAvatar.value = ''
  token.value = ''
  drawerOpen.value = false
  closeAllPanels()
  router.push('/login')
}

onMounted(() => {
  syncAuth()
  updateNavVisibility()
  nextTick(updateNavVisibility)
  window.addEventListener('user-login', syncAuth)
  window.addEventListener('popstate', onPopstate)
  window.addEventListener('storage', onStorage)
  window.addEventListener('scroll', onScrollOrResize, { passive: true })
  window.addEventListener('scrollend', updateNavVisibility, { passive: true })
  window.addEventListener('resize', onScrollOrResize, { passive: true })
  window.addEventListener('pageshow', updateNavVisibility)
  window.addEventListener('load', updateNavVisibility)
  window.addEventListener('keydown', onKeydown)
  document.addEventListener('pointerdown', onDocumentPointerDown)
})

onBeforeUnmount(() => {
  removeRouteTransitionGuard()
  clearPanelTimers()
  clearAccountTimers()
  if (scrollRaf) window.cancelAnimationFrame(scrollRaf)
  scrollRaf = 0
  scrollPending = false
  window.removeEventListener('user-login', syncAuth)
  window.removeEventListener('popstate', onPopstate)
  window.removeEventListener('storage', onStorage)
  window.removeEventListener('scroll', onScrollOrResize)
  window.removeEventListener('scrollend', updateNavVisibility)
  window.removeEventListener('resize', onScrollOrResize)
  window.removeEventListener('pageshow', updateNavVisibility)
  window.removeEventListener('load', updateNavVisibility)
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('pointerdown', onDocumentPointerDown)
  document.body.style.overflow = ''
})
</script>

<style>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--ss-bg);
}

.skip-link {
  position: absolute;
  left: 16px;
  top: -48px;
  z-index: 1200;
  padding: 8px 14px;
  border-radius: 10px;
  background: #151515;
  color: #fff;
  font-size: 14px;
}
.skip-link:focus {
  top: 12px;
}

/* ============ 悬浮圆角导航条 + Partiful 式一体展开面板 ============ */
.app-header {
  position: fixed;
  top: 16px;
  left: 50%;
  z-index: 1000;
  width: min(1040px, calc(100% - 32px));
  min-height: 64px;
  border-radius: 20px;
  background: #fdfdfb;
  box-shadow: 1px 3px 16px 4px rgba(0, 0, 0, 0.04), 0 0 0 1px rgba(21, 21, 21, 0.06);
  transform: translateX(-50%) translateY(0);
  opacity: 1;
  transition: transform 0.6s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}
.app-header.is-hidden {
  transform: translateX(-50%) translateY(-120%);
  opacity: 0;
  pointer-events: none;
}

.nav-inner {
  width: 100%;
  height: 64px;
  padding: 0 20px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 16px;
}

.brand {
  justify-self: start;
  display: inline-flex;
  align-items: baseline;
  gap: 10px;
  white-space: nowrap;
}
.brand-cn {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: #151515;
}
.brand-sub {
  font-size: 13px;
  font-weight: 500;
  color: #737373;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nav-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 36px;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #111111;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: -0.01em;
  white-space: nowrap;
  cursor: pointer;
  transition: color var(--ss-dur) var(--ss-ease);
}
.nav-link:hover,
.nav-link.is-open {
  color: #666666;
}
.nav-link.is-active {
  color: #151515;
  font-weight: 600;
}
.nav-link.is-active::after {
  content: '';
  position: absolute;
  left: 50%;
  top: calc(50% + 14px);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #151515;
  transform: translateX(-50%);
}

.chev {
  opacity: 0.65;
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}
.chev.is-open {
  transform: rotate(180deg);
}

/* 一体向下展开的下拉面板 */
.nav-panel-shell {
  display: grid;
  grid-template-rows: 0fr;
  transition: grid-template-rows 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}
.nav-panel-shell.is-open {
  grid-template-rows: 1fr;
}
.nav-panel-clip {
  overflow: hidden;
  min-height: 0;
}
.nav-panel-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;
  align-items: start;
  padding: 20px 24px 24px;
  border-top: 1px solid rgba(21, 21, 21, 0.06);
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 0.2s ease, transform 0.25s cubic-bezier(0.22, 1, 0.36, 1);
}
.nav-panel-body.is-visible {
  opacity: 1;
  transform: translateY(0);
  transition: opacity 0.26s ease 0.06s, transform 0.3s cubic-bezier(0.22, 1, 0.36, 1) 0.04s;
}

.panel-links-col {
  min-width: 0;
}
.panel-group-label {
  padding: 0 12px;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #8a8f98;
}
.panel-links {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 8px;
  row-gap: 4px;
  align-items: start;
  margin: 0;
  padding: 0;
}
.panel-links li {
  min-width: 0;
}
.panel-link {
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 44px;
  min-width: 0;
  padding: 0 12px;
  border-radius: 10px;
  color: #151515;
  transition: background-color 0.16s ease, color 0.16s ease;
}
.panel-link.has-desc {
  height: 56px;
  gap: 2px;
}
.panel-link:hover {
  background: rgba(21, 21, 21, 0.04);
}
.panel-link.is-active {
  background: rgba(21, 21, 21, 0.04);
}
.panel-link-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.panel-link-title {
  font-size: 16px;
  font-weight: 500;
  line-height: 1.3;
  color: #151515;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.panel-link.is-active .panel-link-title {
  font-weight: 600;
}
.panel-link-desc {
  padding-left: 30px;
  font-size: 13px;
  line-height: 1.35;
  color: #737373;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.panel-link-icon {
  color: #525252;
  flex-shrink: 0;
}
.panel-link:hover .panel-link-icon,
.panel-link.is-active .panel-link-icon {
  color: #151515;
}

.panel-featured {
  display: flex;
  flex-direction: column;
  width: 300px;
  padding: 10px;
  border-radius: 14px;
  border: 1px solid rgba(21, 21, 21, 0.08);
  background: #ffffff;
  color: #151515;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.panel-featured:hover {
  border-color: rgba(21, 21, 21, 0.16);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.featured-cover-wrap {
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: 10px;
  overflow: hidden;
  background: #f4f4f5;
}
.featured-cover {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.featured-meta {
  display: flex;
  flex-direction: column;
  padding: 10px 4px 4px;
}
.featured-tag {
  font-size: 12px;
  font-weight: 500;
  color: #8a8f98;
  line-height: 1.3;
}
.featured-title {
  margin-top: 4px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.35;
  color: #151515;
}
.featured-desc {
  margin-top: 4px;
  font-size: 14px;
  line-height: 1.5;
  color: #5b6472;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 右侧操作区与头像小面板 */
.nav-actions {
  justify-self: end;
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.nav-cta {
  display: inline-flex;
  align-items: center;
  height: 36px;
  padding: 0 18px;
  border-radius: 10px;
  background: #151515;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  transition: background-color var(--ss-dur) var(--ss-ease);
}
.nav-cta:hover,
.nav-cta.is-active {
  background: #2a2a2a;
}

.nav-account-wrap {
  position: relative;
}
.nav-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 10px 0 4px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #151515;
  cursor: pointer;
  transition: background-color var(--ss-dur) var(--ss-ease);
}
.nav-user:hover {
  background: rgba(21, 21, 21, 0.04);
}
.nav-avatar {
  background: #151515;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}
.nav-user .username {
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  font-weight: 500;
}

/* 头像悬浮小面板（220px，14px 圆角，阴影同导航条，用透明桥接防悬停断开） */
.nav-account-pop {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 1010;
  width: 220px;
  padding: 8px;
  border-radius: 14px;
  background: #fdfdfb;
  box-shadow: 1px 3px 16px 4px rgba(0, 0, 0, 0.04), 0 0 0 1px rgba(21, 21, 21, 0.06);
}
.nav-account-pop::before {
  content: '';
  position: absolute;
  top: -14px;
  left: 0;
  right: 0;
  height: 14px;
}
.account-group-label {
  padding: 4px 10px 6px;
  font-size: 12px;
  font-weight: 500;
  color: #8a8f98;
}
.account-link {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 40px;
  padding: 0 10px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #151515;
  font-size: 15px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.16s ease, color 0.16s ease;
}
.account-link:hover {
  background: rgba(21, 21, 21, 0.04);
}
.account-link.is-active {
  background: rgba(21, 21, 21, 0.04);
  font-weight: 600;
}
.account-link-icon {
  color: #525252;
}
.account-link:hover .account-link-icon,
.account-link.is-active .account-link-icon {
  color: #151515;
}
.account-divider {
  height: 1px;
  margin: 6px 4px;
  background: rgba(21, 21, 21, 0.08);
}
.account-logout:hover {
  color: #c2410c;
}
.account-logout:hover .account-link-icon {
  color: #c2410c;
}

.account-pop-enter-active,
.account-pop-leave-active {
  transition: opacity 0.2s ease, transform 0.22s cubic-bezier(0.22, 1, 0.36, 1);
}
.account-pop-enter-from,
.account-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.nav-text,
.nav-ghost {
  display: inline-flex;
  align-items: center;
  height: 36px;
  padding: 0 14px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #525252;
  transition: background-color var(--ss-dur) var(--ss-ease), color var(--ss-dur) var(--ss-ease);
}
.nav-text:hover {
  color: #151515;
}
.nav-ghost {
  background: var(--ss-fill);
  color: #151515;
}
.nav-ghost:hover {
  background: var(--ss-fill-hover);
}

.nav-burger {
  display: none;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 10px;
  background: var(--ss-fill);
  color: #151515;
  cursor: pointer;
  place-items: center;
}

/* ============ 移动端抽屉 ============ */
.nav-drawer {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow-y: auto;
}
.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--ss-header-h);
  padding: 0 16px 0 20px;
  border-bottom: 1px solid var(--ss-line);
}
.drawer-head .nav-burger {
  display: grid;
}
.drawer-body {
  padding: 20px 20px 40px;
}
.drawer-cta {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  border-radius: 12px;
  background: #151515;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
}
.drawer-home {
  margin-top: 20px;
}
.drawer-group {
  margin-top: 24px;
}
.drawer-title {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.04em;
  color: #8a8f98;
}
.drawer-link {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 44px;
  padding: 0 6px;
  border: 0;
  border-bottom: 1px solid var(--ss-line);
  background: none;
  color: #151515;
  font-size: 16px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
}
.drawer-link-icon {
  color: #525252;
}
.drawer-link.is-active {
  color: #151515;
  font-weight: 600;
}
.drawer-link.is-active .drawer-link-icon {
  color: #151515;
}
.drawer-logout {
  color: #c2410c;
}
.drawer-logout .drawer-link-icon {
  color: #c2410c;
}
.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 180ms var(--ss-ease);
}
.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

/* ============ 主体（非首页顶部留出 96px 避开悬浮导航） ============ */
.app-main {
  flex: 1;
  padding: 96px 16px 40px;
  overflow-x: clip;
}
.app-main.home-main {
  padding: 24px 16px 40px;
}
.app-main.auth-main {
  padding: 0;
}

/* ============ 页面切换过渡（只作用于主内容区 router-view） ============ */
.route-slide-left-leave-active,
.route-slide-right-leave-active {
  transition: opacity 0.2s ease-in, transform 0.2s ease-in;
}
.route-slide-left-enter-active,
.route-slide-right-enter-active {
  transition: opacity 0.38s cubic-bezier(0.22, 1, 0.36, 1), transform 0.38s cubic-bezier(0.22, 1, 0.36, 1);
}
.route-fade-leave-active {
  transition: opacity 0.2s ease-in;
}
.route-fade-enter-active {
  transition: opacity 0.38s cubic-bezier(0.22, 1, 0.36, 1);
}
.route-slide-left-leave-to {
  opacity: 0;
  transform: translateX(-32px);
}
.route-slide-left-enter-from {
  opacity: 0;
  transform: translateX(32px);
}
.route-slide-right-leave-to {
  opacity: 0;
  transform: translateX(32px);
}
.route-slide-right-enter-from {
  opacity: 0;
  transform: translateX(-32px);
}
.route-fade-leave-to,
.route-fade-enter-from {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .route-slide-left-leave-to,
  .route-slide-left-enter-from,
  .route-slide-right-leave-to,
  .route-slide-right-enter-from {
    transform: none;
  }
}

/* ============ 页脚：与导航同宽（1040px），三列读自 siteNav ============ */
.app-footer {
  background: var(--ss-bg-soft);
  border-top: 1px solid var(--ss-line);
}
.footer-inner {
  max-width: 1040px;
  margin: 0 auto;
  padding: 48px 20px 8px;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 2fr);
  gap: 40px;
}
.footer-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--ss-ink);
}
.footer-desc {
  max-width: 22em;
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--ss-muted);
}
.footer-cols {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}
.footer-col {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}
.footer-title {
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ss-ink);
}
.footer-link {
  font-size: 14px;
  color: var(--ss-muted);
  transition: color var(--ss-dur) var(--ss-ease);
}
.footer-link:hover {
  color: var(--ss-ink);
}
.footer-bottom {
  max-width: 1040px;
  margin: 0 auto;
  padding: 24px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 12px;
  color: #8a8f98;
}
.footer-admin {
  font-size: 12px;
  color: #8a8f98;
}
.footer-admin:hover {
  color: #151515;
}

@media (max-width: 960px) {
  .nav-inner {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 0 16px;
  }
  .brand-sub {
    display: none;
  }
  .footer-inner {
    grid-template-columns: minmax(0, 1fr);
    gap: 28px;
  }
}

@media (max-width: 820px) {
  .nav-link {
    padding: 0 10px;
    font-size: 15px;
  }
  .nav-user .username {
    max-width: 72px;
  }
  .nav-panel-body {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
    padding: 16px 18px 20px;
    max-height: calc(100vh - 104px);
    overflow-y: auto;
  }
  .panel-featured {
    display: none;
  }
}

@media (max-width: 768px) {
  .app-main {
    padding: 88px 12px 32px;
  }
  .app-main.home-main {
    padding: 16px 12px 32px;
  }
}

@media (max-width: 640px) {
  .app-header {
    top: 12px;
    width: calc(100% - 20px);
    min-height: 54px;
    border-radius: 16px;
  }
  .nav-inner {
    height: 54px;
    padding: 0 12px;
    gap: 6px;
  }
  .brand-cn {
    font-size: 15px;
    letter-spacing: 0.02em;
  }
  .nav-menu {
    gap: 2px;
  }
  .nav-link {
    height: 32px;
    padding: 0 7px;
    gap: 2px;
    font-size: 14px;
  }
  .nav-link .chev {
    width: 12px;
    height: 12px;
  }
  .nav-link.is-active::after {
    top: calc(50% + 13px);
  }
  .nav-actions {
    gap: 6px;
  }
  .nav-cta {
    height: 32px;
    padding: 0 12px;
    border-radius: 9px;
    font-size: 13px;
  }
  .nav-text {
    height: 32px;
    padding: 0 8px;
    font-size: 13px;
  }
  .nav-ghost {
    height: 32px;
    padding: 0 10px;
    border-radius: 9px;
    font-size: 13px;
  }
  .nav-user {
    height: 32px;
    gap: 4px;
    padding: 0 4px 0 2px;
  }
  .nav-user .username {
    display: none;
  }
  .nav-avatar {
    width: 26px !important;
    height: 26px !important;
    line-height: 26px !important;
    font-size: 11px;
  }
  .nav-account-pop {
    top: calc(100% + 10px);
    width: 204px;
    max-width: calc(100vw - 24px);
  }
  .nav-panel-body {
    padding: 14px 12px 16px;
    max-height: calc(100vh - 88px);
  }
  .panel-links {
    grid-template-columns: minmax(0, 1fr);
  }
  .panel-link {
    height: 42px;
    padding: 0 10px;
  }
  .panel-link.has-desc {
    height: 52px;
  }
  .panel-link-title {
    font-size: 15px;
  }
  .panel-link-desc {
    font-size: 12px;
  }
  .app-main {
    padding: 80px 12px 32px;
  }
  .footer-inner {
    padding: 36px 20px 4px;
  }
  .footer-cols {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .footer-bottom {
    padding: 20px;
  }
}

@media (max-width: 440px) {
  .app-header {
    top: 10px;
    width: calc(100% - 16px);
    min-height: 50px;
    border-radius: 14px;
  }
  .nav-inner {
    height: 50px;
    padding: 0 10px;
    gap: 4px;
  }
  .brand-cn {
    font-size: 14px;
    letter-spacing: 0.01em;
  }
  .nav-menu {
    gap: 0;
  }
  .nav-link {
    height: 30px;
    padding: 0 5px;
    gap: 2px;
    font-size: 13px;
  }
  .nav-link.is-active::after {
    top: calc(50% + 12px);
    width: 3px;
    height: 3px;
  }
  .nav-actions {
    gap: 4px;
  }
  .nav-cta {
    height: 30px;
    padding: 0 10px;
    border-radius: 8px;
    font-size: 12px;
  }
  .nav-text {
    height: 30px;
    padding: 0 6px;
    font-size: 12px;
  }
  .nav-ghost {
    height: 30px;
    padding: 0 8px;
    border-radius: 8px;
    font-size: 12px;
  }
  .nav-user {
    height: 30px;
    gap: 2px;
    padding: 0 2px;
  }
  .nav-avatar {
    width: 24px !important;
    height: 24px !important;
    line-height: 24px !important;
  }
  .app-main {
    padding: 74px 12px 28px;
  }
}

@media (max-width: 375px) {
  .nav-inner {
    padding: 0 8px;
    gap: 2px;
  }
  .brand-cn {
    font-size: 13px;
  }
  .nav-link {
    padding: 0 4px;
    font-size: 12.5px;
  }
  .nav-cta {
    padding: 0 8px;
  }
  .nav-text {
    padding: 0 5px;
  }
  .nav-ghost {
    padding: 0 7px;
  }
}
</style>
